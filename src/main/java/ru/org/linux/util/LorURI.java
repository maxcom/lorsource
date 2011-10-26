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

package ru.org.linux.util;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import ru.org.linux.site.BadGroupException;
import ru.org.linux.site.Group;
import ru.org.linux.site.Message;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.spring.dao.MessageDao;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for lorsource uri
 */
public class LorURI {
  private static final Pattern requestMessagePattern = Pattern.compile("^/\\w+/\\w+/(\\d+)");
  private static final Pattern requestCommentPattern = Pattern.compile("^comment-(\\d+)");

  private final String rawUrl;
  private final URI lorURI;
  private final URI mainURI;
  private final boolean isTrueLorUrl;
  private final boolean isMessageUrl;
  private final int messageId;
  private final boolean isCommentUrl;
  private final int commentId;

  /**
   * Создаем объект с проверкой что url это подмножество mainUrl и
   * попыткой вычеленить из url id топика и комментария
   * если url неправильные генерируем исключение
   * @param mainURI основоной URI сайта из конфигурации
   * @param url обрабатываемый url
   * @throws URIException если url неправильный
   */
  public LorURI(URI mainURI, String url) throws URIException {
    rawUrl = url;
    this.mainURI = mainURI;
    URI uri;
    try {
      uri = new URI(url, true, "UTF-8");
    } catch (URIException e) {
      uri = new URI(url, false, "UTF-8");
    }
    lorURI = uri;
    if(lorURI.getHost() == null) {
      throw new URIException();
    }
    isTrueLorUrl = (mainURI.getHost().equals(lorURI.getHost()) && mainURI.getPort() == lorURI.getPort());

    if(isTrueLorUrl) {
        // find message id in lor url
        int msgId = 0;
        boolean isMsg = false;
        String path = lorURI.getPath();
        if(path != null) {
          Matcher messageMatcher = requestMessagePattern.matcher(path);

          if(messageMatcher.find()) {
            try {
              msgId = Integer.parseInt(messageMatcher.group(1));
              isMsg = true;
            } catch (NumberFormatException e) {
              msgId = 0;
              isMsg = false;
            }
          } else {
            msgId = 0;
            isMsg = false;
          }
        }
        messageId = msgId;
        isMessageUrl = isMsg;

        // find comment id in lor url
        int commId = 0;
        boolean isComm = false;
        String fragment = lorURI.getFragment();
        if(fragment != null) {
          Matcher commentMatcher = requestCommentPattern.matcher(fragment);
          if(commentMatcher.find()) {
            try {
              commId = Integer.parseInt(commentMatcher.group(1));
              isComm = true;
            } catch (NumberFormatException e) {
              commId = 0;
              isComm = false;
            }
          } else {
            commId = 0;
            isComm = false;
          }
        }
        commentId = commId;
        isCommentUrl = isComm;
    } else {
      messageId = 0;
      isMessageUrl = false;
      commentId = 0;
      isCommentUrl = false;
    }
  }

  @Override
  public String toString() {
    return lorURI.getEscapedURIReference();
  }

  /**
   * Ссылка является ссылкой на внтренности lorsource
   * @return true если lorsource ссылка
   */
  public boolean isTrueLorUrl() {
    return isTrueLorUrl;
  }

  /**
   * Ссылка является ссылкой на топик или комментарий в топике
   * @return true если ссылка на топик или комментарий
   */
  public boolean isMessageUrl() {
    return isMessageUrl;
  }

  /**
   * Вовзращает id топика ссылки или 0 если ссылка не на топик или комментарий
   * @return id топика
   */
  public int getMessageId() {
    return messageId;
  }

  /**
   * Ссылка является комментарием в топике
   * @return true если ссылка на комментарий
   */
  public boolean isCommentUrl() {
    return isCommentUrl;
  }

  /**
   * Возвращает id комментария из ссылки или 0 если ссылка не на комментарий
   * @return id комментария
   */
  public int getCommentId() {
    return commentId;
  }

  /**
   * Исправляет scheme url http или https в зависимости от флага secure
   * предполагалось только для lor ссылок, но будет работать с любыми, только зачем?
   * @param secure true если https
   * @return исправленный url
   * @throws URIException неправильный url
   */
  public String fixScheme(boolean secure) throws URIException {
    if(!isTrueLorUrl) {
      return "";
    }
    String scheme;
    if(secure) {
      scheme = "https";
    } else {
      scheme = "http";
    }
    String host = lorURI.getHost();
    int port = lorURI.getPort();
    String path = lorURI.getPath();
    String query = lorURI.getQuery();
    String fragment = lorURI.getFragment();
    URI fixUri = new URI(scheme, null, host, port, path, query, fragment);
    return fixUri.getEscapedURI();
  }

  /**
   * Создает url для редиректа на текущее сообщение\комментарий
   * @param messageDao доступ к базе сообщений
   * @param secure https ли текуший клиент
   * @return url для редиректа или пустая строка
   * @throws MessageNotFoundException если нет сообещния
   * @throws BadGroupException если нет группы оО
   * @throws URIException если url неправильный
   */
  public String formatJump(MessageDao messageDao, boolean secure) throws MessageNotFoundException, BadGroupException, URIException {
    if(isMessageUrl) {
      Message message = messageDao.getById(messageId);
      Group group = messageDao.getGroup(message);
      String scheme;
      if(secure) {
        scheme = "https";
      } else {
        scheme = "http";
      }
      String host = mainURI.getHost();
      int port = mainURI.getPort();
      String path = group.getUrl() + messageId;
      String query = "";
      if(isCommentUrl()) {
        query = "cid=" + commentId;
      }
      URI jumpUri = new URI(scheme, null , host, port, path, query);
      return jumpUri.getEscapedURI();
    }
    return "";
  }
}
