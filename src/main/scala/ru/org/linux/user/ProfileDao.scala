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

package ru.org.linux.user

import org.springframework.stereotype.Repository
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.scalikejdbc.SpringDB.given
import ru.org.linux.site.DefaultProfile
import ru.org.linux.util.ProfileHashtable
import scalikejdbc.*

import java.util.HashMap

@Repository
class ProfileDao(springDB: SpringDB):
  def readProfile(userId: Int): Profile =
    springDB.run:
      sql"SELECT settings FROM user_settings WHERE id=$userId"
        .map(rs =>
          Profile(ProfileHashtable(DefaultProfile.defaultProfile, rs.get[java.util.Map[String, String]]("settings"))))
        .single
        .apply()
        .getOrElse(Profile(ProfileHashtable(DefaultProfile.defaultProfile, HashMap[String, String]())))

  def deleteProfile(user: User): Unit =
    springDB.run:
      sql"DELETE FROM user_settings WHERE id=${user.id}".update.apply()

  def writeProfile(user: User, profile: ProfileBuilder): Unit =
    val settings = profile.getSettings
    springDB.run:
      sql"INSERT INTO user_settings (id, settings) VALUES (${user
          .id}, $settings) ON CONFLICT (id) DO UPDATE SET settings=$settings".update.apply()
