package ru.org.linux.storage;

public class StorageBadDomainException extends StorageException {
  public StorageBadDomainException(String domain) {
    super("Некорректное имя домена " + domain);
  }
}