/*
 * Copyright 1998-2024 Linux.org.ru
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

import java.util.regex.Pattern;

public class RuTypoChanger {
  /*
  Константы замены кавычек на няшне
   */

  public static final char QUOTE_SYMBOL = '"';

  public static final char QUOTE_OUT_OPEN = '«';
  public static final char QUOTE_OUT_CLOSE = '»';
  public static final char QUOTE_IN_OPEN = '„';
  public static final char QUOTE_IN_CLOSE = '“';

  public static final String QUOTE_OUT_OPEN_HTML = "&#171;"; // "&laquo;";
  public static final String QUOTE_OUT_CLOSE_HTML = "&#187;";// "&raquo;";
  public static final String QUOTE_IN_OPEN_HTML = "&#8222;"; //"&bdquo;";
  public static final String QUOTE_IN_CLOSE_HTML = "&#8220;";// "&ldquo;";

  private static final char[] PUNCTUATION = {'.', ',', ':', ';', '-', '!', '?', '(', ')'};

  private int quoteDepth = 0;
  private CharSequence localBuff = "";

  private final static Pattern QUOTE_PATTERN = Pattern.compile("&quot;", Pattern.LITERAL);
  private final static Pattern QUOTE_CHAR_PATTERN = Pattern.compile("(''|\")");

  private final static Pattern QUOTE_IN_OPEN_PATTERN =
          Pattern.compile(Character.toString(QUOTE_IN_OPEN), Pattern.LITERAL);
  private final static Pattern QUOTE_IN_CLOSE_PATTERN =
          Pattern.compile(Character.toString(QUOTE_IN_CLOSE), Pattern.LITERAL);
  private final static Pattern QUOTE_OUT_OPEN_PATTERN =
          Pattern.compile(Character.toString(QUOTE_OUT_OPEN), Pattern.LITERAL);
  private final static Pattern QUOTE_OUT_CLOSE_PATTERN =
          Pattern.compile(Character.toString(QUOTE_OUT_CLOSE), Pattern.LITERAL);

  private static boolean isQuoteChar(char ch) {
    return ch == QUOTE_SYMBOL ||
            ch == QUOTE_OUT_OPEN || ch == QUOTE_OUT_CLOSE ||
            ch == QUOTE_IN_OPEN || ch == QUOTE_IN_CLOSE;
  }

  private static boolean isPunctuation(char ch) {
    for (char test: PUNCTUATION)
      if (test == ch)
        return true;
    return false;
  }

  private static char firstNonQuote(CharSequence buff, int start) {
    for (int pt = start - 1; pt >= 0; pt--) {
      if (!isQuoteChar(buff.charAt(pt)))
        return buff.charAt(pt);
    }
    return buff.charAt(0);
  }

  private static char lastNonQuote(CharSequence buff, int start) {
    for (int pt = start + 1; pt < buff.length(); pt++) {
      if (!isQuoteChar(buff.charAt(pt)))
        return buff.charAt(pt);
    }
    return buff.charAt(buff.length() - 1);
  }


  private static boolean isQuoteOpening(CharSequence buff, int position) {
    char before, after;

    if (position == buff.length() - 1)
      return false;
    else if (position == 0)
      before = '\0';
    else
      before = firstNonQuote(buff, position);

    after = lastNonQuote(buff, position);

    if (Character.isWhitespace(after) || isPunctuation(after))
      return false;

    if (Character.isLetterOrDigit(before))
      return false;

    // русский авось всегда спасет. авось прокатит :)
    return true;
  }

  private boolean isQuoteClosing(CharSequence buff, int position) {
    char before, after;

    if (position == 0 && localBuff.length()==0)
      return false;
    else if (position == buff.length() - 1)
      return true;

    after = lastNonQuote(buff, position);

    if (position == 0)
      before = firstNonQuote(localBuff, localBuff.length());
    else
      before = firstNonQuote(buff, position);

    if (isQuoteChar(before))
      return false;

    if (Character.isLetterOrDigit(after))
      return false;

    return true;
  }

  /**
   * Делает всякие типографичские штучки (тире и кавычки).
   *
   * @param input
   * @return форматированный текст
   */

  public String format(String input) {
    StringBuilder buff = new StringBuilder(QUOTE_PATTERN.matcher(input).replaceAll("\""));

    for (int iter = 0; iter < buff.length(); iter++) {
      if (buff.charAt(iter) == QUOTE_SYMBOL) {
        if (isQuoteClosing(buff, iter) && quoteDepth > 0) {
          if (quoteDepth == 1)
            buff.setCharAt(iter, QUOTE_OUT_CLOSE);
          else
            buff.setCharAt(iter, QUOTE_IN_CLOSE);
          quoteDepth--;
        }
        else
        if (isQuoteOpening(buff, iter)) { //убеждаемся, что всё так
          if (quoteDepth == 0)
            buff.setCharAt(iter, QUOTE_OUT_OPEN);
          else
            buff.setCharAt(iter, QUOTE_IN_OPEN);
          quoteDepth++;
        }

      }

    }

    localBuff = buff;
    input = QUOTE_CHAR_PATTERN.matcher(buff).replaceAll("&quot;");

    input = QUOTE_IN_OPEN_PATTERN.matcher(input).replaceAll(QUOTE_IN_OPEN_HTML);
    input = QUOTE_IN_CLOSE_PATTERN.matcher(input).replaceAll(QUOTE_IN_CLOSE_HTML);
    input = QUOTE_OUT_OPEN_PATTERN.matcher(input).replaceAll(QUOTE_OUT_OPEN_HTML);
    input = QUOTE_OUT_CLOSE_PATTERN.matcher(input).replaceAll(QUOTE_OUT_CLOSE_HTML);

    return input;
  }
}
