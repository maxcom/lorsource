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

import com.google.common.hash.Hashing;
import com.google.common.html.HtmlEscapers;
import ru.org.linux.util.formatter.RuTypoChanger;
import ru.org.linux.util.formatter.ToHtmlFormatter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

public final class StringUtil {
  private static final SecureRandom random = new SecureRandom();

  private StringUtil() {
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
    return title.trim().replaceAll(ToHtmlFormatter.MDASH_REGEX, ToHtmlFormatter.MDASH_REPLACE);
  }

  public static String makeTitle(String title) {
    if (title != null && !title.trim().isEmpty()) {
      return new RuTypoChanger().format(title);
    }
    return "Без заглавия";
  }

  public static String md5hash(String pass) {
    return Hashing.md5().hashString(pass, StandardCharsets.UTF_8).toString();
  }
  
  public static String hmacSha256(String key, String message) {
    try {
      SecretKeySpec secretKey = new SecretKeySpec(
          key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(secretKey);
      byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new RuntimeException("Failed to compute HMAC-SHA256", e);
    }
  }

  public static boolean verifyHash(String expected, String actual) {
    if (expected == null || actual == null) {
      return false;
    }
    try {
      return MessageDigest.isEqual(
          HexFormat.of().parseHex(expected),
          HexFormat.of().parseHex(actual)
      );
    } catch (Exception e) {
      return false;
    }
  }

  public static String generatePassword() {
    StringBuilder builder = new StringBuilder();

    for (int i=0; i<12; i++) {
      int r = Math.abs(random.nextInt());

      builder.append((char) (33 + r%(126-33)));
    }

    return builder.toString();
  }
  
  /**
   * Экранируем управляющие html символьные последовательности,
   * 
   * @param str сырая строка
   * @return экранированная строка
   */
  public static String escapeHtml(String str) {
    return HtmlEscapers.htmlEscaper().escape(str);
  }

  public static boolean isUnsignedPositiveNumber(String s) {
    return s.matches("\\d+");
  }

  // http://stackoverflow.com/questions/4237625/removing-invalid-xml-characters-from-a-string-in-java
  private static final Pattern INVALID_XML = Pattern.compile("[^"
          + "\u0009\r\n"
          + "\u0020-\uD7FF"
          + "\uE000-\uFFFD"
          + "\ud800\udc00-\udbff\udfff"
          + "]");

  public static String removeInvalidXmlChars(String str) {
    return INVALID_XML.matcher(str).replaceAll("");
  }
}
