/*
 * Copyright 1998-2010 Linux.org.ru
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class URLUtilTest {

  private String MainUrl = "http://127.0.0.1:8080/";

  @Test
  public void testGetRequest() {
    assertEquals("news/debian/6753486#comment-6753612",
        URLUtil.getRequestFromUrl(MainUrl, "http://127.0.0.1:8080/news/debian/6753486#comment-6753612"));
    assertEquals("forum/general/6890857/page2?lastmod=1319022386177#comment-6892917",
        URLUtil.getRequestFromUrl(MainUrl, "http://127.0.0.1:8080/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917"));
    assertEquals("news/debian/6753486#comment-6753612",
        URLUtil.getRequestFromUrl(MainUrl, "https://127.0.0.1:8080/news/debian/6753486#comment-6753612"));
    assertEquals("forum/general/6890857/page2?lastmod=1319022386177#comment-6892917",
        URLUtil.getRequestFromUrl(MainUrl, "https://127.0.0.1:8080/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917"));
    assertEquals("forum/talks/6893165?lastmod=1319027964738",
        URLUtil.getRequestFromUrl(MainUrl, "http://127.0.0.1:8080/forum/talks/6893165?lastmod=1319027964738"));
    assertEquals("forum/talks/6893165?lastmod=1319027964738",
        URLUtil.getRequestFromUrl(MainUrl, "https://127.0.0.1:8080/forum/talks/6893165?lastmod=1319027964738"));
  }

  @Test
  public void testGetMessageId() {
    assertEquals(6753486,
        URLUtil.getMessageIdFromRequest(URLUtil.getRequestFromUrl(MainUrl, "http://127.0.0.1:8080/news/debian/6753486#comment-6753612")));
    assertEquals(6753486,
        URLUtil.getMessageIdFromRequest(URLUtil.getRequestFromUrl(MainUrl, "https://127.0.0.1:8080/news/debian/6753486#comment-6753612")));
    assertEquals(6890857,
        URLUtil.getMessageIdFromRequest(URLUtil.getRequestFromUrl(MainUrl, "http://127.0.0.1:8080/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917")));
    assertEquals(6890857,
        URLUtil.getMessageIdFromRequest(URLUtil.getRequestFromUrl(MainUrl, "https://127.0.0.1:8080/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917")));
    assertEquals(6893165,
        URLUtil.getMessageIdFromRequest(URLUtil.getRequestFromUrl(MainUrl, "https://127.0.0.1:8080/forum/talks/6893165?lastmod=1319027964738")));
  }

  @Test
  public void testGetCommentId() {
    assertEquals(6753612,
        URLUtil.getCommentIdFromRequest(URLUtil.getRequestFromUrl(MainUrl, "http://127.0.0.1:8080/news/debian/6753486#comment-6753612")));
    assertEquals(6753612,
        URLUtil.getCommentIdFromRequest(URLUtil.getRequestFromUrl(MainUrl, "https://127.0.0.1:8080/news/debian/6753486#comment-6753612")));
    assertEquals(6892917,
        URLUtil.getCommentIdFromRequest(URLUtil.getRequestFromUrl(MainUrl, "http://127.0.0.1:8080/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917")));
    assertEquals(6892917,
        URLUtil.getCommentIdFromRequest(URLUtil.getRequestFromUrl(MainUrl, "https://127.0.0.1:8080/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917")));
    assertEquals(0,
        URLUtil.getCommentIdFromRequest(URLUtil.getRequestFromUrl(MainUrl, "https://127.0.0.1:8080/forum/talks/6893165?lastmod=1319027964738")));
  }


  @Test
  public void testSimpleURL() {
    assertTrue(URLUtil.isUrl("http://www.linux.org.ru/"));
  }

  @Test
  public void testURL() {
    assertTrue(URLUtil.isUrl("http://www.linux.org.ru:80/dfrer.jsp?msgid=3412345"));
  }

  @Test
  public void testBadURL() {
    assertFalse(URLUtil.isUrl("xttp://www.linux.org.ru/"));
  }

  @Test
  public void testIPUrl() {
    assertTrue(URLUtil.isUrl("http://127.0.0.1/"));
  }

  @Test
  public void testCyrillicURL() {
    assertTrue(URLUtil.isUrl("http://президент.рф/"));
  }
}
