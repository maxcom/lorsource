/*
 * Copyright 1998-2018 Linux.org.ru
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

package ru.org.linux.markup

import org.jsoup.Jsoup
import org.springframework.stereotype.Service
import ru.org.linux.spring.dao.MarkupType.{Html, Lorcode}
import ru.org.linux.spring.dao.MessageText
import ru.org.linux.user.User
import ru.org.linux.util.StringUtil
import ru.org.linux.util.bbcode.LorCodeService
import scala.collection.JavaConverters._

@Service
class MessageTextService(lorCodeService: LorCodeService) {
  /**
    * Получить html представление текста комментария
    *
    * @param messageText текст комментария
    * @return строку html комментария
    */
  def renderCommentText(messageText: MessageText, nofollow: Boolean): String = {
    messageText.markup match {
      case Lorcode ⇒
        lorCodeService.parseComment(messageText.text, nofollow)
      case Html ⇒
        "<p>" + messageText.text + "</p>"
    }
  }

  /**
    * Получить RSS представление текста комментария
    *
    * @param messageText текст комментария
    * @return строку html комментария
    */
  def renderTextRSS(messageText: MessageText): String = {
    messageText.markup match {
      case Lorcode ⇒
        lorCodeService.parseCommentRSS(messageText.text)
      case Html ⇒
        "<p>" + messageText.text + "</p>"
    }
  }

  def extractPlainText(text: MessageText): String = {
    text.markup match {
      case Lorcode ⇒
        lorCodeService.extractPlainTextFromLorcode(text.text)
      case Html ⇒
        Jsoup.parse(text.text).text
    }
  }

  /**
    * Обрезать чистый текст до заданого размера
    *
    * @param plainText  обрабатываемый текст (не lorcode!)
    * @param maxLength  обрезать текст до указанной длинны
    * @param encodeHtml экранировать теги
    * @return обрезанный текст
    */
  def trimPlainText(plainText: String, maxLength: Int, encodeHtml: Boolean): String = {
    val cut = if (plainText.length < maxLength) {
      plainText
    } else {
      plainText.substring(0, maxLength).trim + "..."
    }

    if (encodeHtml) {
      StringUtil.escapeForceHtml(cut)
    } else {
      cut
    }
  }

  def mentions(text: MessageText): java.util.Set[User] = {
    text.markup match {
      case Lorcode ⇒
        lorCodeService.getReplierFromMessage(text.text)
      case Html ⇒
        Set.empty[User].asJava
    }
  }

  def isEmpty(text: MessageText): Boolean = {
    extractPlainText(text).trim.isEmpty
  }
}
