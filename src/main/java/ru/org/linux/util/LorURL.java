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
package ru.org.linux.util;

import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import ru.org.linux.group.Group;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.org.linux.util.StringUtil.isUnsignedPositiveNumber;

public class LorURL {
  private static final Pattern requestMessagePattern = Pattern.compile("^/[\\w-]+/[\\w-]+/(\\d+)");
  private static final Pattern requestCommentPattern = Pattern.compile("^comment-(\\d+)");
  private static final Pattern requestCommentPatternNew = Pattern.compile("cid=(\\d+)");
  private static final Pattern requestOldJumpPathPattern = Pattern.compile("^/jump-message.jsp$");
  private static final Pattern requestOldJumpQueryPattern = Pattern.compile("^msgid=(\\d+)&amp;cid=(\\d+)");
  
  private boolean _true_lor_url = false;

  private int _topic_id = -1;
  private int _comment_id = -1;

  private final URI parsed;

  private static class Impl extends URI {
    Impl(String url) throws URIException {
      protocolCharset = "UTF-8";

      try {
        parseUriReference(url, true);

        /*
         * Пытаемся вычислить, что fragment таки не encode
         */
        if (_fragment != null) {
          String fragmentStr = new String(_fragment);
          String asciiFragment = fragmentStr.replaceAll("[^\\p{ASCII}]", "");

          if (fragmentStr.length() != asciiFragment.length()) {
            throw new URIException("error fragment?");
          }
        }

        getQuery(); // check if we can decode it
      } catch (URIException ex) {
        parseUriReference(url, false);
      }

      if (_host == null) {
        throw new URIException("no host");
      }
    }
  }

  public LorURL(URI mainURI, String url) throws URIException {
    parsed = new Impl(url);

    char[] _main_host = mainURI.getRawHost();
    int _main_port = mainURI.getPort();

    char[] _https_scheme = "https".toCharArray();
    char[] _http_scheme = "http".toCharArray();

    _true_lor_url = Arrays.equals(_main_host, parsed.getRawHost()) && _main_port == parsed.getPort()
        && (Arrays.equals(_http_scheme, parsed.getRawScheme()) || Arrays.equals(_https_scheme, parsed.getRawScheme()));

    findURLIds();
  }
  
  private void findURLIds() throws URIException {
    if(_true_lor_url) {
      // find message id in lor url
      String path = parsed.getPath();
      String query = parsed.getQuery();
      String fragment = parsed.getFragment();
      
      if (path != null && query != null) {
        Matcher oldJumpPathMatcher = requestOldJumpPathPattern.matcher(path);
        Matcher oldJumpQueryMatcher = requestOldJumpQueryPattern.matcher(query);
        if (oldJumpPathMatcher.find() && oldJumpQueryMatcher.find()) {
          if (isUnsignedPositiveNumber(oldJumpQueryMatcher.group(1)) &&
              isUnsignedPositiveNumber(oldJumpQueryMatcher.group(2))) {
            _topic_id = Integer.parseInt(oldJumpQueryMatcher.group(1));
            _comment_id = Integer.parseInt(oldJumpQueryMatcher.group(2)); 
          }
        }
      }
      
      if (path != null && _topic_id == -1) {
        Matcher messageMatcher = requestMessagePattern.matcher(path);

        if (messageMatcher.find()) {
          if(isUnsignedPositiveNumber(messageMatcher.group(1))) {
            try {
              _topic_id = Integer.parseInt(messageMatcher.group(1));
            } catch (NumberFormatException ex) {
            }
          }
        }
        if(path.endsWith("/history") || path.endsWith("/comments")) {
          _topic_id = -1;
        }
      }

      if (fragment != null && _topic_id != -1) {
        Matcher commentMatcher = requestCommentPattern.matcher(fragment);
        if (commentMatcher.find()) {
          if(isUnsignedPositiveNumber(commentMatcher.group(1))) {
            try {
              _comment_id = Integer.parseInt(commentMatcher.group(1));
            } catch (NumberFormatException ex) {
            }
          }
        }
      }

      if (query != null && _topic_id != -1) {
        Matcher commentMatcher = requestCommentPatternNew.matcher(query);
        if (commentMatcher.find()) {
          if(isUnsignedPositiveNumber(commentMatcher.group(1))) {
            _comment_id = Integer.parseInt(commentMatcher.group(1));
          }
        }
      }
    }
  }

