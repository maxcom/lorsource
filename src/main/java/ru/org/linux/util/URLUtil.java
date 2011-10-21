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

/* (C) Max Valjansky,
       Anastasiya Mishechkina 2000
*/

package ru.org.linux.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class URLUtil {
  private static final Pattern isUrl = Pattern.compile(
    "(((https?)|(ftp))://(([0-9\\p{L}.-]+\\.\\p{L}+)|(\\d+\\.\\d+\\.\\d+\\.\\d+))(:[0-9]+)?(/[^ ]*)?)|(mailto:[a-z0-9_+-.]+@[0-9a-z.-]+\\.[a-z]+)|(news:[a-z0-9.-]+)|(((www)|(ftp))\\.(([0-9a-z.-]+\\.[a-z]+(:[0-9]+)?(/[^ ]*)?)|([a-z]+(/[^ ]*)?)))",
    Pattern.CASE_INSENSITIVE
  );

  private static final Pattern requestMessagePattern = Pattern.compile("\\w+/\\w+/(\\d+)");
  private static final Pattern requestCommentPattern = Pattern.compile("#comment-(\\d+)");

  private URLUtil() {
  }

  @Deprecated
  public static String checkAndFixURL(String url) throws BadURLException {
    url = url.trim();

    if (isUrl(url)) {
      if (url.toLowerCase().startsWith("www.")) {
        return "http://" + url;
      }
      if (url.toLowerCase().startsWith("ftp.")) {
        return "ftp://" + url;
      }
      return url;
    }

    throw new BadURLException(url);
  }

  public static String fixURL(String url) {
    url = url.trim();

    if (isUrl(url)) {
      if (url.toLowerCase().startsWith("www.")) {
        return "http://" + url;
      }
      if (url.toLowerCase().startsWith("ftp.")) {
        return "ftp://" + url;
      }
      return url;
    }

    return url;
  }

  /**
   * Откусываем от url http\https или оставляем как есть
   * @param url обрабатываемый url
   * @return обкусанный url
   */
  private static String cropSchemeFromUrl(String url) {
    String newUrl;
    if(url.startsWith("http://")) {
      newUrl = url.substring(8);
    } else if(url.startsWith("https://")) {
      newUrl = url.substring(9);
    } else {
      newUrl = url;
    }
    return newUrl;
  }

  /**
   * Возвращает запрос из URL, если URL начинается с MainUrl
   * тоесть все что после MainUrl иначе пустую строку
   * @param mainUrl по идее MainUrl из properties
   * @param url URL который обрабатываем
   * @return значимую часть URL без MainUrl
   */
  public static String getRequestFromUrl(String mainUrl, String url) {
    // MainUrl http://127.0.0.1:8080/
    // Request https://127.0.0.1:8080/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917
    String tempMainUrl = cropSchemeFromUrl(mainUrl);
    String tempUrl = cropSchemeFromUrl(url);

    if(tempUrl.startsWith(tempMainUrl)) {
      return tempUrl.substring(tempMainUrl.length());
    } else {
      return "";
    }
  }

  /**
   * Из запроса который возвращает getRequestFromUrl пытается достать id топика
   * если не удается то 0
   * @param request запрос
   * @return id топика
   */
  public static int getMessageIdFromRequest(String request) {
    if(request.length() == 0) {
      return 0;
    }
    Matcher m = requestMessagePattern.matcher(request);
    if(m.find()) {
      try {
        return Integer.parseInt(m.group(1));
      } catch (NumberFormatException e) {
        return 0;
      }
    }
    return 0;
  }

  /**
   * Из запроса который возвращает getRequestFromUrl пытается достать id клмментария
   * если не удается то 0
   * @param request запрос
   * @return id топика
   */
  public static int getCommentIdFromRequest(String request) {
    if(request.length() == 0) {
      return 0;
    }
    Matcher m = requestCommentPattern.matcher(request);
    if(m.find()) {
      try {
        return Integer.parseInt(m.group(1));
      } catch (NumberFormatException e) {
        return 0;
      }
    }
    return 0;
  }

  /**
   * Создает валидный url перехода к конкретному комментарию
   * @param mainUrl главный url :-|
   * @param msgid id топика
   * @param cid id коментария
   * @return пустую строку если что-то не так или валидный jump
   */
  public static String formatLorUrl(String mainUrl, int msgid, int cid, boolean secure) {
    if(msgid == 0 || cid ==0 || "".equals(mainUrl)) {
      return "";
    }
    String cropMainUrl = cropSchemeFromUrl(mainUrl);
    String scheme;
    if(secure) {
      scheme = "https://";
    } else {
      scheme = "http://";
    }

    return String.format("%s%s/jump-message.jsp?msgid=%d&cid=%d", scheme, mainUrl, msgid, cid);
  }

  public static boolean isSecureUrl(String url) {
    return url.startsWith("https://");
  }



  public static boolean isUrl(String x) {
    return isUrl.matcher(x).matches();
  }
}

