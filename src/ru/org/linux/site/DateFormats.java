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

package ru.org.linux.site;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class DateFormats {
  private static final Locale RUSSIAN_LOCALE = new Locale("ru");private DateFormats() {
  }

  public static DateFormat createDefault() {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, RUSSIAN_LOCALE);
  }

  public static DateFormat createShort() {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, RUSSIAN_LOCALE);
  }

  public static DateFormat createRFC822() {
    return new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
  }
}
