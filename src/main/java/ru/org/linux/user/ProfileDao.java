/*
 * Copyright 1998-2016 Linux.org.ru
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.org.linux.site.DefaultProfile;
import ru.org.linux.util.ProfileHashtable;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ProfileDao {
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @Nonnull
  public Profile readProfile(@NotNull User user) {
    List<Profile> profiles = jdbcTemplate.query(
            "SELECT settings, main FROM user_settings WHERE id=?",
            new RowMapper<Profile>() {
              @Override
              public Profile mapRow(ResultSet resultSet, int i) throws SQLException {
                Array boxes = resultSet.getArray("main");

                if (boxes != null) {
                  return new Profile(
                          new ProfileHashtable(DefaultProfile.getDefaultProfile(), (Map<String, String>) resultSet.getObject("settings")),
                          Arrays.asList((String[]) boxes.getArray())
                  );
                } else {
                  return new Profile(
                          new ProfileHashtable(DefaultProfile.getDefaultProfile(), (Map<String, String>) resultSet.getObject("settings")),
                          null
                  );
                }
              }
            },
            user.getId()
    );

    if (profiles.isEmpty()) {
      return new Profile(new ProfileHashtable(DefaultProfile.getDefaultProfile(), new HashMap<String, String>()), null);
    } else {
      return profiles.get(0);
    }
  }

  public void deleteProfile(@Nonnull User user) {
    jdbcTemplate.update("DELETE FROM user_settings WHERE id=?", user.getId());
  }

  public void writeProfile(@Nonnull final User user, @Nonnull final Profile profile) {
    String boxlets[] = null;

    List<String> customBoxlets = profile.getCustomBoxlets();

    if (customBoxlets !=null) {
      boxlets = customBoxlets.toArray(new String[customBoxlets.size()]);
    }

    final String[] finalBoxlets = boxlets;
    if (jdbcTemplate.update(
            new PreparedStatementCreator() {
              @Override
              public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement st = con.prepareStatement("UPDATE user_settings SET settings=?, main=? WHERE id=?");

                st.setObject(1, profile.getSettings());

                if (finalBoxlets!=null) {
                  st.setArray(2, con.createArrayOf("text", finalBoxlets));
                } else {
                  st.setNull(2, Types.ARRAY);
                }

                st.setInt(3, user.getId());

                return st;
              }
            })==0) {
      jdbcTemplate.update(
              new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                  PreparedStatement st = con.prepareStatement("INSERT INTO user_settings (id, settings, main) VALUES (?,?,?)");

                  st.setInt(1, user.getId());

                  st.setObject(2, profile.getSettings());

                  if (finalBoxlets!=null) {
                    st.setArray(3, con.createArrayOf("text", finalBoxlets));
                  } else {
                    st.setNull(3, Types.ARRAY);
                  }

                  return st;
                }
              }
      );
    }
  }
}
