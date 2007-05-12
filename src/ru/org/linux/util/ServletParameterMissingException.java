package ru.org.linux.util;

public class ServletParameterMissingException extends ServletParameterException {
  private final String name;

  ServletParameterMissingException(String n) {
    super("missing parameter `" + n + '\'');
    name = n;
  }

  public String getName() {
    return name;
  }
}
