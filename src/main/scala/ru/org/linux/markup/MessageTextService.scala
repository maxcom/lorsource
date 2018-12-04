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

import com.google.common.base.Strings
import org.jsoup.Jsoup
import org.springframework.stereotype.Service
import ru.org.linux.spring.dao.MarkupType.{Html, Lorcode}
import ru.org.linux.spring.dao.{MarkupType, MessageText}
import ru.org.linux.user.User
import ru.org.linux.util.StringUtil
import ru.org.linux.util.bbcode.LorCodeService
import ru.org.linux.util.formatter.ToLorCodeTexFormatter

import scala.collection.JavaConverters._

@Service
class MessageTextService(lorCodeService: LorCodeService) {
  // раньше это делалось при постинге, теперь будем делать при рендеринге
  private def prepareLorcode(text: String): String = ToLorCodeTexFormatter.quote(text, "\n")

  /**
    * Получить html представление текста комментария
    *
    * @param messageText текст комментария
    * @return строку html комментария
    */
  def renderCommentText(messageText: MessageText, nofollow: Boolean): String = {
    messageText.markup match {
      case Lorcode ⇒
        lorCodeService.parseComment(prepareLorcode(messageText.text), nofollow)
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
        lorCodeService.parseCommentRSS(prepareLorcode(messageText.text))
      case Html ⇒
        "<p>" + messageText.text + "</p>"
    }
  }

  def renderTopic(text: MessageText, minimizeCut: Boolean, nofollow: Boolean, canonicalUrl: String): String = {
    text.markup match {
      case Lorcode ⇒
        if (minimizeCut) {
          lorCodeService.parseTopicWithMinimizedCut(prepareLorcode(text.text), canonicalUrl, nofollow)
        } else {
          lorCodeService.parseTopic(prepareLorcode(text.text), nofollow)
        }
      case Html ⇒
        "<p>" + text.text
    }
  }

  def extractPlainText(text: MessageText): String = {
    text.markup match {
      case Lorcode ⇒
        lorCodeService.extractPlainTextFromLorcode(prepareLorcode(text.text))
      case Html ⇒
        Jsoup.parse(text.text).text
    }
  }

  def mentions(text: MessageText): java.util.Set[User] = {
    text.markup match {
      case Lorcode ⇒
        lorCodeService.getReplierFromMessage(prepareLorcode(text.text))
      case Html ⇒
        Set.empty[User].asJava
    }
  }

  def isEmpty(text: MessageText): Boolean = extractPlainText(text).trim.isEmpty

  // TODO надо бы извести эту логику; для moveBy/moveFrom если история; url/linktext лучше просто показывать всегда
  def moveInfo(markup: MarkupType, url: String, linktext: String, moveBy: User, moveFrom: String): String = {
    /* if url is not null, update the topic text */
    val link = if (!Strings.isNullOrEmpty(url)) {
      markup match {
        case Html ⇒
          s"""<br><a href="$url">$linktext</a>
             |<br>
             |""".stripMargin
        case Lorcode ⇒
          s"""
             |[url=$url]$linktext[/url]
             |""".stripMargin
      }
    } else ""

    markup match {
      case Lorcode ⇒
        '\n' + link + "\n\n[i]Перемещено " + moveBy.getNick + " из " + moveFrom + "[/i]\n"
      case Html ⇒
        '\n' + link + "<br><i>Перемещено " + moveBy.getNick + " из " + moveFrom + "</i>\n"
    }
  }
}

object MessageTextService {
  val PostingModes: Map[String, String] = Map("lorcode" -> "LORCODE", "ntobr" -> "User line break")
  val PostingModesJava: java.util.Map[String, String] = PostingModes.asJava

  /**
    * Предобработка нового сообщения. При редактировании не используется.
    *
    * По сути костыли, которым надо переехать на фазу рендеринга.
    *
    * @param message текст нового сообщения из формы
    * @param mode режим постинга (lorcode или ntobr)
    * @return обработанный текст для сохранения и рендеринга
    */
  def preprocessPostingText(message: String, mode: String): MessageText = {
    mode match {
      case "ntobr" ⇒
        MessageText.apply(ToLorCodeTexFormatter.quote(message, "[br]"), MarkupType.Lorcode)
      case "lorcode" ⇒
        // это надо убрать, так как все равно еще раз делаем при рендеринге
        MessageText.apply(ToLorCodeTexFormatter.quote(message, "\n"), MarkupType.Lorcode)
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
}
