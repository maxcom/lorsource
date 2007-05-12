package ru.org.linux.util;

public class ServletParameterBadValueException extends ServletParameterException {
  ServletParameterBadValueException(String name, Exception e) {
    super("Bad format of '" + name + "' " + e.toString());
  }

  ServletParameterBadValueException(String name, String info) {
    super("Bad format of '" + name + "' " + info);
  }
}
