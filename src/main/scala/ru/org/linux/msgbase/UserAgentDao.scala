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
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
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

  /** Отдаёт id записи словаря для User-Agent, при необходимости создавая её.
    *
    * Значение усекается до 511 символов (user_agents.name — varchar(512)), как в CommentDao/TopicDao: User-Agent из
    * заголовка может быть длиннее, и без усечения вставка падает с ошибкой БД.
    */
  def createOrGetId(userAgent: String)(using Transaction): Int =
    val truncated = userAgent.substring(0, Math.min(UserAgentDao.MaxNameLength, userAgent.length))

    sql"SELECT create_user_agent($truncated)"
      .map(rs => rs.int(1))
      .single
      .apply()
      .getOrElse(throw new IllegalStateException(s"create_user_agent returned no result for: $truncated"))

  /** Условие неиспользуемости записи словаря user_agents по правилам doc/data-retention.md: на запись нет упоминаний в
    * БД — внешние ключи topics.ua_id / comments.ua_id и упоминание id в журнале действий учётной записи (user_log, ключ
    * info->'user_agent', значение — id строкой).
    *
    * Общий для [[getUnreferencedUserAgentIds]] и перепроверки в [[purgeUnreferencedUserAgents]], чтобы запросы не
    * расходились.
    */
  private val UnreferencedCondition: SQLSyntax =
    sqls"""NOT EXISTS (SELECT 1 FROM topics t WHERE t.ua_id = ua.id)
         AND NOT EXISTS (SELECT 1 FROM comments c WHERE c.ua_id = ua.id)
         AND NOT EXISTS (SELECT 1 FROM user_log l WHERE l.info->'user_agent' = ua.id::text)"""

  /** Неиспользуемые записи словаря user_agents — кандидаты на окончательное удаление.
    *
    * Критерии описаны в doc/data-retention.md
    *
    * @return
    *   список идентификаторов кандидатов, упорядоченный по возрастанию id
    */
  def getUnreferencedUserAgentIds: Seq[Int] =
    springDB.run(sql"""SELECT ua.id FROM user_agents ua
            WHERE $UnreferencedCondition
            ORDER BY ua.id""".map(rs => rs.int("id")).list.apply())

  /** Окончательно удаляет неиспользуемые записи словаря user_agents (в одной транзакции).
    *
    * Критерии описаны в doc/data-retention.md
    *
    * Перед удалением перепроверяем условия: за время между выборкой кандидатов и удалением на запись мог сослаться
    * новый комментарий/топик либо упоминание в user_log. Новая версия create_user_agent держит лок FOR KEY SHARE до
    * конца транзакции вызывающего, поэтому DELETE блокируется до его коммита и перепроверка условий происходит по
    * свежим данным.
    *
    * @param ids
    *   идентификаторы окончательно удаляемых записей
    * @return
    *   число фактически удалённых записей
    */
  def purgeUnreferencedUserAgents(ids: Seq[Int]): Int =
    if ids.isEmpty then
      0
    else
      springDB.localTx {
        sql"""DELETE FROM user_agents ua
              WHERE ua.id IN ($ids)
                AND $UnreferencedCondition""".update.apply()
      }

end UserAgentDao

object UserAgentDao:
  /** Максимальная длина имени в словаре user_agents (столбец name — varchar(512)) */
  val MaxNameLength = 511
