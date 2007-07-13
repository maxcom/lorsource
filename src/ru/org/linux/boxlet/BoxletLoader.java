package ru.org.linux.boxlet;

public class BoxletLoader {
  public Boxlet loadBox(String name) throws BoxletLoadException {
    try {
      Class BoxletFactory = Class.forName("ru.org.linux.site.boxes." + name);
      return (Boxlet) BoxletFactory.newInstance();
    } catch (Exception e) {
      throw new BoxletLoadException(e);
    }
  }
}
