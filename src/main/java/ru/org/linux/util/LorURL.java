/*
 * Copyright 1998-2026 Linux.org.ru
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

import ru.org.linux.group.Group;
import ru.org.linux.group.GroupService;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;

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

  private final LorURI parsed;

  public LorURL(LorURI mainURI, String url) throws LorURIException {
    parsed = LorURI.parse(url);

    _true_lor_url = mainURI.getHost().equalsIgnoreCase(parsed.getHost()) && mainURI.getPort() == parsed.getPort()
        && ("http".equalsIgnoreCase(parsed.getScheme()) || "https".equalsIgnoreCase(parsed.getScheme()));

    findURLIds();
  }
  
  private void findURLIds() throws LorURIException {
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
    return parsed.escaped();
  }

  /**
   * Ссылка является ссылкой на внутренности lorsource
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

  public String formatUrlBody(int maxLength) throws LorURIException {
    String all = parsed.unescaped();
    // Костыль для однобайтовых неудачников
    if (isContainReplacementCharset(all)) {
      all = parsed.escaped();
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
   * @param canonical канонический URL сайта
   * @return исправленный url
   * @throws LorURIException неправильный url
   */
  public String canonize(LorURI canonical) throws LorURIException {
    if(!_true_lor_url) {
      return toString();
    }

    try {
      String path = parsed.getPath();
      String query = parsed.getQuery();
      String fragment = parsed.getFragment();

      return LorURI.create(canonical.getScheme(), canonical.getHost(), canonical.getPort(), path, query, fragment).escaped();
    } catch (LorURIException e) {
      return toString();
    }
  }

  /**
   * Создает url для редиректа на текущее сообщение\комментарий
   * @param messageDao доступ к базе сообщений
   * @param canonical канонический URL сайта
   * @return url для редиректа или пустая строка
   * @throws MessageNotFoundException если нет сообещния
   * @throws LorURIException если url неправильный
   */
  public String formatJump(TopicDao messageDao, GroupService groupService, LorURI canonical) throws MessageNotFoundException, LorURIException {
    if(_topic_id != -1) {
      Topic message = messageDao.getById(_topic_id);

      Group group = groupService.getGroup(message.getGroupId());

      String path = group.getUrl() + _topic_id;
      String query = null;
      if(_comment_id != -1) {
        query = "cid=" + _comment_id;
      }
      return LorURI.create(canonical.getScheme(), canonical.getHost(), canonical.getPort(), path, query, null).escaped();
    }

    return "";
  }
}
