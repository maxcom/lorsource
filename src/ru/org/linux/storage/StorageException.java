package ru.org.linux.storage;

public class StorageException extends Exception {
  public StorageException() {
    super("неизвестная ошибка хранилища");
  }

  public StorageException(String info) {
    super(info);
  }

  public StorageException(String info, Exception e) {
    super(info, e);
  }
}