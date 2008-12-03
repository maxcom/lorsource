package ru.org.linux.storage;

public class StorageInternalErrorException extends StorageException {
  public StorageInternalErrorException(String info) {
    super("Внутренняя ошибка хранилища: " + info);
  }
}