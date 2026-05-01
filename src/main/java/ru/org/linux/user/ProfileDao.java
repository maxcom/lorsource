/*
 * Copyright 1998-2026 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.org.linux.site.DefaultProfile;
import ru.org.linux.util.ProfileHashtable;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ProfileDao {
  private final JdbcTemplate jdbcTemplate;

  public ProfileDao(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  public Profile readProfile(int userId) {
    List<Profile> profiles = jdbcTemplate.query(
            "SELECT settings FROM user_settings WHERE id=?",
            (resultSet, i) -> Profile.apply(
                    new ProfileHashtable(DefaultProfile.defaultProfile(), (Map<String, String>) resultSet.getObject("settings"))
            ),
            userId
    );

    if (profiles.isEmpty()) {
      return Profile.apply(new ProfileHashtable(DefaultProfile.defaultProfile(), new HashMap<>()));
    } else {
      return profiles.getFirst();
    }
  }

  public void deleteProfile(User user) {
    jdbcTemplate.update("DELETE FROM user_settings WHERE id=?", user.getId());
  }

  public void writeProfile(User user, ProfileBuilder profile) {
    if (jdbcTemplate.update(
            con -> {
              PreparedStatement st = con.prepareStatement("UPDATE user_settings SET settings=? WHERE id=?");
              st.setObject(1, profile.getSettings());
              st.setInt(2, user.getId());
              return st;
            }) == 0) {
      jdbcTemplate.update(
              con -> {
                PreparedStatement st = con.prepareStatement("INSERT INTO user_settings (id, settings) VALUES (?,?)");
                st.setInt(1, user.getId());
                st.setObject(2, profile.getSettings());
                return st;
              }
      );
    }
  }
}
