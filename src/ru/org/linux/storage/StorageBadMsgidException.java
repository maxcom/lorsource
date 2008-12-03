package ru.org.linux.storage;

public class StorageBadMsgidException extends StorageException {
  public StorageBadMsgidException(String msgid) {
    super("Некорректный идентификатор объекта " + msgid);
  }
}