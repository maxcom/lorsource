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

package ru.org.linux.util;

import ru.org.linux.util.formatter.RuTypoChanger;
import sun.security.ssl.Debug;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtil {
  private static final Random random = new Random();

  private StringUtil() {
  }

  public static String getFileName(String pathname) {
    StringTokenizer parsed = new StringTokenizer(pathname, "/\\", false);
    String filename = "";
    while (parsed.hasMoreElements()) {
      filename = parsed.nextToken();
    }
    return filename;
  }

  private static final Pattern loginCheckRE = Pattern.compile("[a-z][a-z0-9_-]*");

  public static boolean checkLoginName(String login) {
    login = login.toLowerCase();

    // no zerosize login
    if (login.isEmpty()) {
      return false;
    }
    if (login.length() >= 80) {
      return false;
    }

    return loginCheckRE.matcher(login).matches();
  }

  public static String processTitle(String title) {
    //Debug.println("ProcessTitle", title);
    //return (new RuTypoChanger()).changeBatch(title);
    //Ну что это за фигня, а?  Почему часть шаблона попадает в title?
    return title.replaceAll(" -- ", "&nbsp;&mdash; ");
  }

  public static String makeTitle(String title) {
    if (title != null && !title.trim().isEmpty()) {
      return title;
    }
    return "Без заглавия";
  }

  public static String md5hash(String pass) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
      BigInteger bi = new BigInteger(1, md.digest(pass.getBytes()));
      String hash = bi.toString(16);
      if (hash.length() < 32) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < 32 - hash.length(); i++) {
          buf.append('0');
        }
        buf.append(hash);
//        logger.fine("Calculated hash="+buf.toString()); //$NON-NLS-1$
        return buf.toString();
      } else {
        return hash;
      }
    } catch (GeneralSecurityException gse) {
      throw new RuntimeException(gse);
    }
  }

  public static String generatePassword() {
    StringBuilder builder = new StringBuilder();

    for (int i=0; i<10; i++) {
      int r = Math.abs(random.nextInt());

      builder.append((char) (33 + r%(126-33)));
    }

    return builder.toString();
  }

  /**
   * Convert special SGML (HTML) chars to
   * SGML entities
   */
  private static final Pattern uniRE = Pattern.compile("^&((#[1-9]\\d{1,4})|(\\w{1,8}));");

  /**
   * Экранируем управляющие html символьные последовательности, кроме &#NNNN;
   * @param str сырая строка
   * @return отэкранированная строка
   */
  public static String escapeHtml(String str) {
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
  /**
   * Экранируем управляющие html символьные последовательности, в отличии от
   * escapeHtml &#NNN; тоже экранируем
   * @param str сырая строка
   * @return отэкранированная строка
   */
  public static String escapeForceHtml(String str) {
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
          res.append("&amp;");
          break;
        default:
          res.append(str.charAt(i));
      }

    }

    return res.toString();
  }


  public static String escapeBBCode(String content) {
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

  /**
   * Повторить строку n раз
   * @param s строка
   * @param n сколько повторять строку
   * @return повторенная n раз строка
   */
  public static String repeat(String s, int n) {
    if(s == null) {
      return null;
    }
    if(n <= 0) {
      return s;
    }
    final StringBuilder sb = new StringBuilder(s.length()*n);
    for(int i = 0; i < n; i++) {
        sb.append(s);
    }
    return sb.toString();
  }

  public static boolean isUnsignedPositiveNumber(String s) {
    return s.matches("\\d+");
  }

}