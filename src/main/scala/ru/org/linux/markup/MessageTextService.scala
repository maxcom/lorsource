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

package ru.org.linux.markup

import com.google.common.base.Strings
import org.jsoup.Jsoup
import org.springframework.stereotype.Service
import ru.org.linux.markup.MarkupType.*
import ru.org.linux.spring.dao.MessageText
import ru.org.linux.user.User
import ru.org.linux.util.StringUtil
import ru.org.linux.util.bbcode.LorCodeService
import ru.org.linux.util.markdown.MarkdownFormatter

@Service
class MessageTextService(lorCodeService: LorCodeService, markdownFormatter: MarkdownFormatter) {

  /**
    * Получить html представление текста комментария
    *
    * @param text текст комментария
    * @return строку html комментария
    */
  def renderCommentText(text: MessageText, nofollow: Boolean): String = {
    text.markup match {
      case Lorcode =>
        lorCodeService.parseComment(text.text, nofollow, LorCodeService.Lorcode)
      case LorcodeUlb =>
        lorCodeService.parseComment(text.text, nofollow, LorCodeService.Ulb)
      case Html =>
        "<p>" + text.text + "</p>"
      case Markdown =>
        markdownFormatter.renderToHtml(text.text, nofollow)
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
      case Lorcode =>
        lorCodeService.parseCommentRSS(text.text, LorCodeService.Lorcode)
      case LorcodeUlb =>
        lorCodeService.parseCommentRSS(text.text, LorCodeService.Ulb)
      case Html =>
        "<p>" + text.text + "</p>"
      case Markdown =>
        // TODO check if rss needs special rendering
        markdownFormatter.renderToHtml(text.text, nofollow = false)
    }
  }

  def renderTopic(text: MessageText, minimizeCut: Boolean, nofollow: Boolean, canonicalUrl: String): String = {
    text.markup match {
      case Lorcode =>
        if (minimizeCut) {
          lorCodeService.parseTopicWithMinimizedCut(text.text, canonicalUrl, nofollow, LorCodeService.Lorcode)
        } else {
          lorCodeService.parseTopic(text.text, nofollow, LorCodeService.Lorcode)
        }
      case LorcodeUlb =>
        if (minimizeCut) {
          lorCodeService.parseTopicWithMinimizedCut(text.text, canonicalUrl, nofollow, LorCodeService.Ulb)
        } else {
          lorCodeService.parseTopic(text.text, nofollow, LorCodeService.Ulb)
        }
      case Html =>
        "<p>" + text.text
      case Markdown =>
        if (minimizeCut) {
          markdownFormatter.renderWithMinimizedCut(text.text, nofollow, canonicalUrl)
        } else {
          markdownFormatter.renderToHtml(text.text, nofollow)
        }
    }
  }

  def extractPlainText(text: MessageText): String = {
    text.markup match {
      case Lorcode =>
        lorCodeService.extractPlain(text.text, LorCodeService.Lorcode)
      case LorcodeUlb =>
        lorCodeService.extractPlain(text.text, LorCodeService.Ulb)
      case Html =>
        Jsoup.parse(text.text).text
      case Markdown =>
        markdownFormatter.renderToText(text.text)
    }
  }

  def mentions(text: MessageText): collection.Set[User] = {
    text.markup match {
      case Lorcode | LorcodeUlb =>
        lorCodeService.getMentions(text.text)
      case Html =>
        Set.empty[User]
      case Markdown =>
        markdownFormatter.mentions(text.text)
    }
  }

  def isEmpty(text: MessageText): Boolean = extractPlainText(text).trim.isEmpty

  // TODO надо бы извести эту логику; для moveBy/moveFrom есть история; url/linktext лучше просто показывать всегда
  def moveInfo(markup: MarkupType, url: String, linktext: String, moveBy: User, moveFrom: String): String = {
    /* if url is not null, update the topic text */
    val link = if (!Strings.isNullOrEmpty(url)) {
      // TODO escape linktext everywhere; encode url in html

      markup match {
        case Html =>
          s"""<br><a href="$url">$linktext</a>
             |<br>
             |""".stripMargin
        case Lorcode | LorcodeUlb =>
          s"""
             |[url=$url]$linktext[/url]
             |""".stripMargin
        case Markdown =>
          s"""
             |[$linktext]($url)
             |""".stripMargin
      }
    } else ""

    markup match {
      case Lorcode | LorcodeUlb =>
        s"""
           |$link
           |
           |[i]Перемещено ${moveBy.getNick} из $moveFrom[/i]
           |""".stripMargin
      case Html =>
        s"""
           |$link<br><i>Перемещено ${moveBy.getNick} из $moveFrom</i>
           |""".stripMargin
      case Markdown =>
        s"""
           |$link
           |
           |Перемещено ${moveBy.getNick} из $moveFrom
           |""".stripMargin
    }
  }
}

object MessageTextService {
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