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
import ru.org.linux.markup.MarkupType
import ru.org.linux.scalikejdbc.{SpringDB, Transaction}
import ru.org.linux.scalikejdbc.Transaction.given
import ru.org.linux.site.MessageNotFoundException
import scalikejdbc.*

import java.sql.SQLException

@Repository
class MsgbaseDao(springDB: SpringDB):

  def saveNewMessage(message: MessageText, msgid: Int)(using Transaction): Unit =
    sql"INSERT INTO msgbase (id, message, markup) VALUES ($msgid, ${message.text}, ${message.markup.id})".update.apply()

  @throws[SQLException]
  private def messageTextOf(rs: WrappedResultSet): MessageText =
    MessageText(rs.string("message"), MarkupType.of(rs.string("markup")))

  def getMessageText(msgid: Int): MessageText =
    springDB.run:
      sql"SELECT message, markup FROM msgbase WHERE id=$msgid"
        .map(messageTextOf)
        .single
        .apply()
        .getOrElse(throw new MessageNotFoundException(msgid))

  def getMessageText(msgids: collection.Seq[Int]): Map[Int, MessageText] =
    if msgids.isEmpty then
      Map.empty
    else
      springDB.run:
        sql"SELECT message, markup, id FROM msgbase WHERE id IN ($msgids)"
          .map(rs => rs.int("id") -> messageTextOf(rs))
          .list
          .apply()
          .toMap

  def updateMessage(msgid: Int, text: String)(using Transaction): Unit =
    sql"UPDATE msgbase SET message=$text WHERE id=$msgid".update.apply()

  def appendMessage(msgid: Int, text: String)(using Transaction): Unit =
    sql"UPDATE msgbase SET message=message||$text WHERE id=$msgid".update.apply()
