/*
 * Copyright 1998-2026 Linux.org.ru
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
package ru.org.linux.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

/**
 * URL с мягким (lenient) разбором поверх java.net.URI.
 *
 * <p>Сначала строка разбирается строгим парсером java.net.URI; при неудаче недопустимые
 * символы (пробел, кавычки, угловые скобки, управляющие, не-ASCII и т.п.) кодируются
 * в percent-escaping и разбор повторяется. Одиночный '%' превращается в %25,
 * корректные %XX-последовательности не перекодируются.</p>
 *
 * <p>URL обязан иметь scheme и host; хосты, которые java.net.URI не считает
 * server-based (подчёркивание, не-ASCII, percent-encoding), извлекаются из authority
 * напрямую.</p>
 */
public final class LorURI {
  private static final char[] HexDigits = "0123456789ABCDEF".toCharArray();
  private static final String MustEncode = "\"<>\\^`{|}[]";

  private final URI uri;
  private final String host;
  private final int port;

  private LorURI(URI uri, String host, int port) {
    this.uri = uri;
    this.host = host;
    this.port = port;
  }

  /**
   * Мягкий разбор строки как URL.
   *
   * @param url строка с URL
   * @return разобранный URL
   * @throws LorURIException если строка не является абсолютным URL с хостом
   */
  @Nonnull
  public static LorURI parse(@Nullable String url) throws LorURIException {
    if (url == null || url.isEmpty()) {
      throw new LorURIException("empty url");
    }

    URI parsed = tryParse(url);

    if (parsed == null) {
      parsed = tryParse(sanitize(url));
    }

    if (parsed == null) {
      throw new LorURIException("invalid url");
    }

    return of(parsed);
  }

  /**
   * Оборачивает уже разобранный java.net.URI с проверкой scheme и host.
   */
  @Nonnull
  public static LorURI of(@Nonnull URI uri) throws LorURIException {
    if (uri.getScheme() == null) {
      throw new LorURIException("no scheme");
    }

    String authority = uri.getRawAuthority();

    if (authority == null) {
      throw new LorURIException("no host");
    }

    String host = uri.getHost();
    int port = uri.getPort();

    if (host == null) {
      // java.net.URI считает такой authority registry-based и не разбирает host/port;
      // извлекаем host[:port] самостоятельно (как это делал commons-httpclient)
      int at = authority.lastIndexOf('@');
      String hostPort = at >= 0 ? authority.substring(at + 1) : authority;

      int colon = hostPort.lastIndexOf(':');
      if (colon > 0) {
        String maybePort = hostPort.substring(colon + 1);
        if (isAsciiDigits(maybePort)) {
          port = parsePort(maybePort);
          hostPort = hostPort.substring(0, colon);
        }
      }

      host = hostPort;
    }

    if (port > 65535) {
      throw new LorURIException("invalid port");
    }

    if (host.isEmpty()) {
      throw new LorURIException("no host");
    }

    return new LorURI(uri, host, port);
  }

  /**
   * Собирает URL из компонентов; компоненты считаются нераскодированными.
   */
  @Nonnull
  public static LorURI create(
      @Nonnull String scheme,
      @Nonnull String host,
      int port,
      @Nonnull String path,
      @Nullable String query,
      @Nullable String fragment
  ) throws LorURIException {
    try {
      return of(new URI(scheme, null, host, port, path, query, fragment));
    } catch (URISyntaxException e) {
      throw new LorURIException(e.getMessage(), e);
    }
  }

  @Nonnull
  public String getScheme() {
    return uri.getScheme();
  }

  @Nonnull
  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  @Nullable
  public String getPath() {
    return uri.getPath();
  }

  @Nullable
  public String getQuery() {
    return uri.getQuery();
  }

  @Nullable
  public String getFragment() {
    return uri.getFragment();
  }

  public boolean isAbsolute() {
    return uri.isAbsolute();
  }

  /**
   * Экранированное (percent-encoded, ASCII) представление URL; аналог
   * getEscapedURIReference() у commons-httpclient.
   */
  @Nonnull
  public String escaped() {
    return uri.toASCIIString();
  }

