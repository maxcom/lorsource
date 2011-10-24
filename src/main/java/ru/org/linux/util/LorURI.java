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

  private final URI lorUri;
  private final URI mainUri;
  private final boolean isTrueLorUrl;
  private final boolean isMessageUrl;
  private final int messageId;
  private final boolean isCommentUrl;
  private final int commentId;

  /**
   * Создаем объект с проверкой что url это подмножество mainUrl и
   * попыткой вычеленить из url id топика и комментария
   * если mainUrl или url неправильные генерируем исключение
   * @param mainUrl основоной url сайта
   * @param url обрабатываемый url
   * @throws URIException если что-то не так в mainUrl или url
   */
  public LorURI(String mainUrl, String url) throws URIException{
    lorUri = new URI(url, true, "UTF-8");
    mainUri = new URI(mainUrl, true, "UTF-8");
    isTrueLorUrl = (lorUri.getHost().equals(mainUri.getHost()) && lorUri.getPort() == mainUri.getPort());

    if(isTrueLorUrl) {

      // find message id in lor url
      int msgId = 0;
      boolean isMsg = false;
      String path = lorUri.getPath();
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
      String fragment = lorUri.getFragment();
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

  public boolean isTrueLorUrl() {
    return isTrueLorUrl;
  }

  public boolean isMessageUrl() {
    return isMessageUrl;
  }

  public int getMessageId() {
    return messageId;
  }

  public boolean isCommentUrl() {
    return isCommentUrl;
  }

  public int getCommentId() {
    return commentId;
  }

  public String fixScheme(boolean secure) throws URIException {
    String scheme;
    if(secure) {
      scheme = "https";
    } else {
      scheme = "http";
    }
    String host = lorUri.getHost();
    int port = lorUri.getPort();
    String path = lorUri.getPath();
    String query = lorUri.getQuery();
    String fragment = lorUri.getFragment();
    URI fixUri = new URI(scheme, null, host, port, path, query, fragment);
    return fixUri.getEscapedURI();
  }

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
      String host = mainUri.getHost();
      int port = mainUri.getPort();
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
