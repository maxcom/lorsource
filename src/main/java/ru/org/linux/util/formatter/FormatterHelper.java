package ru.org.linux.util.formatter;

import org.apache.commons.httpclient.URI;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormatterHelper {
  private static final String URL_REGEX = "(?:(?:(?:(?:https?://)|(?:ftp://)|(?:www\\.))|(?:ftp\\.))[a-z0-9.-]+(?:\\.[a-z]+)?(?::[0-9]+)?" +
    "(?:/(?:([\\w=?+/\\[\\]~%;,._@#'!\\p{L}:-]|(\\([^\\)]*\\)))*([\\p{L}:'" +
    "\\w=?+/~@%#-]|(?:&[\\w:$_.+!*'#%(),@\\p{L}=;/-]+)+|(\\([^\\)]*\\))))?)?)" +
    "|(?:mailto: ?[a-z0-9+.]+@[a-z0-9.-]+.[a-z]+)|(?:news:([\\w+]\\.?)+)";

  private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  private static final int MAX_LENGTH = 80;

  protected String formatLine(String line) {
    StringBuilder out = new StringBuilder();
    Matcher m = URL_PATTERN.matcher(line);
    int index = 0;
    while (m.find()) {
      int start = m.start();
      int end = m.end();

      // обработка начальной части до URL
      out.append(line.substring(index, start));

      // обработка URL
      String url = line.substring(start, end);
      String urlChunk = url;

      if (url.toLowerCase().startsWith("www.")) {
        url = "http://" + url;
      } else if (url.toLowerCase().startsWith("ftp.")) {
        url = "ftp://" + url;
      }

      if (urlChunk.length() > MAX_LENGTH) {
        urlChunk = urlChunk.substring(0, MAX_LENGTH - 3) + "...";
      }

      try {
        URI uri = new URI(url, true, "UTF-8");

        out.append("[url=").append(uri.toString()).append(']').append(urlChunk).append("[/url]");
      } catch (Exception e) {
        out.append(urlChunk);
      }
      index = end;
    }

    // обработка последнего фрагмента
    if (index < line.length()) {
      out.append(line.substring(index));
    }

    return out.toString();
  }


}
