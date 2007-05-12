package ru.org.linux.site;

public class PollNotFoundException extends ScriptErrorException {
  public PollNotFoundException(int id) {
    super("Голосование #" + id + " не существует");    
  }
}
