package ru.org.linux.util;

import java.io.*;

public final class FileUtils {
  private FileUtils() {
  }

  public static String readfile(String filename) throws IOException {
    StringBuffer out = new StringBuffer();
    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "KOI8-R"));

    char[] buf = new char[8192];

    int i = 0;
    while ((i = in.read(buf, 0, buf.length)) > -1) {
      if (i > 0) {
        out.append(buf, 0, i);
      }
    }

    in.close();
    return out.toString();
  }
}