  /**
   * Раскодированное представление URL (UTF-8); '+' не интерпретируется как пробел.
   * Некорректные UTF-8-последовательности дают U+FFFD, как у commons-httpclient.
   */
  @Nonnull
  public String unescaped() {
    return percentDecode(uri.toString());
  }

  /**
   * Возвращает копию с заменённым fragment.
   */
  @Nonnull
  public LorURI withFragment(@Nonnull String fragment) throws LorURIException {
    String base = uri.toString();
    int hash = base.indexOf('#');
    if (hash >= 0) {
      base = base.substring(0, hash);
    }
    return parse(base + "#" + fragment);
  }

  @Nonnull
  public URI toJavaURI() {
    return uri;
  }

  @Override
  public String toString() {
    return escaped();
  }

  private static URI tryParse(String url) {
    try {
      return new URI(url);
    } catch (URISyntaxException e) {
      return null;
    }
  }

  private static int parsePort(String s) throws LorURIException {
    if (s.length() > 5) {
      throw new LorURIException("invalid port");
    }

    int port = Integer.parseInt(s);

    if (port > 65535) {
      throw new LorURIException("invalid port");
    }

    return port;
  }

  private static boolean isAsciiDigits(String s) {
    if (s.isEmpty()) {
      return false;
    }
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

  private static boolean isHex(char c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  private static int hexValue(char c) {
    if (c >= '0' && c <= '9') {
      return c - '0';
    }
    if (c >= 'a' && c <= 'f') {
      return c - 'a' + 10;
    }
    return c - 'A' + 10;
  }

  /**
   * Кодирует символы, недопустимые для java.net.URI; возвращает сколько символов входа
   * потреблено (суррогатная пара занимает два char).
   */
  private static int appendEscaped(StringBuilder sb, String s, int i) {
    int codePoint = s.codePointAt(i);
    String chunk = new String(Character.toChars(codePoint));

    for (byte b : chunk.getBytes(StandardCharsets.UTF_8)) {
      sb.append('%');
      sb.append(HexDigits[(b >> 4) & 0xF]);
      sb.append(HexDigits[b & 0xF]);
    }

    return Character.charCount(codePoint);
  }

  private static String sanitize(String url) {
    StringBuilder sb = new StringBuilder(url.length() + 16);
    int i = 0;

    while (i < url.length()) {
      char c = url.charAt(i);

      if (c == '%') {
        if (i + 2 < url.length() && isHex(url.charAt(i + 1)) && isHex(url.charAt(i + 2))) {
          sb.append(url, i, i + 3);
          i += 3;
        } else {
          sb.append("%25");
          i += 1;
        }
        continue;
      }

      if (needsEncoding(c)) {
        i += appendEscaped(sb, url, i);
        continue;
      }

      sb.append(c);
      i += 1;
    }

    return sb.toString();
  }

  private static boolean needsEncoding(char c) {
    if (c <= 0x20 || c == 0x7f) {
      return true;
    }
    if (c > 0x7f) {
      return true;
    }
    return MustEncode.indexOf(c) >= 0;
  }

  private static String percentDecode(String s) {
    if (s.indexOf('%') < 0) {
      return s;
    }

    StringBuilder result = new StringBuilder(s.length());
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    int i = 0;
    while (i < s.length()) {
      char c = s.charAt(i);
      if (c == '%' && i + 2 < s.length() && isHex(s.charAt(i + 1)) && isHex(s.charAt(i + 2))) {
        bytes.write((hexValue(s.charAt(i + 1)) << 4) | hexValue(s.charAt(i + 2)));
        i += 3;
      } else {
        flushBytes(result, bytes);
        result.append(c);
        i += 1;
      }
    }

    flushBytes(result, bytes);

    return result.toString();
  }

  private static void flushBytes(StringBuilder sb, ByteArrayOutputStream bytes) {
    if (bytes.size() == 0) {
      return;
    }
    sb.append(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
    bytes.reset();
  }
}
