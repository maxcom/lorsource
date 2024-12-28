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
package ru.org.linux.util.bbcode

import org.apache.commons.httpclient.URI
import org.springframework.stereotype.Service
import ru.org.linux.user.User
import ru.org.linux.user.UserService
import ru.org.linux.util.bbcode.LorCodeService.*
import ru.org.linux.util.formatter.{ToHtmlFormatter, ToLorCodeTexFormatter}
import ru.org.linux.util.bbcode.Parser.DEFAULT_PARSER

import scala.jdk.CollectionConverters.SetHasAsScala

@Service
class LorCodeService(userService: UserService, toHtmlFormatter: ToHtmlFormatter) {
  /**
   * Преобразует LORCODE в HTML для комментариев
   * тэги [cut] не отображаются никак
   *
   * @param text     LORCODE
   * @param nofollow add rel=nofollow to links
   * @return HTML
   */
  def parseComment(text: String, nofollow: Boolean, mode: Mode): String =
    DEFAULT_PARSER.parseRoot(prepareCommentRootNode(rss = false, nofollow = nofollow), prepare(mode, text)).renderXHtml

  def parseCommentRSS(text: String, mode: Mode): String =
    DEFAULT_PARSER.parseRoot(prepareCommentRootNode(rss = true, nofollow = false), prepare(mode, text)).renderXHtml

  /**
   * Получить чистый текст из LORCODE текста
   *
   * @param text обрабатываемый текст
   * @return извлеченный текст
   */
  def extractPlain(text: String, mode: Mode): String =
    DEFAULT_PARSER.parseRoot(prepareCommentRootNode(rss = true, nofollow = false), prepare(mode, text)).renderOg

  /**
   * Возвращает множество пользователей, упомянутых в сообщении.
   *
   * @param text сообщение
   * @return множество пользователей
   */
  def getMentions(text: String): collection.Set[User] = {
    val rootNode = DEFAULT_PARSER.parseRoot(prepareCommentRootNode(rss = false, nofollow = false), text)

    rootNode.renderXHtml

    rootNode.getReplier.asScala
  }

  /**
   * Преобразует LORCODE в HTML для топиков со свернутым содержимым тэга cut
   *
   * @param text     LORCODE
   * @param cutURL   абсолютный URL топика
   * @param nofollow add rel=nofollow to links
   * @return HTML
   */
  def parseTopicWithMinimizedCut(text: String, cutURL: String, nofollow: Boolean, mode: Mode): String =
    DEFAULT_PARSER.parseRoot(prepareTopicRootNode(minimizeCut = true, cutURL, nofollow = nofollow), prepare(mode, text)).renderXHtml

  /**
   * Преобразует LORCODE в HTML для топиков со развернутым содержимым тэга cut
   * содержимое тэга cut оборачивается в div с якорем
   *
   * @param text     LORCODE
   * @param nofollow add rel=nofollow to links
   * @return HTML
   */
  def parseTopic(text: String, nofollow: Boolean, mode: Mode): String =
    DEFAULT_PARSER.parseRoot(prepareTopicRootNode(minimizeCut = false, null, nofollow = nofollow), prepare(mode, text)).renderXHtml

  private def prepareCommentRootNode(rss: Boolean, nofollow: Boolean) = {
    val rootNode = DEFAULT_PARSER.createRootNode

    rootNode.setCommentCutOptions()
    rootNode.setUserService(userService)
    rootNode.setToHtmlFormatter(toHtmlFormatter)
    rootNode.setRss(rss)
    rootNode.setNofollow(nofollow)

    rootNode
  }

  private def prepareTopicRootNode(minimizeCut: Boolean, cutURL: String, nofollow: Boolean) = {
    val rootNode = DEFAULT_PARSER.createRootNode

    if (minimizeCut) {
      val fixURI = new URI(cutURL, true, "UTF-8")
      rootNode.setMinimizedTopicCutOptions(fixURI)
    } else {
      rootNode.setMaximizedTopicCutOptions()
    }

    rootNode.setUserService(userService)
    rootNode.setToHtmlFormatter(toHtmlFormatter)
    rootNode.setNofollow(nofollow)

    rootNode
  }
}

object LorCodeService {
  sealed trait Mode
  case object Plain extends Mode
  case object Ulb extends Mode
  case object Lorcode extends Mode

  private def prepare(mode: Mode, value: String): String = mode match {
    case Plain => value
    case Ulb => prepareUlb(value)
    case Lorcode => prepareLorcode(value)
  }

  def prepareUlb(text: String): String = ToLorCodeTexFormatter.quote(text, "[br]")
  def prepareLorcode(text: String): String = ToLorCodeTexFormatter.quote(text, "\n")
}