/*
 * Copyright 1998-2010 Linux.org.ru
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.dao.MessageDao;
import ru.org.linux.dao.UserDao;
import ru.org.linux.dto.UserDto;
import ru.org.linux.spring.Configuration;
import ru.org.linux.util.LorURI;
import ru.org.linux.util.bbcode.nodes.RootNode;
import ru.org.linux.util.formatter.ToHtmlFormatter;

import java.util.Set;

@Service
public class LorCodeService {
  private static final Parser defaultParser = new Parser(new DefaultParserParameters());

  @Autowired
  UserDao userDao;

  @Autowired
  Configuration configuration;

  @Autowired
  MessageDao messageDao;

  @Autowired
  ToHtmlFormatter toHtmlFormatter;

  /**
   * Преобразует LORCODE в HTML для комментариев
   * тэги [cut] не отображаются никак
   * @param text LORCODE
   * @param secure является ли текущее соединение secure
   * @return HTML
   */
  public String parseComment(String text, boolean secure) {
    return defaultParser.parseRoot(prepareCommentRootNode(secure, false), text).renderXHtml();
  }

  public String parseCommentRSS(String text, boolean secure) {
    return defaultParser.parseRoot(prepareCommentRootNode(secure, true), text).renderXHtml();
  }

  /**
   * Возвращает множество пользователей упомянутых в сообщении
   * @param text сообщение
   * @return множество пользователей
   */
  public Set<UserDto> getReplierFromMessage(String text) {
    RootNode rootNode = defaultParser.parseRoot(prepareCommentRootNode(false, false), text);
    rootNode.renderXHtml();
    return rootNode.getReplier();
  }
  /**
   * Преобразует LORCODE в HTML для топиков со свернутым содержимым тэга cut
   * @param text LORCODE
   * @param cutURL абсолютный URL до топика
   * @param secure является ли текущее соединение secure
   * @return HTML
   */
  public String parseTopicWithMinimizedCut(String text, String cutURL, boolean secure) {
    return defaultParser.parseRoot(prepareTopicRootNode(true, cutURL, secure), text).renderXHtml();
  }
  /**
   * Преобразует LORCODE в HTML для топиков со развернутым содержимым тэга cut
   * содержимое тэга cut оборачивается в div с якорем
   * @param text LORCODE
   * @param secure является ли текущее соединение secure
   * @return HTML
   */
  public String parseTopic(String text, boolean secure) {
    return defaultParser.parseRoot(prepareTopicRootNode(false, null, secure), text).renderXHtml();
  }

  private RootNode prepareCommentRootNode(boolean secure, boolean rss) {
    RootNode rootNode = defaultParser.getRootNode();
    rootNode.setCommentCutOptions();
    rootNode.setUserDao(userDao);
    rootNode.setSecure(secure);
    rootNode.setToHtmlFormatter(toHtmlFormatter);
    rootNode.setRss(rss);

    return rootNode;
  }

  private RootNode prepareTopicRootNode(boolean minimizeCut, String cutURL, boolean secure) {
    RootNode rootNode = defaultParser.getRootNode();
    if(minimizeCut) {
      try {
        LorURI cutURI = new LorURI(configuration.getMainURI(), cutURL);
        if(cutURI.isTrueLorUrl()) {
          URI fixURI = new URI(cutURI.fixScheme(secure), true, "UTF-8");
          rootNode.setMinimizedTopicCutOptions(fixURI);
        } else {
          rootNode.setMaximizedTopicCutOptions();
        }
      } catch (Exception e) {
        rootNode.setMaximizedTopicCutOptions();
      }
    } else {
      rootNode.setMaximizedTopicCutOptions();
    }
    rootNode.setUserDao(userDao);
    rootNode.setSecure(secure);
    rootNode.setToHtmlFormatter(toHtmlFormatter);

    return rootNode;
  }
}
