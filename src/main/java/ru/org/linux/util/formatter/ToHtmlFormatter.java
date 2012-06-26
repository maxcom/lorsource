/*
 * Copyright 1998-2012 Linux.org.ru
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

package ru.org.linux.util.formatter;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.comment.CommentService;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.topic.Topic;
import ru.org.linux.user.User;
import ru.org.linux.spring.Configuration;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.comment.Comment;
import ru.org.linux.util.LorURL;
import ru.org.linux.util.StringUtil;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Форматирует сообщение в html
 * Основная функция преобразование того, что похоже на ссылку в html ссылку
 */
@Service
public class ToHtmlFormatter {

  private static final String URL_REGEX = "(?:(?:(?:https?://(?:(?:\\w+\\:)?\\w+@)?)|(?:ftp://(?:(?:\\w+\\:)?\\w+@)?)|(?:www\\.)|(?:ftp\\.))[a-z0-9.-]+(?:\\.[a-z]+)?(?::[0-9]+)?" +
    "(?:/(?:([\\w=?+/\\[\\]~%;,._@#'!\\p{L}:-]|(\\([^\\)]*\\)))*([\\p{L}:'" +
    "\\w=?+/~@%#-]|(?:&[\\w:|\\[\\]$_.+!*'#%(),@\\p{L}=;/-]+)+|(\\([^\\)]*\\))))?)?)" +
    "|(?:mailto: ?[a-z0-9+.]+@[a-z0-9.-]+.[a-z]+)|(?:news:([\\w+]\\.?)+)";

  private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /*
  Замена двойного минуса на тире
   */
  public static final String MDASH_REGEX = " -- ";
  public static final String MDASH_REPLACE = "&nbsp;&mdash; ";

  private Configuration configuration;
  private TopicDao messageDao;

  @Autowired
  private CommentService commentService;

  private int maxLength=80;

  @Autowired
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  @Autowired
  public void setMessageDao(TopicDao messageDao) {
    this.messageDao = messageDao;
  }

  // для тестирования (todo: заюзать SpringContext)
  public void setCommentService(CommentService commentService) {
    this.commentService = commentService;
  }

  // для тестирования
  public void setMaxLength(int maxLength) {
    this.maxLength = maxLength;
  }

  /**
   * Форматирует текст
   * @param text текст
   * @param secure флаг https
   * @return отфарматированный текст
   */
  public String format(String text, boolean secure) {
    String escapedText = StringUtil.escapeHtml(text);


    StringTokenizer st = new StringTokenizer(escapedText, " \n", true);
    StringBuilder sb = new StringBuilder();

    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      String formattedToken = formatURL(token, secure);
      sb.append(formattedToken);
    }

    return sb.toString().replaceAll(MDASH_REGEX, MDASH_REPLACE);
  }

  public String memberURL(User user, boolean secure) throws URIException {
    URI mainUri = configuration.getMainURI();
    String scheme;
    if(secure) {
      scheme = "https";
    } else {
      scheme = "http";
    }
    return (new URI(scheme, null, mainUri.getHost(), mainUri.getPort(), String.format("/people/%s/profile", user.getNick()))).getEscapedURIReference();
  }

  protected String formatURL(String line, boolean secure) {
    StringBuilder out = new StringBuilder();
    Matcher m = URL_PATTERN.matcher(line);
    int index = 0;
    while (m.find()) {
      int start = m.start();
      int end = m.end();

      // обработка начальной части до URL
      out.append(line.substring(index, start));

      // возможно это url
      String mayUrl = line.substring(start, end);
      // href
      String urlHref = mayUrl;

      if (mayUrl.toLowerCase().startsWith("www.")) {
        urlHref = "http://" + mayUrl;
      } else if (mayUrl.toLowerCase().startsWith("ftp.")) {
        urlHref = "ftp://" + mayUrl;
      }

      try {
        LorURL url = new LorURL(configuration.getMainURI(), urlHref);

        if(url.isMessageUrl()) {
          processMessageUrl(secure, out, url);
        } else if(url.isTrueLorUrl()) {
          processGenericLorUrl(secure, out, url);
        } else {
          // ссылка не из lorsource
          String fixedUrlHref = url.toString();
          String fixedUrlBody = url.formatUrlBody(maxLength);
          out.append("<a href=\"").append(fixedUrlHref).append("\">").append(fixedUrlBody).append("</a>");
        }
      } catch (URIException e) {
        // e.printStackTrace();
        // ссылка не ссылка
        out.append(mayUrl);
      }
      index = end;
    }

    // обработка последнего фрагмента
    if (index < line.length()) {
      out.append(line.substring(index));
    }

    return out.toString();
  }

  private void processGenericLorUrl(boolean secure, StringBuilder out, LorURL url) throws URIException {
    // ссылка внутри lorsource исправляем scheme
    String fixedUrlHref = url.fixScheme(secure);
    String fixedUrlBody = url.formatUrlBody(maxLength);
    out.append("<a href=\"").append(fixedUrlHref).append("\">").append(fixedUrlBody).append("</a>");
  }

  /**
   * Ссылка на топик или комментарий
   *
   * @param secure признак того какой надо url: https или http
   * @param out сюда будет записана ссылка
   * @param url исходный url
   * @throws URIException если uri не корректный
   * @throws MessageNotFoundException если uri не корректный
   */
  private void processMessageUrl(boolean secure, StringBuilder out, LorURL url) throws URIException {
    try {
      Topic message = messageDao.getById(url.getMessageId());

      boolean deleted = message.isDeleted();

      if (!deleted && url.isCommentUrl()) {
        Comment comment = commentService.getById(url.getCommentId());

        deleted = comment.isDeleted();
      }

      String urlTitle = StringUtil.escapeHtml(message.getTitle());

      String newUrlHref = url.formatJump(messageDao, secure);
      String fixedUrlBody = url.formatUrlBody(maxLength);

      if (deleted) {
        out.append("<s>");
      }

      out.append("<a href=\"").append(newUrlHref).append("\" title=\"").append(urlTitle).append("\">").append(fixedUrlBody).append("</a>");

      if (deleted) {
        out.append("</s>");
      }
    } catch (MessageNotFoundException ex) {
      out.append("<a href=\"").append(url.toString()).append("\">").append(url.formatUrlBody(maxLength)).append("</a>");
    }
  }
}
