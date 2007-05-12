package ru.org.linux.storage;

public class StorageNotFoundException extends StorageException {
  public StorageNotFoundException(String domain, int msgid) {
    super("Не найден объект " + domain + ':' + msgid);
  }

  public StorageNotFoundException(String domain, String msgid) {
    super("Не найден объект " + domain + ':' + msgid);
  }

  public StorageNotFoundException(String domain, String msgid, Exception e) {
    super("Не найден объект " + domain + ':' + msgid, e);
  }
}