package ru.org.linux.util.cli;

import ru.org.linux.util.ImageInfo;

final class image {
  public static void main(String[] args) {
    try {
      for (int i = 0; i < args.length; i++) {
        System.out.print(args[i] + ' ');
        System.out.println(new ImageInfo(args[i]).getCode());
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}