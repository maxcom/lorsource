package ru.org.linux.site;

public class UserNotFoundException extends ScriptErrorException {
  public UserNotFoundException(String name) {
    super("Пользователь \"" + name + "\" не существует");
  }

  public UserNotFoundException(int id) {
    super("Пользователь id=" + id + " не существует");
  }

}