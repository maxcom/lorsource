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

/* (C) Max Valjanski,
       Anastasiya Mishechkina
*/

package ru.org.linux.util;

import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.spring.dao.MessageDao;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLFormatter {
  private final String text;
  private int maxlength = 80;
  private boolean urlHighlight = false;
  private boolean newLine = false;
  private boolean texNewLine = false;
  private boolean quoting = false;

  private boolean outputLorcode = false;

  private String mainUrl = "";
  private boolean secure = false;
  private MessageDao messageDao = null;

  public HTMLFormatter(String atext) {
    text = atext;
  }

  private static final Pattern nlRE = Pattern.compile("\r?\n");
  private static final Pattern texnlRE = Pattern.compile("\n\r?\n\r?");

  public String process() {
    String str; 

    if (outputLorcode) {
      str = text.replaceAll("\\[(/?code)\\]", "[[$1]]");
//      str = escapeHtmlBBcode(text);
    } else {
      str = htmlSpecialChars(text);
    }

    StringTokenizer st = new StringTokenizer(str, " \n", true);

    StringBuilder sb = new StringBuilder();

    while (st.hasMoreTokens()) {
      sb.append(formatHTMLLine(st.nextToken()));
    }

    String res = sb.toString();

    if (newLine) {
      res = nl2br(res, quoting, outputLorcode);
    }
    if (texNewLine) {
      res = texnl2br(res, quoting, outputLorcode);
    }

    return res;
  }

  public void setMainUrl(String mainUrl) {
    this.mainUrl = mainUrl;
  }

  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  public void setMessageDao(MessageDao messageDao) {
    this.messageDao = messageDao;
  }

  public void setMaxLength(int value) {
    maxlength = value;
  }

  public void enableUrlHighLightMode() {
    urlHighlight = true;
  }

  public void enableNewLineMode() {
    newLine = true;
    texNewLine = false;
  }

  public void enableTexNewLineMode() {
    newLine = false;
    texNewLine = true;
  }

  public void enableQuoting() {
    quoting = true;
  }

  private static String URLEncoder(String str) {
    try {
      StringBuilder buf = new StringBuilder();
      for (int i = 0; i < str.length(); i++) {
        char c = str.charAt(i);
        if (c > ' ' && c <= 'z') {
          buf.append(c);
        } else {
          buf.append(URLEncoder.encode(String.valueOf(c), "UTF-8"));
        }
      }
      return buf.toString();
    } catch (UnsupportedEncodingException e) {
      return str;
    }
  }

  private static final String URL_PATTTERN = "(?:(?:(?:(?:https?://)|(?:ftp://)|(?:www\\.))|(?:ftp\\.))[a-z0-9.-]+(?:\\.[a-z]+)?(?::[0-9]+)?" +
    "(?:/(?:([\\w=?+/\\[\\]~%;,._@#'!\\p{L}:-]|(\\([^\\)]*\\)))*([\\p{L}:'" +
    "\\w=?+/~@%#-]|(?:&(?=amp;)[\\w:$_.+!" +
    "*'#%(),@\\p{L}=;/-]+)+|(\\([^\\)]*\\))))?)?)" +
    "|(?:mailto: ?[a-z0-9+.]+@[a-z0-9.-]+.[a-z]+)|(?:news:([\\w+]\\.?)+)";

  private static final String URL_PATTTERN_UNESCAPED = "(?:(?:(?:(?:https?://)|(?:ftp://)|(?:www\\.))|(?:ftp\\.))[a-z0-9.-]+(?:\\.[a-z]+)?(?::[0-9]+)?" +
    "(?:/(?:([\\w=?+/\\[\\]~%;,._@#'!\\p{L}:-]|(\\([^\\)]*\\)))*([\\p{L}:'" +
    "\\w=?+/~@%#-]|(?:&[\\w:$_.+!*'#%(),@\\p{L}=;/-]+)+|(\\([^\\)]*\\))))?)?)" +
    "|(?:mailto: ?[a-z0-9+.]+@[a-z0-9.-]+.[a-z]+)|(?:news:([\\w+]\\.?)+)";

  public static final Pattern urlRE = Pattern.compile(URL_PATTTERN, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  public static final Pattern urlRE_UNESCAPED = Pattern.compile(URL_PATTTERN_UNESCAPED, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /** форматирует фрагмент исходного текста
   *
   * @param chunk фрагмент текста
   * @return отформатированную строку
   * @throws UtilException в случае некорректного входного текста
   */
  private String formatHTMLLine(String chunk) {
    StringBuilder out = new StringBuilder();

    Matcher m = (outputLorcode?urlRE_UNESCAPED:urlRE).matcher(chunk);
    int index = 0;

    while (m.find()) {
      int start = m.start();
      int end = m.end();

      // обработка начальной части до URL
      out.append(chunk.substring(index, start));

      // обработка URL
      String url = chunk.substring(start, end);
      if (urlHighlight) {
        String urlchunk = url;

        if (url.toLowerCase().startsWith("www.")) {
          url = "http://" + url;
        } else if (url.toLowerCase().startsWith("ftp.")) {
          url = "ftp://" + url;
        }

        if (urlchunk.length() > maxlength) {
          urlchunk = urlchunk.substring(0, maxlength - 3) + "...";
        }

        if (outputLorcode) {
          out.append("[url=").append(URLEncoder(url)).append(']').append(urlchunk).append("[/url]");
        } else {
          try {
            LorURI lorURI = new LorURI(mainUrl, url);
            if(lorURI.isMessageUrl()) {
              // Ссылка на топик или комментарий
              String title;
              try {
                title = escapeHtmlBBcode(messageDao.getById(lorURI.getMessageId()).getTitle());
              } catch (MessageNotFoundException e) {
                title = "Комментарий в несуществующем топике";
              }
              String newurl = lorURI.formatJump(messageDao, secure);
              out.append("<a href=\"").append(newurl).append("\" title=\"").append(title).append("\">").append(newurl).append("</a>");
            } else if(lorURI.isTrueLorUrl()) {
              // ссылка внутри lorsource исправляем scheme
              String fixedUri = lorURI.fixScheme(secure);
              out.append("<a href=\"").append(fixedUri).append("\">").append(fixedUri).append("</a>");
            } else {
              out.append("<a href=\"").append(URLEncoder(url)).append("\">").append(urlchunk).append("</a>");
            }
          } catch (Exception e) {
            out.append("<a href=\"").append(URLEncoder(url)).append("\">").append(urlchunk).append("</a>");
          }
        }
      } else {
        out.append(url);
      }
      index = end;
    }

    // обработка последнего фрагмента
    if (index < chunk.length()) {
      out.append(chunk.substring(index));
    }

    return out.toString();
  }

  /**
   * converts new line characters in input string to
   * HTML line brake tag
   */
  public static String nl2br(String text) {
    return nl2br(text,false, false);
  }

  /**
   * converts new line characters in input string to
   * HTML line brake tag
   */
  static String nl2br(String text, boolean quoting, boolean outputLorcode) {
    if (!quoting) {
      if (outputLorcode) {
        return text.replaceAll(nlRE.pattern(), "[br]\n");
      } else {
        return text.replaceAll(nlRE.pattern(), "<br>");
      }
    }

    StringBuilder buf = new StringBuilder();

    boolean quot = false;

    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '\r') {
        continue;
      }
      if (text.charAt(i) == '\n' || i == 0) {
        if (quot) {
          quot = false;
          if (outputLorcode) {
            buf.append("[/i]");
          } else {
            buf.append("</i>");
          }
        }

        if (outputLorcode) {
          if (text.substring(i).trim().startsWith(">")) {
            quot = true;
            buf.append("[i]");
          }
        } else {
          if (text.substring(i).trim().startsWith("&gt;")) {
            quot = true;
            buf.append("<i>");
          }
        }

        if (text.charAt(i) == '\n') {
          if (outputLorcode) {
            buf.append("[br]");
          } else {
            buf.append("<br>");
          }
        }
      }

      buf.append(text.charAt(i));
    }

    if (quot) {
      if (outputLorcode) {
        buf.append("[/i]");
      } else {
        buf.append("</i>");
      }
    }

    return buf.toString();  
  }

  /**
   * converts double new line characters in input string to
   * HTML paragraph tag
   */
  static String texnl2br(String text, boolean quoting, boolean outputLorcode) {
    if (!quoting) {
      if (outputLorcode) {
        return text;
      }

      return text.replaceAll(texnlRE.pattern(), "<p>");
    }

    StringBuilder buf = new StringBuilder();

    boolean cr = false;
    boolean quot = false;

    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '\r') {
        continue;
      }
      if (text.charAt(i) == '\n' || i == 0) {
        if (cr || i == 0) {
          if (quot) {
            quot = false;
            if (outputLorcode) {
              buf.append("[/i]\n");
            } else {
              buf.append("</i>");
            }
          }

          if (i != 0 && !outputLorcode) {
            buf.append("<p>");
          }

          if (outputLorcode) {
            if (text.substring(i).trim().startsWith(">")) {
              quot = true;
              buf.append("\n[i]");
            }
          } else {
            if (text.substring(i).trim().startsWith("&gt;")) {
              quot = true;
              buf.append("<i>");
            }
          }
        } else {
          cr = true;
        }
      } else {
        cr = false;
      }

      buf.append(text.charAt(i));
    }

    if (quot) {
      if (outputLorcode) {
        buf.append("[/i]");
      } else {
        buf.append("</i>");
      }
    }

    return buf.toString();
  }


  /**
   * Convert special SGML (HTML) chars to
   * SGML entities
   */  
  private static final Pattern uniRE = Pattern.compile("^&((#[1-9]\\d{1,4})|(\\w{1,8}));");

  public static String htmlSpecialChars(String str) {
    StringBuilder res = new StringBuilder();

    for (int i = 0; i < str.length(); i++) {
      switch (str.charAt(i)) {
        case '<':
          res.append("&lt;");
          break;
        case '>':
          res.append("&gt;");
          break;
        case '\"':
          res.append("&quot;");
          break;
        case '&':
          Matcher m = uniRE.matcher(str.substring(i));
          if (m.find()) {
              String s = m.group();
              res.append(s);
              i+=s.length()-1;
              continue;
          } else {
            res.append("&amp;");
          }

          break;
        default:
          res.append(str.charAt(i));
      }

    }

    return res.toString();
  }

  public void setOutputLorcode(boolean outputLorcode) {
    this.outputLorcode = outputLorcode;
  }

  public static String escapeHtmlBBcode(String content) {
    // escaping single characters
    content = replaceAll(content, "[]{}\t".toCharArray(), new String[]{
        "&#91;",
        "&#93;",
        "&#123;",
        "&#125;",
        "&nbsp; &nbsp;"});

    return content;
  }

  public static String replaceAll(CharSequence str, char[] chars, String[] replacement) {
    StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      boolean matched = false;
      for (int j = 0; j < chars.length; j++) {
        if (c == chars[j]) {
          buffer.append(replacement[j]);
          matched = true;
        }
      }
      if (!matched) {
        buffer.append(c);
      }
    }
    return buffer.toString();
  }
}
