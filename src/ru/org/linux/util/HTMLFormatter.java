/*
 * Copyright 1998-2009 Linux.org.ru
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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;

public class HTMLFormatter {
  private final String text;
  private String nl = " ";
  private int maxlength = 80;
  private boolean urlHighlight = false;
  private boolean preformat = false;
  private boolean NewLine = false;
  private boolean texNewLine = false;
  private boolean quoting = false;
  private String delim = " \n";

  public HTMLFormatter(String atext) {
    text = atext;
  }

  private static final Pattern nlRE;
  private static final Pattern texnlRE;

  static {
    try {
      nlRE = Pattern.compile("\n");
      texnlRE = Pattern.compile("\n\r?\n\r?");
    } catch (PatternSyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public String process() {
    StringTokenizer st = new StringTokenizer(htmlSpecialChars(text), delim, true);

    StringBuilder sb = new StringBuilder();

    while (st.hasMoreTokens()) {
      sb.append(formatHTMLLine(st.nextToken()));
    }

    String res = sb.toString();

    if (NewLine) {      
      res = nl2br(res, quoting);
    }
    if (texNewLine) {
      res = texnl2br(res, quoting);
    }

    if (preformat) {
      res = "<pre>" + res + "</pre>";
    }

    return res;
  }

  public void setMaxLength(int value) {
    maxlength = value;
  }

  public void enableUrlHighLightMode() {
    urlHighlight = true;
  }

  public void enablePreformatMode() {
    preformat = true;
    nl = "\n";
    delim = " \n";
  }

  public void enableNewLineMode() {
    NewLine = true;
    texNewLine = false;
  }

  public void enableTexNewLineMode() {
    NewLine = false;
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

  private static final Pattern urlRE;

  static {
    try {

//      urlRE = new RE("(?:(?:(?:(?:https?://)|(?:ftp://)|(?:www\\.))|(?:ftp\\.))[a-z0-9.-]+\\.[a-z]+(?::[0-9]+)?(?:/(?:[\\w=?:+/\\(\\)\\[\\]~&%;,._#-]*[\\w=?+/~&%-])?)?)|(?:mailto: ?[a-z0-9+]+@[a-z0-9.-]+.[a-z]+)", RE.REG_ICASE);
        /*urlRE = new RE("(?:(?:(?:(?:https?://)|(?:ftp://)|(?:www\\.))|(?:ftp\\.))[a-z0-9.-]+\\.[a-z]+(?::[" +
          "0-9]+)?(?:/(?:([\\w=?:+/\\[\\]~&%;,._#-]|(\\([^\\)]*\\)))*([\\w=?+/~&%-]|(\\([^\\)]*\\))))" +
          "?)?)|(?:mailto: ?[a-z0-9+]+@[a-z0-9.-]+.[a-z]+)", RE.REG_ICASE);*/
      //fix #73: allow only &amp; entity in url    "[\\w$-_.+!*'(),\\u0999]+"
        urlRE = Pattern.compile("(?:(?:(?:(?:https?://)|(?:ftp://)|(?:www\\.))|(?:ftp\\.))[a-z0-9.-]+(?:\\.[a-z]+)?(?::[0-9]+)?(?:/(?:([\\w=?+/\\[\\]~%;,._@\\#'\\p{InCyrillic}:-]|(\\([^\\)]*\\)))*([\\p{InCyrillic}:'\\w=?+/~@%-]|(?:&(?=amp;)[\\w:$_.+!*'#(),@\\p{InCyrillic}=;-]+)+|(\\([^\\)]*\\))))?)?)|(?:mailto: ?[a-z0-9+]+@[a-z0-9.-]+.[a-z]+)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    } catch (PatternSyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /** форматирует фрагмент исходного текста
   *
   * @param chunk фрагмент текста
   * @return отформатированную строку
   * @throws UtilException в случае некорректного входного текста
   */
  private String formatHTMLLine(String chunk)  {
    StringBuilder out = new StringBuilder();

    Matcher m = urlRE.matcher(chunk);

    int index = 0;

    while (m.find()) {
      int start = m.start();
      int end = m.end();

      // обработка начальной части до URL
      out.append(wrapLongLine(chunk.substring(index, start), maxlength, nl, index));

      // обработка URL
      String url = chunk.substring(start, end);
      if (urlHighlight) {
        String urlchunk = url;

        if (url.toLowerCase().startsWith("www.")) {
          url = "http://" + url;
        } else if (url.toLowerCase().startsWith("ftp.")) {
          url = "ftp://" + url;
        }

        if (!preformat && urlchunk.length() > maxlength) {
          urlchunk = urlchunk.substring(0, maxlength - 3) + "...";
        }

        out.append("<a href=\"").append(URLEncoder(url)).append("\">").append(urlchunk).append("</a>");
      } else {
        out.append(url);
      }
      index = end;
    }

    // обработка последнего фрагмента
    if (index < chunk.length()) {
      out.append(preformat?chunk.substring(index):wrapLongLine(chunk.substring(index), maxlength, nl, index));
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
    return nl2br(text,false);
  }

  /**
   * converts new line characters in input string to
   * HTML line brake tag
   */
  private static String nl2br(String text, boolean quoting) {
    if (!quoting) {
      return text.replaceAll(nlRE.pattern(), "<br>");
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
          buf.append("</i>");
        }

        if (text.substring(i).trim().startsWith("&gt;")) {
          quot = true;
          buf.append("<i>");
        } 
        
        if (text.charAt(i) == '\n') {
          buf.append("<br>");
        }
      }

      buf.append(text.charAt(i));
    }

    if (quot) {
      buf.append("</i>");
    }

    return buf.toString();  
  }

  /**
   * converts double new line characters in input string to
   * HTML paragraph tag
   */
  static String texnl2br(String text, boolean quoting) {
    if (!quoting) {
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
            buf.append("</i>");
          }

          if (i != 0) {
            buf.append("<p>");
          }

          if (text.substring(i).trim().startsWith("&gt;")) {
            quot = true;
            buf.append("<i>");
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
      buf.append("</i>");
    }

    return buf.toString();
  }


  /**
   * Convert special SGML (HTML) chars to
   * SGML entities
   */  
  private static final Pattern uniRE;

  static {
     try {
       uniRE = Pattern.compile("^&#[1-9]\\d{1,4};");
     } catch (PatternSyntaxException e) {
       throw new RuntimeException(e);
     }
   }
  
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

}
