package ru.org.linux.site;

import javax.servlet.ServletException;

public class ScriptErrorException extends ServletException {
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