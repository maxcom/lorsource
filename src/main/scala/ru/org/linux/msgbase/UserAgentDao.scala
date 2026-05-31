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
package ru.org.linux.msgbase

import org.springframework.stereotype.Repository
import ru.org.linux.scalikejdbc.SpringDB
import scalikejdbc.*

import java.util.Optional
import scala.jdk.OptionConverters.RichOption

@Repository
class UserAgentDao(springDB: SpringDB):
  def getUserAgentById(id: Int): Optional[String] =
    if id == 0 then
      Optional.empty[String]
    else
      springDB
        .run:
          sql"SELECT name FROM user_agents WHERE id = $id".map(rs => rs.string("name")).single.apply()
        .toJava

  def createOrGetId(userAgent: String): Int =
    springDB.run:
      sql"SELECT create_user_agent($userAgent)".map(rs => rs.int(1)).single.apply().getOrElse(
        throw new IllegalStateException(s"create_user_agent returned no result for: $userAgent")
      )

end UserAgentDao
