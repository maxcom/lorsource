/*
 * Copyright 1998-2017 Linux.org.ru
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
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.user.User;
import ru.org.linux.user.UserService;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.bbcode.nodes.RootNode;
import ru.org.linux.util.formatter.ToHtmlFormatter;

import java.util.Set;

import static ru.org.linux.util.bbcode.Parser.DEFAULT_PARSER;

@Service
public class LorCodeService {
  private UserService userService;
  private ToHtmlFormatter toHtmlFormatter;

  @Autowired
  public void setUserService(UserService userService) {
    this.userService = userService;
  }

  @Autowired
  public void setToHtmlFormatter(ToHtmlFormatter toHtmlFormatter) {
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
   * Обрезать чистый текст до заданого размера
   *
   * @param plainText обрабатываемый текст (не lorcode!)
   * @param maxLength обрезать текст до указанной длинны
   * @param encodeHtml экранировать теги
   *
   * @return обрезанный текст
   */
  public String trimPlainText(String plainText, int maxLength, boolean encodeHtml) {
    String cut;

    if(plainText.length() < maxLength) {
      cut = plainText;
    } else {
      cut = plainText.substring(0, maxLength).trim() + "...";
    }

    if (encodeHtml) {
      return StringUtil.escapeForceHtml(cut);
    } else {
      return cut;
    }
  }

  public String extractPlainText(MessageText text) {
    if (text.isLorcode()) {
      return extractPlainTextFromLorcode(text.getText());
    } else {
      return Jsoup.parse(text.getText()).text();
    }
  }

  /**
   * Проверяем комментарий на отсутствие текста
   * @param msg текст
   * @return флаг пустоты
   */
  public boolean isEmptyTextComment(String msg) {
    return extractPlainTextFromLorcode(msg.trim()).isEmpty();
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
  
  public String prepareTextRSS(String text, boolean lorcode) {
    if (lorcode) {
      return parseCommentRSS(text);
    } else {
      return "<p>" + text + "</p>";
    }
  }
}
