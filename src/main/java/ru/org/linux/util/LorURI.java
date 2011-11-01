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

import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
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
  private static final Pattern requestMessagePattern = Pattern.compile("^/[\\w-]+/[\\w-]+/(\\d+)");
  private static final Pattern requestCommentPattern = Pattern.compile("^comment-(\\d+)");
  private static final Pattern requestConmmentPatternNew = Pattern.compile("cid=(\\d+)");
  private static final Pattern requestOldJumpPathPattern = Pattern.compile("^/jump-message.jsp$");
  private static final Pattern requestOldJumpQueryPattern = Pattern.compile("^msgid=(\\d+)&amp;cid=(\\d+)");

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
    /*
    это uri из lorsouce если хост и порт совпадают и scheme http или https
     */
    isTrueLorUrl = (mainURI.getHost().equals(lorURI.getHost()) && mainURI.getPort() == lorURI.getPort()
                  && ("http".equals(lorURI.getScheme()) || "https".equals(lorURI.getScheme())));

    if (isTrueLorUrl) {
      // find message id in lor url
      int msgId = 0;
      int commId = 0;
      boolean isMsg = false;
      boolean isComm = false;
      if(lorURI.getPath() != null && lorURI.getQuery() != null) {
        Matcher oldJumpPathMatcher = requestOldJumpPathPattern.matcher(lorURI.getPath());
        Matcher oldJumpQueryMatcher = requestOldJumpQueryPattern.matcher(lorURI.getQuery());
        if(oldJumpPathMatcher.find() && oldJumpQueryMatcher.find()) {
          try {
            msgId = Integer.parseInt(oldJumpQueryMatcher.group(1));
            commId = Integer.parseInt(oldJumpQueryMatcher.group(2));
            isMsg = true;
            isComm = true;
          } catch (NumberFormatException e) {
            msgId = 0;
            commId = 0;
            isMsg = false;
            isComm = false;
          }
        }
      }
      String path = lorURI.getPath();
      if (path != null && !isMsg) {
        Matcher messageMatcher = requestMessagePattern.matcher(path);

        if (messageMatcher.find()) {
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
      String fragment = lorURI.getFragment();
      if (fragment != null) {
        Matcher commentMatcher = requestCommentPattern.matcher(fragment);
        if (commentMatcher.find()) {
          try {
            commId = Integer.parseInt(commentMatcher.group(1));
            isComm = true;
          } catch (NumberFormatException e) {
            commId = 0;
            isComm = false;
          }
        }
      }

      if (lorURI.getQuery()!=null) {
        Matcher commentMatcher = requestConmmentPatternNew.matcher(lorURI.getQuery());
        if (commentMatcher.find()) {
          try {
            commId = Integer.parseInt(commentMatcher.group(1));
            isComm = true;
          } catch (NumberFormatException e) {
            commId = 0;
            isComm = false;
          }
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

  /**
   * Возвращает escaped URL
   * @return url
   */
  @Override
  public String toString() {
    return lorURI.getEscapedURIReference();
  }

  /**
   * Пытается вернуть unescaped URL если не получится то возвращает escaped
   * @return url
   */
  public String toUnEscapedString() {
    try {
      return lorURI.getURIReference();
    } catch (URIException e) {
      return toString();
    }
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

  public String formatUrlBody(int maxLength) throws URIException {
    String all = lorURI.getURIReference();
    String scheme = lorURI.getScheme();
    String uriWithoutScheme = all.substring(scheme.length()+3);
    if(uriWithoutScheme.length() < maxLength) {
      return uriWithoutScheme;
    } else {
      String hostPort = lorURI.getHost();
      if(lorURI.getPort() != -1) {
        hostPort += ":" + lorURI.getPort();
      }
      if(hostPort.length() > maxLength) {
        return hostPort+"/...";
      } else {
        return uriWithoutScheme.substring(0, maxLength) + "...";
      }
    }
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
      return toString();
    }
    String host = lorURI.getHost();
    int port = lorURI.getPort();
    String path = lorURI.getPath();
    String query = lorURI.getQuery();
    String fragment = lorURI.getFragment();
    if(!secure) {
      return (new HttpURL(null, host, port, path, query, fragment)).getEscapedURIReference();
    } else {
      return (new HttpsURL(null, host, port, path, query, fragment)).getEscapedURIReference();
    }
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
