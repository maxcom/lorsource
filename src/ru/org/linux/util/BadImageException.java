package ru.org.linux.util;

public class BadImageException extends Exception {
  public BadImageException() {
    super("Некорректное изображение");
  }

  public BadImageException(String info) {
    super(info);
  }
}