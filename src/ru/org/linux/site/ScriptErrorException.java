package ru.org.linux.site;

public class ScriptErrorException extends Exception {
  public ScriptErrorException() {
    super("неизвестная ошибка скрипта");
  }

  public ScriptErrorException(String info) {
    super(info);
  }

  public ScriptErrorException(String info, Throwable th) {
    super(info, th);
  }
}