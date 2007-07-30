package ru.org.linux.site;

public class MessageNotFoundException extends ScriptErrorException {
  public MessageNotFoundException(int id, String info) {
    super(info);
  }

  public MessageNotFoundException(int id) {
    super("Сообщение #" + id + " не существует");
  }
}