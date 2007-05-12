package ru.org.linux.util;

public class UtilKeyNotFoundException extends UtilException {
  UtilKeyNotFoundException(String key) {
    super("Ключ `" + key + "' не найден");
  }
}
