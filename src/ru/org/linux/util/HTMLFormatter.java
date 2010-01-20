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

import java.net.URLEncoder;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.javabb.bbcode.CodeTag;

public class HTMLFormatter {
  private final String text;
  private int maxlength = 80;
  private boolean urlHighlight = false;
  private boolean newLine = false;
  private boolean texNewLine = false;
  private boolean quoting = false;

  private boolean outputLorcode = false;

  public HTMLFormatter(String atext) {
    text = atext;
  }

  private static final Pattern nlRE = Pattern.compile("\r?\n");
  private static final Pattern texnlRE = Pattern.compile("\n\r?\n\r?");

  public String process() {
    String str; 

    if (outputLorcode) {
      str = escapeHtmlBBcode(text);
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
    } catch (Exception e) {
      return str;
    }
  }

  private static final String URL_PATTTERN = "(?:(?:(?:(?:https?://)|(?:ftp://)|(?:www\\.))|(?:ftp\\.))[a-z0-9.-]+(?:\\.[a-z]+)?(?::[0-9]+)?" +
    "(?:/(?:([\\w=?+/\\[\\]~%;,._@#'\\p{L}:-]|(\\([^\\)]*\\)))*([\\p{L}:'" +
    "\\w=?+/~@%#-]|(?:&(?=amp;)[\\w:$_.+!" +
    "*'#%(),@\\p{L}=;/-]+)+|(\\([^\\)]*\\))))?)?)" +
    "|(?:mailto: ?[a-z0-9+]+@[a-z0-9.-]+.[a-z]+)|(?:news:([\\w+]\\.?)+)";

  private static final String URL_PATTTERN_UNESCAPED = "(?:(?:(?:(?:https?://)|(?:ftp://)|(?:www\\.))|(?:ftp\\.))[a-z0-9.-]+(?:\\.[a-z]+)?(?::[0-9]+)?" +
    "(?:/(?:([\\w=?+/\\[\\]~%;,._@#'\\p{L}:-]|(\\([^\\)]*\\)))*([\\p{L}:'" +
    "\\w=?+/~@%#-]|(?:&[\\w:$_.+!*'#%(),@\\p{L}=;/-]+)+|(\\([^\\)]*\\))))?)?)" +
    "|(?:mailto: ?[a-z0-9+]+@[a-z0-9.-]+.[a-z]+)|(?:news:([\\w+]\\.?)+)";

  public static final Pattern urlRE = Pattern.compile(URL_PATTTERN, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  public static final Pattern urlRE_UNESCAPED = Pattern.compile(URL_PATTTERN_UNESCAPED, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  /** форматирует фрагмент исходного текста
   *
   * @param chunk фрагмент текста
   * @return отформатированную строку
   * @throws UtilException в случае некорректного входного текста
   */
  private String formatHTMLLine(String chunk)  {
    StringBuilder out = new StringBuilder();

    Matcher m = (outputLorcode?urlRE_UNESCAPED:urlRE).matcher(chunk);
    int index = 0;

    while (m.find()) {
      int start = m.start();
      int end = m.end();

      // обработка начальной части до URL
      out.append(wrapLongLine(chunk.substring(index, start), maxlength, " ", index));

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
          out.append("<a href=\"").append(URLEncoder(url)).append("\">").append(urlchunk).append("</a>");
        }
      } else {
        out.append(url);
      }
      index = end;
    }

    // обработка последнего фрагмента
    if (index < chunk.length()) {
      if (outputLorcode) {
        out.append(chunk.substring(index));
      } else {
        out.append(wrapLongLine(chunk.substring(index), maxlength, " ", index));
      }
    }

    return out.toString();
  }

  public static int countCharacters(String str) throws UtilException {
    int size = 0;

    try {
      for (Iterator<String> i=new SGMLStringIterator(str); i.hasNext(); ) {
        i.next();
        size++;
      }
    } catch (StringIndexOutOfBoundsException ex) {
      throw new UtilException("Invalid SGML Entity");
    }

    return size;
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

  /** Разбивает слишком длинный фрагмент в строке на части
   *
   * @param line строка
   * @param maxlength максимальная длинна фрагмента
   * @param delim разделитель которым будет разбиваться строка
   * @param start текущая позиция в строке
   * @return разбитая строка
   */
  private static String wrapLongLine(String line, int maxlength, String delim, int start)  {
    StringBuilder sb = new StringBuilder();

    int index = start;

    for (Iterator i = new SGMLStringIterator(line); i.hasNext(); ) {
      String ch = (String) i.next();

      if (index%maxlength == maxlength-1) {
        sb.append(delim);
      }

      sb.append(ch);

      index++;
    }

    return sb.toString();
  }

  /**
   * Wrap long text line
   */
  public static String wrapLongLine(String line, int maxlength, String delim)  {
    return wrapLongLine(line, maxlength, delim, 0);
  }

  /**
   * Wrap long text lines
   */
  public static String wrapLongLines(String text, int maxlength) {
    StringTokenizer st = new StringTokenizer(text, "\n", true);
    StringBuilder sb = new StringBuilder();

    while (st.hasMoreTokens()) {
      sb.append(wrapLongLine(st.nextToken(), maxlength, "\n"));
    }

    return sb.toString();
  }

  public void setOutputLorcode(boolean outputLorcode) {
    this.outputLorcode = outputLorcode;
  }

  public static String escapeHtmlBBcode(String content) {
    // escaping single characters
    content = CodeTag.replaceAll(content, "[]{}\t".toCharArray(), new String[]{
        "&#91;",
        "&#93;",
        "&#123;",
        "&#125;",
        "&nbsp; &nbsp;"});

    return content;
  }

}
