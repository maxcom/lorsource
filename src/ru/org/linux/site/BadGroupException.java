package ru.org.linux.site;

public class BadGroupException extends ScriptErrorException {
  public BadGroupException() {
  }

  public BadGroupException(String info) {
    super(info);
  }
}