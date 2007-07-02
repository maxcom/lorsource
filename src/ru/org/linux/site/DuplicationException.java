package ru.org.linux.site;

public class DuplicationException extends UserErrorException {
  public DuplicationException() {
    super("Следующее сообщение может быть записано не менее чем через 30 секунд после предыдущего");
  }
}
