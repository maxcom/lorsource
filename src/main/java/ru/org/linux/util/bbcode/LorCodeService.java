/*
 * Copyright 1998-2022 Linux.org.ru
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

package ru.org.linux.util.bbcode;

import org.apache.commons.httpclient.URI;
import org.springframework.stereotype.Service;
import ru.org.linux.user.User;
import ru.org.linux.user.UserService;
import ru.org.linux.util.bbcode.nodes.RootNode;
import ru.org.linux.util.formatter.ToHtmlFormatter;

import java.util.Set;

import static ru.org.linux.util.bbcode.Parser.DEFAULT_PARSER;

@Service
public class LorCodeService {
  private final UserService userService;
  private final ToHtmlFormatter toHtmlFormatter;

  public LorCodeService(UserService userService, ToHtmlFormatter toHtmlFormatter) {
    this.userService = userService;
    this.toHtmlFormatter = toHtmlFormatter;
  }

  /**
   * Преобразует LORCODE в HTML для комментариев
   * тэги [cut] не отображаются никак
   *
   * @param text LORCODE
   * @param nofollow add rel=nofollow to links
   * @return HTML
   */
  public String parseComment(String text, boolean nofollow) {
    return DEFAULT_PARSER.parseRoot(prepareCommentRootNode(false, nofollow), text).renderXHtml();
  }

  public String parseCommentRSS(String text) {
    return DEFAULT_PARSER.parseRoot(prepareCommentRootNode(true, false), text).renderXHtml();
  }

  /**
   * Получить чистый текст из LORCODE текста
   *
   * @param text обрабатываемый текст
   * @return извлеченный текст
   */
  public String extractPlainTextFromLorcode(String text) {
    return DEFAULT_PARSER.parseRoot(prepareCommentRootNode(true, false), text).renderOg();
  }

  /**
   * Возвращает множество пользователей упомянутых в сообщении
   * @param text сообщение
   * @return множество пользователей
   */
  public Set<User> getReplierFromMessage(String text) {
    RootNode rootNode = DEFAULT_PARSER.parseRoot(prepareCommentRootNode(false, false), text);
    rootNode.renderXHtml();
    return rootNode.getReplier();
  }
  /**
   * Преобразует LORCODE в HTML для топиков со свернутым содержимым тэга cut
   * @param text LORCODE
   * @param cutURL абсолютный URL до топика
   * @param nofollow add rel=nofollow to links
   * @return HTML
   */
  public String parseTopicWithMinimizedCut(String text, String cutURL, boolean nofollow) {
    return DEFAULT_PARSER.parseRoot(prepareTopicRootNode(true, cutURL, nofollow), text).renderXHtml();
  }
  /**
   * Преобразует LORCODE в HTML для топиков со развернутым содержимым тэга cut
   * содержимое тэга cut оборачивается в div с якорем
   * @param text LORCODE
   * @param nofollow add rel=nofollow to links
   * @return HTML
   */
  public String parseTopic(String text, boolean nofollow) {
    return DEFAULT_PARSER.parseRoot(prepareTopicRootNode(false, null, nofollow), text).renderXHtml();
  }

  private RootNode prepareCommentRootNode(boolean rss, boolean nofollow) {
    RootNode rootNode = DEFAULT_PARSER.createRootNode();
    rootNode.setCommentCutOptions();
    rootNode.setUserService(userService);
    rootNode.setToHtmlFormatter(toHtmlFormatter);
    rootNode.setRss(rss);
    rootNode.setNofollow(nofollow);

    return rootNode;
  }

  private RootNode prepareTopicRootNode(boolean minimizeCut, String cutURL, boolean nofollow) {
    RootNode rootNode = DEFAULT_PARSER.createRootNode();
    if(minimizeCut) {
      try {
        URI fixURI = new URI(cutURL, true, "UTF-8");
        rootNode.setMinimizedTopicCutOptions(fixURI);
      } catch (Exception e) {
        rootNode.setMaximizedTopicCutOptions();
      }
    } else {
      rootNode.setMaximizedTopicCutOptions();
    }
    rootNode.setUserService(userService);
    rootNode.setToHtmlFormatter(toHtmlFormatter);
    rootNode.setNofollow(nofollow);

    return rootNode;
  }
}
