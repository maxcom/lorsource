/*
 * Copyright 1998-2024 Linux.org.ru
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
package ru.org.linux.spring.dao

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Repository
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.markup.MarkupType

import java.sql.{ResultSet, SQLException}
import javax.sql.DataSource
import scala.jdk.CollectionConverters.*

@Repository
object MsgbaseDao {
  /**
   * Запрос тела сообщения и признака bbcode для сообщения
   */
  private val QUERY_MESSAGE_TEXT = "SELECT message, markup FROM msgbase WHERE id=?"
}

@Repository
class MsgbaseDao(dataSource: DataSource, val transactionManager: PlatformTransactionManager) extends TransactionManagement {
  private val jdbcTemplate: JdbcTemplate = new JdbcTemplate(dataSource)
  private val namedJdbcTemplate: NamedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource)
  private val insertMsgbase: SimpleJdbcInsert = new SimpleJdbcInsert(dataSource)

  insertMsgbase.setTableName("msgbase")
  insertMsgbase.usingColumns("id", "message", "markup")

  def saveNewMessage(message: MessageText, msgid: Int): Unit = transactional() { _ =>
    insertMsgbase.execute(Map("id" -> Integer.valueOf(msgid), "message" -> message.text, "markup" -> message.markup.id).asJava)
  }

  @throws[SQLException]
  private def messageTextOf(resultSet: ResultSet) = {
    val text = resultSet.getString("message")
    val markup = resultSet.getString("markup")

    MessageText(text, MarkupType.of(markup))
  }

  def getMessageText(msgid: Int): MessageText =
    jdbcTemplate.queryForObject(MsgbaseDao.QUERY_MESSAGE_TEXT, (resultSet: ResultSet, _: Int) => messageTextOf(resultSet), msgid)

  def getMessageText(msgids: collection.Seq[Int]): Map[Int, MessageText] = {
    if (msgids.isEmpty) {
      Map.empty
    } else {
      val out = namedJdbcTemplate.query(
        "SELECT message, markup, id FROM msgbase WHERE id IN (:list)",
        Map("list" -> msgids.asJava).asJava,
        (resultSet: ResultSet, _: Int) => {
          resultSet.getInt("id") -> messageTextOf(resultSet)
        })

      out.asScala.toMap
    }
  }

  def updateMessage(msgid: Int, text: String): Unit =
    namedJdbcTemplate.update("UPDATE msgbase SET message=:message WHERE id=:msgid",
      Map("message" -> text, "msgid" -> msgid).asJava)

  def appendMessage(msgid: Int, text: String): Unit =
    jdbcTemplate.update("UPDATE msgbase SET message=message||? WHERE id=?", text, msgid)
}