  /**
   * Возвращает escaped URL
   * @return url
   */
  @Override
  public String toString() {
    return parsed.getEscapedURIReference();
  }

  /**
   * Ссылка является ссылкой на внтренности lorsource
   * @return true если lorsource ссылка
   */
  public boolean isTrueLorUrl() {
    return _true_lor_url;
  }

  /**
   * Ссылка является ссылкой на топик или комментарий в топике
   * @return true если ссылка на топик или комментарий
   */
  public boolean isMessageUrl() {
    return _topic_id != -1;
  }

  /**
   * Вовзращает id топика ссылки или 0 если ссылка не на топик или комментарий
   * @return id топика
   */
  public int getMessageId() {
    return _topic_id;
  }

  /**
   * Ссылка является комментарием в топике
   * @return true если ссылка на комментарий
   */
  public boolean isCommentUrl() {
    return _comment_id != -1;
  }

  /**
   * Возвращает id комментария из ссылки или 0 если ссылка не на комментарий
   * @return id комментария
   */
  public int getCommentId() {
    return _comment_id;
  }

  /**
   * Ищет в стороке символ который полчается если строка однобайтовая вместо предполагаемого utf8
   * @param str строка для проверки
   * @return флажок
   */
  private boolean isContainReplacementCharset(String str) {
    for(char c : str.toCharArray()) {
      if(c == 65533) {
        return true;
      }
    }
    return false;
  }

  public String formatUrlBody(int maxLength) throws URIException {
    String all = parsed.getURIReference();
    // Костыль для однобайтовых неудачников
    if (isContainReplacementCharset(all)) {
      all = parsed.getEscapedURIReference();
    }
    String scheme = parsed.getScheme();
    String uriWithoutScheme = all.substring(scheme.length()+3);
    int trueMaxLength = maxLength - 3; // '...'
    if(_true_lor_url) {
      if(uriWithoutScheme.length() < maxLength + 1) {
        return uriWithoutScheme;
      } else {
        String hostPort = parsed.getHost();
        if(parsed.getPort() != -1) {
          hostPort += ":" + parsed.getPort();
        }
        if(hostPort.length() > maxLength) {
          return hostPort+"/...";
        } else {
          return uriWithoutScheme.substring(0, trueMaxLength) + "...";
        }
      }
    } else {
      if(all.length() < maxLength + 1) {
        return all;
      } else {
        return all.substring(0, trueMaxLength) + "...";
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
    if(!_true_lor_url) {
      return toString();
    }
    String host = parsed.getHost();
    int port = parsed.getPort();
    String path = parsed.getPath();
    String query = parsed.getQuery();
    String fragment = parsed.getFragment();
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
   * @throws URIException если url неправильный
   */
  public String formatJump(TopicDao messageDao, boolean secure) throws MessageNotFoundException, URIException {
    if(_topic_id != -1) {
      Topic message = messageDao.getById(_topic_id);

      Group group = messageDao.getGroup(message);

      String scheme;
      if(secure) {
        scheme = "https";
      } else {
        scheme = "http";
      }
      String host = parsed.getHost();
      int port = parsed.getPort();
      String path = group.getUrl() + _topic_id;
      String query = "";
      if(_comment_id != -1) {
        query = "cid=" + _comment_id;
      }
      URI jumpUri = new URI(scheme, null , host, port, path, query);
      return jumpUri.getEscapedURI();
    }
    
    return "";
  }
}
