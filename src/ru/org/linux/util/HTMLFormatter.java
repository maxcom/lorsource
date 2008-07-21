/* (C) Max Valjanski,
       Anastasiya Mishechkina
*/

package ru.org.linux.util;

import java.net.URLEncoder;
import java.util.Iterator;
import java.util.StringTokenizer;

import gnu.regexp.RE;
import gnu.regexp.REException;
import gnu.regexp.REMatch;
import gnu.regexp.REMatchEnumeration;

public class HTMLFormatter {
  private final String text;
  private String nl = " ";
  private int maxlength = 80;
  private boolean urlHighlight = false;
  private boolean Preformat = false;
  private boolean NewLine = false;
  private boolean plainTextInput = false;
  private boolean htmlInput = false;
  private boolean texNewLine = false;
  private boolean quoting = false;
  private String delim = " \n";

  public HTMLFormatter(String atext) {
    text = atext;
  }

  private static final RE nlRE;
  private static final RE texnlRE;

  static {
    try {
      nlRE = new RE("\n");
      texnlRE = new RE("\n\r?\n\r?");
    } catch (REException e) {
      throw new RuntimeException(e);
    }
  }

  public String process() throws UtilException {
    if (htmlInput) {
      checkHTML();
    }

    StringTokenizer st;
    if (plainTextInput) {
      st = new StringTokenizer(htmlSpecialChars(text), delim, true);
    } else {
      st = new StringTokenizer(text, delim, true);
    }

    StringBuffer sb = new StringBuffer();

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

    if (Preformat) {
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
    Preformat = true;
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

  public void enablePlainTextMode() {
    plainTextInput = true;
    htmlInput = false;
  }

  public void enableCheckHTML() {
    htmlInput = true;
    plainTextInput = false;
  }

  public void enableQuoting() {
    quoting = true;
  }

  public static String URLEncoder(String str) {
    try {
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < str.length(); i++) {
        char c = str.charAt(i);
        if (c > ' ' && c <= 'z') {
          buf.append(c);
        } else {
          buf.append(URLEncoder.encode("" + c, "UTF-8"));
        }
      }
      return buf.toString();
    } catch (Exception e) {
      return str;
    }
  }

  private static final RE urlRE;

  static {
    try {
      urlRE = new RE("(?:(?:(?:(?:https?://)|(?:ftp://)|(?:www\\.))|(?:ftp\\.))[a-z0-9.-]+\\.[a-z]+(?::[0-9]+)?(?:/(?:[\\w=?:+/\\(\\)\\[\\]~&%;,._#-]*[\\w=?+/~&%-])?)?)|(?:mailto: ?[a-z0-9+]+@[a-z0-9.-]+.[a-z]+)", RE.REG_ICASE);
    } catch (REException e) {
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
    StringBuffer out = new StringBuffer();

    REMatchEnumeration en = urlRE.getMatchEnumeration(chunk);
    int index = 0;

    while (en.hasMoreElements()) {
      REMatch found = en.nextMatch();

      // обработка начальной части до URL
      out.append(wrapLongLine(chunk.substring(index, found.getStartIndex()), maxlength, nl, index));

      // обработка URL
      String url = chunk.substring(found.getStartIndex(), found.getEndIndex());
      if (urlHighlight) {
        String urlchunk = url;

        if (url.toLowerCase().startsWith("www.")) {
          url = "http://" + url;
        } else if (url.toLowerCase().startsWith("ftp.")) {
          url = "ftp://" + url;
        }

        if (Preformat) {
          urlchunk = wrapLongLine(urlchunk, maxlength, nl, found.getStartIndex());
        } else if (urlchunk.length() > maxlength) {
          urlchunk = urlchunk.substring(0, maxlength - 3) + "...";
        }
        out.append("<a href=\"").append(URLEncoder(url)).append("\">").append(urlchunk).append("</a>");
      } else {
        out.append(url);
      }
      index = found.getEndIndex();
    }

    // обработка последнего фрагмента
    if (index < chunk.length()) {
      out.append(wrapLongLine(chunk.substring(index), maxlength, nl, index));
    }

    return out.toString();
  }

  public static int countCharacters(String str) throws UtilException {
    int size = 0;

    try {
      for (Iterator i=new SGMLStringIterator(str); i.hasNext(); ) {
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
  public static String nl2br(String text, boolean quoting) {
    if (!quoting) {
      return nlRE.substituteAll(text, "<br>");
    }

    StringBuffer buf = new StringBuffer();

    boolean cr = false;
    boolean quot = false;
    boolean skip = false;

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
            skip = true;
          }

          if (text.substring(i).trim().startsWith("&gt;")) {
            quot = true;
            buf.append("<i>");
          }
        } else {
          cr = true;
        }
        
        if (text.charAt(i) == '\n') {
          if (skip) {
            skip = false;
          } else {
            buf.append("<br>");
          }
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
   * converts double new line characters in input string to
   * HTML paragraph tag
   */
  public static String texnl2br(String text, boolean quoting) {
    if (!quoting) {
      return texnlRE.substituteAll(text, "<p>");
    }

    StringBuffer buf = new StringBuffer();

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
  private static final RE uniRE;

  static {
     try {
       uniRE = new RE("^&#[1-9]\\d{1,4};");
     } catch (REException e) {
       throw new RuntimeException(e);
     }
   }
  
  public static String htmlSpecialChars(String str) {
    StringBuffer res = new StringBuffer();

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
          REMatch m = uniRE.getMatch(str.substring(i));
          if ((m instanceof REMatch) ) {
              String s = m.toString();
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

  private static final RE tagRE;
  private static final RE tagREAnchor;

  static {
    try {
      tagRE = new RE("(<p>)|(<br>)|(<li>)", RE.REG_ICASE);
      tagREAnchor = new RE("(<p>)|(<br>)|(<li>)|(<a +href=\"[^<> \"]+\">[^><]+</a>)", RE.REG_ICASE);
    } catch (REException e) {
      throw new RuntimeException(e);
    }
  }

  private void checkHTML() throws UtilBadHTMLException {
    String str;

    if (!urlHighlight) {
      str = tagREAnchor.substituteAll(text, "");
    } else {
      str = tagRE.substituteAll(text, "");
    }

    if (str.indexOf('<') != -1 || str.indexOf('>') != -1) {
      throw new UtilBadHTMLException();
    }
  }

  /** Разбивает слишком длинный фрагмент в строке на части
   *
   * @param line строка
   * @param maxlength максимальная длинна фрагмента
   * @param delim разделитель которым будет разбиваться строка
   * @param start текущая позиция в строке
   * @return разбитая строка
   */
  public static String wrapLongLine(String line, int maxlength, String delim, int start)  {
    StringBuffer sb = new StringBuffer();

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
    StringBuffer sb = new StringBuffer();

    while (st.hasMoreTokens()) {
      sb.append(wrapLongLine(st.nextToken(), maxlength, "\n"));
    }

    return sb.toString();
  }

}
