/*
 * Copyright 1998-2022 Linux.org.ru
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

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.org.linux.group.Group;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LorURITest {
  private TopicDao messageDao;


  URI mainURI; // 127.0.0.1:8080
  URI mainLORURI; // linux.org.ru

  private String failurl10 = null;

  private URI canon;

  @Before
  public void initTest() throws Exception {
    mainURI = new URI("http://127.0.0.1:8080/", true, "UTF-8");
    mainLORURI = new URI("http://www.linux.org.ru/", true, "UTF-8");
    canon = new URI("https://127.0.0.1:8085/", true);

    messageDao = mock(TopicDao.class);
    Topic message1 = mock(Topic.class);
    Group group1 = mock(Group.class);
    Topic message2 = mock(Topic.class);
    Group group2 = mock(Group.class);
    Topic message3 = mock(Topic.class);
    Group group3 = mock(Group.class);
    Topic message12 = mock(Topic.class);
    Group group12 = mock(Group.class);
    Topic message15 = mock(Topic.class);
    Group group15 = mock(Group.class);

    when(group1.getUrl()).thenReturn("/news/debian/");
    when(group2.getUrl()).thenReturn("/forum/talks/");
    when(group3.getUrl()).thenReturn("/forum/general/");
    when(group12.getUrl()).thenReturn("/forum/security/");
    when(group15.getUrl()).thenReturn("/forum/linux-org-ru/");
    when(messageDao.getGroup(message1)).thenReturn(group1);
    when(messageDao.getGroup(message2)).thenReturn(group2);
    when(messageDao.getGroup(message3)).thenReturn(group3);
    when(messageDao.getGroup(message12)).thenReturn(group12);
    when(messageDao.getGroup(message15)).thenReturn(group15);
    when(messageDao.getById(6753486)).thenReturn(message1);
    when(messageDao.getById(6893165)).thenReturn(message2);
    when(messageDao.getById(6890857)).thenReturn(message3);
    when(messageDao.getById(1948661)).thenReturn(message12);
    when(messageDao.getById(6944260)).thenReturn(message15);
  }

  @Test
  public void test1() throws Exception {
    String url1 = "http://127.0.0.1:8080/news/debian/6753486#comment-6753612";
    LorURL lorURI = new LorURL(mainURI, url1);

    assertTrue(lorURI.isTrueLorUrl());
    assertTrue(lorURI.isMessageUrl());
    assertTrue(lorURI.isCommentUrl());

    assertEquals(6753486, lorURI.getMessageId());
    assertEquals(6753612, lorURI.getCommentId());
    assertEquals("https://127.0.0.1:8085/news/debian/6753486#comment-6753612", lorURI.canonize(canon));
    assertEquals("https://127.0.0.1:8085/news/debian/6753486?cid=6753612", lorURI.formatJump(messageDao, canon));
  }

  @Test
  public void test1n() throws Exception {
    String url1n = "http://127.0.0.1:8080/news/debian/6753486?cid=6753612";
    LorURL lorURI = new LorURL(mainURI, url1n);

    assertTrue(lorURI.isTrueLorUrl());
    assertTrue(lorURI.isMessageUrl());
    assertTrue(lorURI.isCommentUrl());

    assertEquals(6753486, lorURI.getMessageId());
    assertEquals(6753612, lorURI.getCommentId());
    assertEquals("https://127.0.0.1:8085/news/debian/6753486?cid=6753612", lorURI.canonize(canon));
    assertEquals("https://127.0.0.1:8085/news/debian/6753486?cid=6753612", lorURI.formatJump(messageDao, canon));
  }

  @Test
  public void test2() throws Exception {
    String url2 = "https://127.0.0.1:8080/forum/talks/6893165?lastmod=1319027964738";
    LorURL lorURI = new LorURL(mainURI, url2);
    assertEquals(6893165, lorURI.getMessageId());
    assertEquals(-1, lorURI.getCommentId());
    assertTrue(lorURI.isTrueLorUrl());
    assertTrue(lorURI.isMessageUrl());
    assertFalse(lorURI.isCommentUrl());
    assertEquals("https://127.0.0.1:8085/forum/talks/6893165?lastmod=1319027964738", lorURI.canonize(canon));
    assertEquals("https://127.0.0.1:8085/forum/talks/6893165", lorURI.formatJump(messageDao, canon));
  }

  @Test
  public void test3() throws Exception {
    String url3 = "https://127.0.0.1:8080/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917";
    LorURL lorURI = new LorURL(mainURI, url3);
    assertEquals(6890857, lorURI.getMessageId());
    assertEquals(6892917, lorURI.getCommentId());
    assertTrue(lorURI.isTrueLorUrl());
    assertTrue(lorURI.isMessageUrl());
    assertTrue(lorURI.isCommentUrl());
    assertEquals("https://127.0.0.1:8085/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917", lorURI.canonize(canon));
    assertEquals("https://127.0.0.1:8085/forum/general/6890857?cid=6892917", lorURI.formatJump(messageDao, canon));
  }

  @Test
  public void test4() throws Exception {
    // not message url
    String url4 = "https://127.0.0.1:8080/news";
    LorURL lorURI = new LorURL(mainURI, url4);
    assertEquals(-1, lorURI.getMessageId());
    assertEquals(-1, lorURI.getCommentId());
    assertTrue(lorURI.isTrueLorUrl());
    assertFalse(lorURI.isMessageUrl());
    assertFalse(lorURI.isCommentUrl());
    assertEquals("", lorURI.formatJump(messageDao, canon));
  }

  @Test
  public void test5() throws Exception {
    // not lorsource url
    String url5 = "https://example.com";
    LorURL lorURI = new LorURL(mainURI, url5);
    assertEquals(-1, lorURI.getMessageId());
    assertEquals(-1, lorURI.getCommentId());
    assertFalse(lorURI.isTrueLorUrl());
    assertFalse(lorURI.isMessageUrl());
    assertFalse(lorURI.isCommentUrl());
    assertEquals("", lorURI.formatJump(messageDao, canon));
  }

  @Test
  public void test6() throws Exception {
    // search url
    String url6 = "http://127.0.0.1:8080/search.jsp?q=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on";
    LorURL lorURI = new LorURL(mainURI, url6);
    assertEquals(-1, lorURI.getMessageId());
    assertEquals(-1, lorURI.getCommentId());
    assertTrue(lorURI.isTrueLorUrl());
    assertFalse(lorURI.isMessageUrl());
    assertFalse(lorURI.isCommentUrl());
    assertEquals("", lorURI.formatJump(messageDao, canon));
    assertEquals("https://127.0.0.1:8085/search.jsp?q=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on", lorURI.canonize(canon));
  }

  @Test
  public void test7() throws Exception {
    // search url unescaped
    String url7 = "http://127.0.0.1:8080/search.jsp?q=привет&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on";
    LorURL lorURI = new LorURL(mainURI, url7);
    assertEquals(-1, lorURI.getMessageId());
    assertEquals(-1, lorURI.getCommentId());
    assertTrue(lorURI.isTrueLorUrl());
    assertFalse(lorURI.isMessageUrl());
    assertFalse(lorURI.isCommentUrl());
    assertEquals("", lorURI.formatJump(messageDao, canon));
    assertEquals("https://127.0.0.1:8085/search.jsp?q=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on", lorURI.canonize(canon));
  }

  @Test
  public void test8() {
    boolean result = false;
    try {
      String failurl8 = "some crap";
      LorURL lorURI = new LorURL(mainURI, failurl8);
    } catch (URIException e) {
      result = true;
    }
    assertTrue(result);
  }

  @Test
  public void test9() {
    boolean result = false;
    try {
      String failurl9 = "";
      LorURL lorURI = new LorURL(mainURI, failurl9);
    } catch (URIException e) {
      result = true;
    }
    assertTrue(result);
  }

  @Test
  public void test10() {
    boolean result = false;
    try {
      LorURL lorURI = new LorURL(mainURI, failurl10);
    } catch (Exception e) {
      result=true;
    }
    assertTrue(result);
  }

  @Test
  public void test11() {
    boolean result = false;
    try {
      String failurl11 = "127.0.0.1:8080/news/debian/6753486#comment-6753612";
      LorURL lorURI = new LorURL(mainURI, failurl11);
    } catch (Exception e) {
      result=true;
    }
    assertTrue(result);
  }

  @Test
  public void test12() throws Exception {
    String url12 = "http://127.0.0.1:8080/forum/security/1948661?lastmod=1319623223360#comment-1948668";
    LorURL lorURI = new LorURL(mainURI, url12);
    assertEquals(1948661, lorURI.getMessageId());
    assertEquals(1948668, lorURI.getCommentId());
    assertTrue(lorURI.isTrueLorUrl());
    assertTrue(lorURI.isMessageUrl());
    assertTrue(lorURI.isCommentUrl());
    assertEquals("https://127.0.0.1:8085/forum/security/1948661?cid=1948668", lorURI.formatJump(messageDao, canon));
  }

  @Test
  public void test13() throws Exception {
    String url13_1 = "http://www.linux.org.ru/view-news.jsp?tag=c%2B%2B";
    String url13_2 = "http://www.linux.org.ru/view-news.jsp?tag=c++";
    String url13_3 = "http://www.linux.org.ru/view-news.jsp?tag=c+c";
    LorURL lorURI1 = new LorURL(mainLORURI, url13_1);
    LorURL lorURI2 = new LorURL(mainLORURI, url13_2);
    LorURL lorURI3 = new LorURL(mainLORURI, url13_3);
    assertEquals("https://127.0.0.1:8085/view-news.jsp?tag=c++", lorURI1.canonize(canon));
    assertEquals("https://127.0.0.1:8085/view-news.jsp?tag=c++", lorURI2.canonize(canon));
    assertEquals("https://127.0.0.1:8085/view-news.jsp?tag=c+c", lorURI3.canonize(canon));
  }

  @Test
  public void test14() throws Exception {
    String url14_1 = "https://www.linux.org.ru/jump-message.jsp?msgid=6890857&amp;cid=6892917";
    String url14_2 = "https://127.0.0.1:8080/jump-message.jsp?msgid=6890857&amp;cid=6892917";
    LorURL lorURI1 = new LorURL(mainLORURI, url14_1);
    LorURL lorURI2 = new LorURL(mainURI, url14_2);

    assertEquals(6890857, lorURI1.getMessageId());
    assertEquals(6892917, lorURI1.getCommentId());
    assertTrue(lorURI1.isTrueLorUrl());
    assertTrue(lorURI1.isMessageUrl());
    assertTrue(lorURI1.isCommentUrl());
    assertEquals("https://127.0.0.1:8085/jump-message.jsp?msgid=6890857&amp;cid=6892917", lorURI1.canonize(canon));
    assertEquals("https://127.0.0.1:8085/forum/general/6890857?cid=6892917", lorURI1.formatJump(messageDao, canon));

    assertEquals(6890857, lorURI2.getMessageId());
    assertEquals(6892917, lorURI2.getCommentId());
    assertTrue(lorURI2.isTrueLorUrl());
    assertTrue(lorURI2.isMessageUrl());
    assertTrue(lorURI2.isCommentUrl());
    assertEquals("https://127.0.0.1:8085/jump-message.jsp?msgid=6890857&amp;cid=6892917", lorURI2.canonize(canon));
    assertEquals("https://127.0.0.1:8085/forum/general/6890857?cid=6892917", lorURI2.formatJump(messageDao, canon));
  }

  @Test
  public void test15() throws Exception {
    String url15_1 = "https://www.linux.org.ru/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831";
    String url15_2 = "https://127.0.0.1:8080/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831";
    LorURL lorURI1 = new LorURL(mainLORURI, url15_1);
    LorURL lorURI2 = new LorURL(mainURI, url15_2);

    assertEquals(6944260, lorURI1.getMessageId());
    assertEquals(6944831, lorURI1.getCommentId());
    assertTrue(lorURI1.isTrueLorUrl());
    assertTrue(lorURI1.isMessageUrl());
    assertTrue(lorURI1.isCommentUrl());
    assertEquals("https://127.0.0.1:8085/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831", lorURI1.canonize(canon));
    assertEquals("https://127.0.0.1:8085/forum/linux-org-ru/6944260?cid=6944831", lorURI1.formatJump(messageDao, canon));

    assertEquals(6944260, lorURI2.getMessageId());
    assertEquals(6944831, lorURI2.getCommentId());
    assertTrue(lorURI2.isTrueLorUrl());
    assertTrue(lorURI2.isMessageUrl());
    assertTrue(lorURI2.isCommentUrl());
    assertEquals("https://127.0.0.1:8085/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831", lorURI2.canonize(canon));
    assertEquals("https://127.0.0.1:8085/forum/linux-org-ru/6944260?cid=6944831", lorURI2.formatJump(messageDao, canon));
  }


  @Test
  public void testForumatUrlBody() throws Exception {
    // url == mainURL и mainURL host:port
    LorURL uri1 = new LorURL(mainURI, "http://127.0.0.1:8080/forum/security/1948661?cid=1948668");
    assertEquals("127.0.0.1:8080/...", uri1.formatUrlBody(10));
    assertEquals("127.0.0.1:8080/fo...", uri1.formatUrlBody(20));
    assertEquals(20, uri1.formatUrlBody(20).length());
    assertEquals("127.0.0.1:8080/forum/security/1948661?cid=1948668", uri1.formatUrlBody(80));
    // url == mainURL и mainURL host
    LorURL uri2 = new LorURL(mainLORURI, "https://www.linux.org.ru/search.jsp?q=%D0%B1%D0%BB%D1%8F&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on");
    assertEquals("www.linux.org.ru/...", uri2.formatUrlBody(10));
    assertEquals("www.linux.org.ru/...", uri2.formatUrlBody(20));
    assertEquals(20, uri2.formatUrlBody(20).length());
    assertEquals("www.linux.org.ru/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertop...", uri2.formatUrlBody(80));
    assertEquals(80, uri2.formatUrlBody(80).length());
    // unescaped url == mainURL и mainURL host
    LorURL uri3 = new LorURL(mainLORURI, "https://www.linux.org.ru/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on");
    assertEquals("www.linux.org.ru/...", uri3.formatUrlBody(10));
    assertEquals("www.linux.org.ru/...", uri3.formatUrlBody(20));
    assertEquals(20, uri3.formatUrlBody(20).length());
    assertEquals("www.linux.org.ru/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertop...", uri3.formatUrlBody(80));
    assertEquals(80, uri3.formatUrlBody(80).length());

    // unescaped url != mainURL и mainURL host
    LorURL uri4 = new LorURL(mainLORURI, "https://example.com/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on");
    assertEquals("https:/...", uri4.formatUrlBody(10));
    assertEquals(10, uri4.formatUrlBody(10).length());
    assertEquals("https://example.c...", uri4.formatUrlBody(20));
    assertEquals(20, uri4.formatUrlBody(20).length());
    assertEquals("https://example.com/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_user...", uri4.formatUrlBody(80));
    assertEquals(80, uri4.formatUrlBody(80).length());

    // escaped url != mainURL и mainURL host
    LorURL uri5 = new LorURL(mainLORURI, "https://example.com/search.jsp?q=%D0%B1%D0%BB%D1%8F&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on");
    assertEquals("https:/...", uri5.formatUrlBody(10));
    assertEquals(10, uri5.formatUrlBody(10).length());
    assertEquals("https://example.c...", uri5.formatUrlBody(20));
    assertEquals(20, uri5.formatUrlBody(20).length());
    assertEquals("https://example.com/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_user...", uri5.formatUrlBody(80));
    assertEquals(80, uri5.formatUrlBody(80).length());
  }
  
  @Test
  public void testBadId() throws Exception {
    LorURL uri = new LorURL(mainLORURI, "http://www.linux.org.ru/forum/talks/12345678910");

    assertTrue(uri.isTrueLorUrl());
    assertFalse(uri.isMessageUrl());
    assertFalse(uri.isCommentUrl());
  }

  @Test
  public void testCppTag() throws Exception {
    LorURL uri = new LorURL(mainLORURI, "http://www.linux.org.ru/tags/c++");
    assertTrue(uri.isTrueLorUrl());

    assertEquals("http://www.linux.org.ru/tags/c++", uri.canonize(mainLORURI));
  }
}
