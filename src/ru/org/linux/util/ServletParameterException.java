package ru.org.linux.util;

import javax.servlet.ServletException;

public class ServletParameterException extends ServletException {
  ServletParameterException(String info) {
    super(info);
  }
}
