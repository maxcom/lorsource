package ru.org.linux.storage;

public class StorageNotImplException extends StorageException {
  public StorageNotImplException() {
    super("данный метод доступа к хранилищу не реализован драйвером");
  }
}