package ru.org.linux.util;

public class ServletParameterBadNameException extends ServletParameterException {
  ServletParameterBadNameException(String name) {
    super("bad parameter name: " + name);
  }
}
