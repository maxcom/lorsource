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

package ru.org.linux.util.formatter;

public class RuTypoChanger {

  /*
  Константы замены кавычек на няшне
   */

  public static final char QUOTE_SYMBOL = '"';

  public static final char QUOTE_OUT_OPEN = '«';
  public static final char QUOTE_OUT_CLOSE = '»';
  public static final char QUOTE_IN_OPEN = '„';
  public static final char QUOTE_IN_CLOSE = '“';

  public static final String QUOTE_OUT_OPEN_HTML = "&laquo;";
  public static final String QUOTE_OUT_CLOSE_HTML = "&raquo;";
  public static final String QUOTE_IN_OPEN_HTML = "&bdquo;";
  public static final String QUOTE_IN_CLOSE_HTML = "&ldquo;";

  private static final char[] PUNCTUATION = {'.', ',', ':', ';', '-', '!', '?', '(', ')'};

  private int quoteDepth = 0;
  private String localBuff = "";

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

  private static char firstNonQuote(String buff, int start) {

    for (int pt = start - 1; pt >= 0; pt--) {
      if (!isQuoteChar(buff.charAt(pt)))
        return buff.charAt(pt);
    }
    return buff.charAt(0);
  }

  private static char lastNonQuote(String buff, int start) {

    for (int pt = start + 1; pt < buff.length(); pt++) {
      if (!isQuoteChar(buff.charAt(pt)))
        return buff.charAt(pt);
    }
    return buff.charAt(buff.length() - 1);
  }


  private static boolean isQuoteOpening(String buff, int position) {
    char before, after;

    if (position == buff.length() - 1)
      return false;
    else if (position == 0)
      before = '\0';
    else
      before = firstNonQuote(buff, position);

    after = lastNonQuote(buff, position);

    if (Character.isWhitespace(after) || (after != '.' && isPunctuation(after)))
      return false;

    if (Character.isLetterOrDigit(before))
      return false;

    // русский авось всегда спасет. авось прокатит :)
    return true;
  }

  private boolean isQuoteClosing(String buff, int position) {
    char before, after;

    if (position == 0 && localBuff.equals(""))
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

    StringBuffer buff = new StringBuffer(input.replaceAll("&quot;", "\""));

    for (int iter = 0; iter < buff.length(); iter++) {
      if (buff.charAt(iter) == QUOTE_SYMBOL) {
        if (isQuoteClosing(buff.toString(), iter) && quoteDepth > 0) {
          if (quoteDepth == 1)
            buff.setCharAt(iter, QUOTE_OUT_CLOSE);
          else
            buff.setCharAt(iter, QUOTE_IN_CLOSE);
          quoteDepth--;
        }
        else
        if (isQuoteOpening(buff.toString(), iter)) { //убеждаемся, что всё так
          if (quoteDepth == 0)
            buff.setCharAt(iter, QUOTE_OUT_OPEN);
          else
            buff.setCharAt(iter, QUOTE_IN_OPEN);
          quoteDepth++;
        }

      }

    }

    localBuff = buff.toString();
    input = buff.toString().replaceAll("(''|\")", "&quot;");

    input = input.replaceAll(Character.toString(QUOTE_IN_OPEN), QUOTE_IN_OPEN_HTML);
    input = input.replaceAll(Character.toString(QUOTE_IN_CLOSE), QUOTE_IN_CLOSE_HTML);
    input = input.replaceAll(Character.toString(QUOTE_OUT_OPEN), QUOTE_OUT_OPEN_HTML);
    input = input.replaceAll(Character.toString(QUOTE_OUT_CLOSE), QUOTE_OUT_CLOSE_HTML);

    return input;
  }

}
