/*
 * Copyright 1998-2013 Linux.org.ru
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
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.util.LorURL;
import ru.org.linux.util.bbcode.nodes.RootNode;
import ru.org.linux.util.formatter.ToHtmlFormatter;

import java.util.Set;

@Service
public class LorCodeService {
  private static final Parser defaultParser = new Parser(new DefaultParserParameters());

  private UserDao userDao;
  private SiteConfig siteConfig;
  private ToHtmlFormatter toHtmlFormatter;

  @Autowired
  public void setUserDao(UserDao userDao) {
    this.userDao = userDao;
  }

  @Autowired
  public void setSiteConfig(SiteConfig siteConfig) {
    this.siteConfig = siteConfig;
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
   * @param secure является ли текущее соединение secure
   * @param nofollow add rel=nofollow to links
   * @return HTML
   */
  public String parseComment(String text, boolean secure, boolean nofollow) {
    return defaultParser.parseRoot(prepareCommentRootNode(secure, false, nofollow), text).renderXHtml();
  }

  public String parseCommentRSS(String text, boolean secure) {
    return defaultParser.parseRoot(prepareCommentRootNode(secure, true, false), text).renderXHtml();
  }

  /**
   * Получить og:description из LORCODE текста. Тоесть только текст и без содержимого [code]
   * @param text обрабатываемый текст
   * @return og:description
   */
  public String parseForOgDescription(String text) {
    return defaultParser.parseRoot(prepareCommentRootNode(false, true, false), text).renderOg();
  }

  /**
   * Проверяем комментарий на отсутствие текста
   * @param msg текст
   * @return флаг пустоты
   */
  public boolean isEmptyTextComment(String msg) {
    return parseForOgDescription(msg.trim()).isEmpty();
  }

  /**
   * Возвращает множество пользователей упомянутых в сообщении
   * @param text сообщение
   * @return множество пользователей
   */
  public Set<User> getReplierFromMessage(String text) {
    RootNode rootNode = defaultParser.parseRoot(prepareCommentRootNode(false, false, false), text);
    rootNode.renderXHtml();
    return rootNode.getReplier();
  }
  /**
   * Преобразует LORCODE в HTML для топиков со свернутым содержимым тэга cut
   * @param text LORCODE
   * @param cutURL абсолютный URL до топика
   * @param secure является ли текущее соединение secure
   * @param nofollow add rel=nofollow to links
   * @return HTML
   */
  public String parseTopicWithMinimizedCut(String text, String cutURL, boolean secure, boolean nofollow) {
    return defaultParser.parseRoot(prepareTopicRootNode(true, cutURL, secure, nofollow), text).renderXHtml();
  }
  /**
   * Преобразует LORCODE в HTML для топиков со развернутым содержимым тэга cut
   * содержимое тэга cut оборачивается в div с якорем
   * @param text LORCODE
   * @param secure является ли текущее соединение secure
   * @param nofollow add rel=nofollow to links
   * @return HTML
   */
  public String parseTopic(String text, boolean secure, boolean nofollow) {
    return defaultParser.parseRoot(prepareTopicRootNode(false, null, secure, nofollow), text).renderXHtml();
  }

  private RootNode prepareCommentRootNode(boolean secure, boolean rss, boolean nofollow) {
    RootNode rootNode = defaultParser.getRootNode();
    rootNode.setCommentCutOptions();
    rootNode.setUserDao(userDao);
    rootNode.setSecure(secure);
    rootNode.setToHtmlFormatter(toHtmlFormatter);
    rootNode.setRss(rss);
    rootNode.setNofollow(nofollow);

    return rootNode;
  }

  private RootNode prepareTopicRootNode(boolean minimizeCut, String cutURL, boolean secure, boolean nofollow) {
    RootNode rootNode = defaultParser.getRootNode();
    if(minimizeCut) {
      try {
        LorURL lorCutURL = new LorURL(siteConfig.getMainURI(), cutURL);
        if(lorCutURL.isTrueLorUrl()) {
          URI fixURI = new URI(lorCutURL.fixScheme(secure), true, "UTF-8");
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
    rootNode.setNofollow(nofollow);

    return rootNode;
  }
  
  public String prepareTextRSS(String text, boolean secure, boolean lorcode) {
    if (lorcode) {
      return parseCommentRSS(text, secure);
    } else {
      return "<p>" + text + "</p>";
    }
  }
}
