package ru.org.linux.site;

public class BadSectionException extends ScriptErrorException {
  public BadSectionException() {
    super("Неправильно задан номер секции");
  }
}