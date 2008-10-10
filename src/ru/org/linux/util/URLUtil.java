/* (C) Max Valjansky,
       Anastasiya Mishechkina 2000
*/

package ru.org.linux.util;

import gnu.regexp.RE;
import gnu.regexp.REException;

public final class URLUtil {
  private static final RE isUrl;

  private URLUtil() {
  }

  static {
    try {
      isUrl = new RE("(((https?)|(ftp))://[0-9a-z.-]+\\.[a-z]+(:[0-9]+)?(/[^ ]*)?)|(mailto:[a-z0-9_+-]+@[0-9a-z.-]+\\.[a-z]+)|(news:[a-z0-9.-]+)|(((www)|(ftp))\\.(([0-9a-z.-]+\\.[a-z]+(:[0-9]+)?(/[^ ]*)?)|([a-z]+(/[^ ]*)?)))", RE.REG_ICASE);
    } catch (REException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static String fixURL(String url) throws UtilException {
    url = url.trim();

    if (isUrl(url)) {
      if (url.toLowerCase().startsWith("www.")) {
        return "http://" + url;
      }
      if (url.toLowerCase().startsWith("ftp.")) {
        return "ftp://" + url;
      }
      return url;
    }

    throw new BadURLException(url);
  }

  public static boolean isUrl(String x) {
    return isUrl.isMatch(x);
  }


}

