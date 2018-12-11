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
import ru.org.linux.markup.MarkupType._
import ru.org.linux.spring.dao.MessageText
import ru.org.linux.user.User
import ru.org.linux.util.StringUtil
import ru.org.linux.util.bbcode.LorCodeService
import ru.org.linux.util.formatter.ToLorCodeTexFormatter
import ru.org.linux.util.markdown.MarkdownFormatter

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap

@Service
class MessageTextService(lorCodeService: LorCodeService, markdownFormatter: MarkdownFormatter) {
  // TODO Markdown: fix header font size
  // TODO Markdown: typography
  // TODO Markdown: implement LorURI rendering
  // TODO show markup mode in edit form for correctors

  import MessageTextService._

  /**
    * Получить html представление текста комментария
    *
    * @param text текст комментария
    * @return строку html комментария
    */
  def renderCommentText(text: MessageText, nofollow: Boolean): String = {
    text.markup match {
      case Lorcode ⇒
        lorCodeService.parseComment(prepareLorcode(text.text), nofollow)
      case LorcodeUlb ⇒
        lorCodeService.parseComment(prepareUlb(text.text), nofollow)
      case Html ⇒
        "<p>" + text.text + "</p>"
      case Markdown ⇒
        // TODO nofollow support
        markdownFormatter.renderToHtml(text.text)
    }
  }

  /**
    * Получить RSS представление текста комментария
    *
    * @param text текст комментария
    * @return строку html комментария
    */
  def renderTextRSS(text: MessageText): String = {
    text.markup match {
      case Lorcode ⇒
        lorCodeService.parseCommentRSS(prepareLorcode(text.text))
      case LorcodeUlb ⇒
        lorCodeService.parseCommentRSS(prepareUlb(text.text))
      case Html ⇒
        "<p>" + text.text + "</p>"
      case Markdown ⇒
        // TODO check if rss needs special rendering
        markdownFormatter.renderToHtml(text.text)
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
      case LorcodeUlb ⇒
        if (minimizeCut) {
          lorCodeService.parseTopicWithMinimizedCut(prepareUlb(text.text), canonicalUrl, nofollow)
        } else {
          lorCodeService.parseTopic(prepareUlb(text.text), nofollow)
        }
      case Html ⇒
        "<p>" + text.text
      case Markdown ⇒
        // TODO nofollow support
        // TODO [cut] support
        markdownFormatter.renderToHtml(text.text)
    }
  }

  def extractPlainText(text: MessageText): String = {
    text.markup match {
      case Lorcode ⇒
        lorCodeService.extractPlainTextFromLorcode(prepareLorcode(text.text))
      case LorcodeUlb ⇒
        lorCodeService.extractPlainTextFromLorcode(prepareUlb(text.text))
      case Html ⇒
        Jsoup.parse(text.text).text
      case Markdown ⇒
        text.text
    }
  }

  def mentions(text: MessageText): java.util.Set[User] = {
    text.markup match {
      case Lorcode ⇒
        lorCodeService.getReplierFromMessage(prepareLorcode(text.text))
      case LorcodeUlb ⇒
        lorCodeService.getReplierFromMessage(prepareUlb(text.text))
      case Html ⇒
        Set.empty[User].asJava
      case Markdown ⇒
        // TODO support mentions
        Set.empty[User].asJava
    }
  }

  def isEmpty(text: MessageText): Boolean = extractPlainText(text).trim.isEmpty

  // TODO надо бы извести эту логику; для moveBy/moveFrom если история; url/linktext лучше просто показывать всегда
  def moveInfo(markup: MarkupType, url: String, linktext: String, moveBy: User, moveFrom: String): String = {
    /* if url is not null, update the topic text */
    val link = if (!Strings.isNullOrEmpty(url)) {
      // TODO escape linktext everywhere; encode url in html

      markup match {
        case Html ⇒
          s"""<br><a href="$url">$linktext</a>
             |<br>
             |""".stripMargin
        case Lorcode | LorcodeUlb ⇒
          s"""
             |[url=$url]$linktext[/url]
             |""".stripMargin
        case Markdown ⇒
          s"""
             |[$linktext]($url)
             |""".stripMargin
      }
    } else ""

    markup match {
      case Lorcode | LorcodeUlb ⇒
        s"""
           |$link
           |
           |[i]Перемещено ${moveBy.getNick} из $moveFrom[/i]
           |""".stripMargin
      case Html ⇒
        s"""
           |$link<br><i>Перемещено ${moveBy.getNick} из $moveFrom</i>
           |""".stripMargin
      case Markdown ⇒
        s"""
           |$link
           |
           |Перемещено ${moveBy.getNick} из $moveFrom
           |""".stripMargin
    }
  }
}

object MessageTextService {
  def processPostingText(message: String, mode: String): MessageText = {
    mode match {
      case LorcodeUlb.formId ⇒
        MessageText.apply(message, MarkupType.LorcodeUlb)
      case Lorcode.formId ⇒
        MessageText.apply(message, MarkupType.Lorcode)
      case Markdown.formId ⇒
        MessageText.apply(message, MarkupType.Markdown)
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

  // TODO move to LorCodeService
  // раньше это делалось при постинге, теперь будем делать при рендеринге
  def prepareLorcode(text: String): String = ToLorCodeTexFormatter.quote(text, "\n")
  def prepareUlb(text: String): String = ToLorCodeTexFormatter.quote(text, "[br]")

  def postingModeSelector(user: User, defaultMarkup: String): java.util.Map[String, String] = {
    val modes = MarkupPermissions.allowedFormats(user).filter(f ⇒ !f.deprecated || f.formId == defaultMarkup)

    (if (modes.size > 1) {
      ListMap(modes.toSeq.sortBy(_.order).map(m ⇒ m.formId -> m.title): _*)
    } else {
      Map.empty[String, String]
    }).asJava
  }
}

object MarkupPermissions {
  def allowedFormats(user: User): Set[MarkupType] = {
    if (user==null) { // anonymous
      Set(Lorcode)
    } else if (user.isAdministrator) {
      Set(Lorcode, LorcodeUlb, Markdown, Html)
    } else {
      Set(Lorcode, LorcodeUlb)
    }
  }

  def allowedFormatsJava(user: User): java.util.Set[MarkupType] = allowedFormats(user).asJava
}
