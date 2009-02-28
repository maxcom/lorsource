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

  public static DateFormat createRFC822() {
    return new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
  }
}
