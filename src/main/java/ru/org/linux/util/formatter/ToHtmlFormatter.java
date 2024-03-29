/*
 * Copyright 1998-2023 Linux.org.ru
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
import ru.org.linux.comment.Comment;
import ru.org.linux.comment.CommentDao;
import ru.org.linux.group.GroupDao;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.user.User;
import ru.org.linux.util.LorURL;
import ru.org.linux.util.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Форматирует сообщение в html
 * Основная функция преобразование того, что похоже на ссылку в html ссылку
 */
@Service
public class ToHtmlFormatter {

  private static final String URL_REGEX = "(?<![\\w./])(?:(?:(?:https?://(?:(?:\\w+\\:)?\\w+@)?)|(?:ftp://(?:(?:\\w+\\:)?\\w+@)?)|(?:www\\.)|(?:ftp\\.))[a-z0-9.-]+(?:\\.[a-z]+)?(?::[0-9]+)?" +
    "(?:/(?:([\\w=?+/\\[\\]~%;,._@#'!\\p{L}:-]|(\\([^\\)]*\\)))*([\\p{L}:'" +
    "\\w=?+/~@%#-]|(?:&[\\w:|\\[\\]$_.+!*'#%(),@\\p{L}=;/-]+)+|(\\([^\\)]*\\))))?)?)" +
    "|(?:mailto: ?[a-z0-9+.]+@[a-z0-9.-]+.[a-z]+)|(?:news:([\\w+]\\.?)+)";

  private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /*
  Замена двойного минуса на тире
  */

  public static final String MDASH_REGEX = " -- ";
  public static final String MDASH_REPLACE = "&nbsp;&mdash; ";

  private SiteConfig siteConfig;
  private TopicDao topicDao;

  @Autowired
  private CommentDao commentDao;

  private GroupDao groupDao;

  private int maxLength=80;

  @Autowired
  public void setSiteConfig(SiteConfig siteConfig) {
    this.siteConfig = siteConfig;
  }

  @Autowired
  public void setTopicDao(TopicDao topicDao) {
    this.topicDao = topicDao;
  }

  @Autowired
  public void setGroupDao(GroupDao groupDao) {
    this.groupDao = groupDao;
  }

  // для тестирования (todo: заюзать SpringContext)
  public void setCommentDao(CommentDao commentDao) {
    this.commentDao = commentDao;
  }

  // для тестирования
  public void setMaxLength(int maxLength) {
    this.maxLength = maxLength;
  }

  /**
   * Форматирует текст
   *
   * @param text текст
   * @return отфарматированный текст
   */
  public String format(String text, boolean nofollow) {
    return format(text, nofollow, null);
  }

  public String format(String text, boolean nofollow, RuTypoChanger changer) {
    String escapedText = StringUtil.escapeHtml(text);

    StringTokenizer st = new StringTokenizer(escapedText, " \n", true);
    StringBuilder sb = new StringBuilder();

    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      String formattedToken = formatURL(token, nofollow, changer);
      sb.append(formattedToken);
    }

    return sb.toString().replaceAll(MDASH_REGEX, MDASH_REPLACE);
  }

  /**
   * Только escape и замены
   * @param text текст
   * @return форматированый текст
   */
  public String simpleFormat(String text) {
    return StringUtil.escapeHtml(text).replaceAll(MDASH_REGEX, MDASH_REPLACE);
  }

  private String formatWithMagic(String text, RuTypoChanger changer) {
    return changer!=null ? changer.format(text) : text;
  }

  public String memberURL(User user) throws URIException {
    URI mainUri = siteConfig.getSecureURI();

    return (new URI(mainUri.getScheme(), null, mainUri.getHost(), mainUri.getPort(), String.format("/people/%s/profile", user.getNick()))).getEscapedURIReference();
  }

  private String formatURL(String line, boolean nofollow, RuTypoChanger changer) {
    StringBuilder out = new StringBuilder();
    Matcher m = URL_PATTERN.matcher(line);
    int index = 0;
    while (m.find()) {
      int start = m.start();
      int end = m.end();

      // обработка начальной части до URL
      out.append(formatWithMagic(line.substring(index, start), changer));

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
        processUrl(nofollow, out, urlHref, null);
      } catch (URIException e) {
        // e.printStackTrace();
        // ссылка не ссылка
        out.append(formatWithMagic(mayUrl, changer));
      }
      index = end;
    }

    // обработка последнего фрагмента
    if (index < line.length()) {
      out.append(formatWithMagic(line.substring(index), changer));
    }

    return out.toString();
  }

  public void processUrl(
          boolean nofollow,
          @Nonnull StringBuilder out,
          @Nonnull String urlHref,
          @Nullable String linktext
  ) throws URIException {
    LorURL url = new LorURL(siteConfig.getMainURI(), urlHref);

    if(url.isMessageUrl()) {
      processMessageUrl(out, url, linktext);
    } else if(url.isTrueLorUrl()) {
      processGenericLorUrl(out, url, linktext);
    } else {
      // ссылка не из lorsource
      String fixedUrlHref = url.toString();
      String fixedUrlBody = url.formatUrlBody(maxLength);

      out.append("<a href=\"").append(fixedUrlHref).append("\"");
      if (nofollow) {
        out.append(" rel=nofollow");
      }
      out.append(">");

      if (linktext!=null) {
        out.append(simpleFormat(linktext));
      } else {
        out.append(simpleFormat(fixedUrlBody));
      }

      out.append("</a>");
    }
  }

  private void processGenericLorUrl(
          @Nonnull StringBuilder out,
          @Nonnull LorURL url,
          @Nullable String linktext
  ) throws URIException {
    // ссылка внутри lorsource исправляем scheme
    String fixedUrlHref = url.canonize(siteConfig.getSecureURI());
    String fixedUrlBody = linktext!=null?simpleFormat(linktext):StringUtil.escapeHtml(url.formatUrlBody(maxLength));
    out.append("<a href=\"").append(fixedUrlHref).append("\">").append(fixedUrlBody).append("</a>");
  }

  /**
   * Ссылка на топик или комментарий
   *
   * @param out сюда будет записана ссылка
   * @param url исходный url
   * @throws URIException если uri не корректный
   */
  private void processMessageUrl(
          @Nonnull StringBuilder out,
          @Nonnull LorURL url,
          @Nullable String linkText
  ) throws URIException {
    try {
      Topic message = topicDao.getById(url.getMessageId());

      boolean deleted = message.isDeleted();

      if (!deleted && url.isCommentUrl()) {
        Comment comment = commentDao.getById(url.getCommentId());

        deleted = comment.isDeleted();
      }

      String urlTitle = linkText!=null?simpleFormat(linkText):StringUtil.escapeHtml(message.getTitle());

      String newUrlHref = url.formatJump(topicDao, groupDao, siteConfig.getSecureURI());
      String fixedUrlBody = url.formatUrlBody(maxLength);

      if (deleted) {
        out.append("<s>");
      }
      
      out.append("<a href=\"").append(newUrlHref).append("\" title=\"").append(urlTitle).append("\">");

      if (deleted) {
        out.append(StringUtil.escapeHtml(fixedUrlBody));
      } else {
        out.append(urlTitle);

        if (url.isCommentUrl()) {
          out.append(" (комментарий)");
        }
      }

      out.append("</a>");

      if (deleted) {
        out.append("</s>");
      }
    } catch (MessageNotFoundException ex) {
      out.append("<a href=\"").append(url).append("\">").append(StringUtil.escapeHtml(url.formatUrlBody(maxLength))).append("</a>");
    }
  }
}
