package ru.org.linux.site;

public class AccessViolationException extends UserErrorException {
  public AccessViolationException(String info) {
    super(info);
  }
}