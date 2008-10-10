package ru.org.linux.util;

public class BadURLException extends UtilException {
  public BadURLException() {
    super("Некорректный URL");
  }

  public BadURLException(String URL) {
    super("Некорректный URL: " + URL);
  }
}
