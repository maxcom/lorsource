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
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.scalikejdbc.SpringDB
import scalikejdbc.*

@Repository
class IgnoreListDao(springDB: SpringDB):
  @throws[AccessViolationException]
  def addUser(listOwner: User, userToIgnore: User): Unit =
    if userToIgnore.isModerator then
      throw AccessViolationException("Нельзя игнорировать модератора")

    springDB.run:
      sql"INSERT INTO ignore_list (userid,ignored) VALUES(${listOwner.id},${userToIgnore.id}) ON CONFLICT DO NOTHING"
        .update
        .apply()

  def remove(listOwner: User, userToIgnore: User): Unit =
    springDB.run:
      sql"DELETE FROM ignore_list WHERE userid=${listOwner.id} AND ignored=${userToIgnore.id}".update.apply()

  def get(user: Int): Set[Int] =
    springDB.run:
      sql"SELECT a.ignored FROM ignore_list a WHERE a.userid=$user".map(rs => rs.int("ignored")).list.apply().toSet

  def getIgnoreCount(ignoredUser: User): Int =
    springDB.run:
      sql"SELECT count(*) as inum FROM ignore_list JOIN users ON ignore_list.userid = users.id WHERE ignored=${ignoredUser
          .id} AND not blocked".map(rs => rs.int("inum")).single.apply().getOrElse(0)

  def isIgnored(byUserId: Int, commentId: Int): Boolean =
    springDB.run:
      sql"select exists (select ignored from ignore_list where userid=$byUserId intersect select get_branch_authors($commentId))"
        .map(rs => rs.boolean(1))
        .single
        .apply()
        .getOrElse(false)
