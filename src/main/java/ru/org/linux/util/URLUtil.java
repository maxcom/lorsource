/*
 * Copyright 1998-2015 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/* (C) Max Valjansky,
       Anastasiya Mishechkina 2000
*/

package ru.org.linux.util;

import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

public final class URLUtil {
  private static final Pattern isUrl = Pattern.compile(
    "(((https?)|(ftp))://(([0-9\\p{L}.-]+\\.\\p{L}+)|(\\d+\\.\\d+\\.\\d+\\.\\d+))(:[0-9]+)?(/[^ ]*)?)|(mailto:[a-z0-9_+-.]+@[0-9a-z.-]+\\.[a-z]+)|(news:[a-z0-9.-]+)|(((www)|(ftp))\\.(([0-9a-z.-]+\\.[a-z]+(:[0-9]+)?(/[^ ]*)?)|([a-z]+(/[^ ]*)?)))",
    Pattern.CASE_INSENSITIVE
  );

  private static final Pattern requestMessagePattern = Pattern.compile("\\w+/\\w+/(\\d+)");
  private static final Pattern requestCommentPattern = Pattern.compile("#comment-(\\d+)");

  private URLUtil() {
  }

  @Deprecated
  public static String checkAndFixURL(String url) throws BadURLException {
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

  public static String fixURL(String url) {
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

    return url;
  }

  // from JAMWiki (LGPL2.1)
  // VVVVVVVVVVVVVVVVVVVVVV

  /**
 	 * Encode a topic name for use in a URL.  This method will replace spaces
 	 * with underscores and URL encode the value, but it will not URL encode
 	 * colons.
 	 *
 	 * @param url The topic name to be encoded for use in a URL.
 	 * @return The encoded topic name value.
 	 */
 	public static String encodeAndEscapeTopicName(String url) {
 		if (StringUtils.isBlank(url)) {
 			return url;
 		}
 		String result = encodeTopicName(url);
 		try {
 			result = URLEncoder.encode(result, "UTF-8");
 		} catch (UnsupportedEncodingException e) {
 			// this should never happen
 			throw new IllegalStateException("Unsupporting encoding UTF-8");
 		}
 		// un-encode colons
 		result = StringUtils.replace(result, "%3A", ":");
 		// un-encode forward slashes
 		result = StringUtils.replace(result, "%2F", "/");
 		return result;
 	}

  /**
 	 * Encode a value for use a topic name.  This method will replace any
 	 * spaces with underscores.
 	 *
 	 * @param url The decoded value that is to be encoded.
 	 * @return An encoded value.
 	 */
 	public static String encodeTopicName(String url) {
 		if (StringUtils.isBlank(url)) {
 			return url;
 		}
 		return StringUtils.replace(url, " ", "_");
 	}

  // ^^^^^^^^^^^^^^^^^^^^^^^^
  // from JAMWiki (LGPL2.1)

  public static String buildWikiURL(String virtualWiki, String topic) {
    StringBuilder url = new StringBuilder();
    url.append("/wiki/");
    url.append(encodeAndEscapeTopicName(virtualWiki));
    url.append('/');
    url.append(encodeAndEscapeTopicName(topic));
    return url.toString();
  }

  public static boolean isUrl(String x) {
    return isUrl.matcher(x).matches();
  }

}
