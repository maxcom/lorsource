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

package ru.org.linux.site;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Locale;

public class DateFormats {
  public static final Locale RUSSIAN_LOCALE = new Locale("ru");

  private static final DateTimeFormatter DEFAULT =
          DateTimeFormat.forStyle("MM").withLocale(RUSSIAN_LOCALE);

  private static final DateTimeFormatter ISO8601 =
          ISODateTimeFormat.dateTime();

  private static final DateTimeFormatter SHORT =
          DateTimeFormat.forPattern("dd.MM.yy HH:mm").withLocale(RUSSIAN_LOCALE);

  private static final DateTimeFormatter TIME =
          DateTimeFormat.forPattern("HH:mm").withLocale(RUSSIAN_LOCALE);

  private static final DateTimeFormatter RFC822 =
          DateTimeFormat.forPattern("EEE, d MMM yyyy HH:mm:ss Z").withLocale(Locale.US);

  private DateFormats() {
  }

  public static DateTimeFormatter getDefault() {
    return DEFAULT;
  }

  public static DateTimeFormatter iso8601() {
    return ISO8601;
  }

  public static DateTimeFormatter getShort() {
    return SHORT;
  }

  public static DateTimeFormatter time() {
    return TIME;
  }

  public static DateTimeFormatter rfc822() {
    return RFC822;
  }
}
