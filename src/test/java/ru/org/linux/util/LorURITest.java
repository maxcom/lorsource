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

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.junit.Before;
import org.junit.Test;
import ru.org.linux.site.Group;
import ru.org.linux.site.Message;
import ru.org.linux.spring.dao.MessageDao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LorURITest {
  private MessageDao messageDao;
  private Message message1;
  private Group group1;
  private Message message2;
  private Group group2;
  private Message message3;
  private Group group3;
  private Message message12;
  private Group group12;
  private Message message15;
  private Group group15;


  URI mainURI; // 127.0.0.1:8080
  URI mainLORURI; // linux.org.ru
  private String url1 = "http://127.0.0.1:8080/news/debian/6753486#comment-6753612";
  private String url1n = "http://127.0.0.1:8080/news/debian/6753486?cid=6753612";
  private String url2 = "https://127.0.0.1:8080/forum/talks/6893165?lastmod=1319027964738";
  private String url3 = "https://127.0.0.1:8080/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917";

  private String url4 = "https://127.0.0.1:8080/news"; // not message url
  private String url5 = "https://example.com"; // not lorsource url
  private String url6 = "http://127.0.0.1:8080/search.jsp?q=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on"; // search url
  private String url7 = "http://127.0.0.1:8080/search.jsp?q=привет&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on"; // search url unescaped
  private String failurl8 = "some crap";
  private String failurl9 = "";
  private String failurl10 = null;
  private String failurl11 = "127.0.0.1:8080/news/debian/6753486#comment-6753612";
  private String url12 = "http://127.0.0.1:8080/forum/security/1948661?lastmod=1319623223360#comment-1948668";

  @Before
  public void initTest() throws Exception {
    mainURI = new URI("http://127.0.0.1:8080/", true, "UTF-8");
    mainLORURI = new URI("http://www.linux.org.ru/", true, "UTF-8");

    messageDao = mock(MessageDao.class);
    message1 = mock(Message.class);
    group1 = mock(Group.class);
    message2 = mock(Message.class);
    group2 = mock(Group.class);
    message3 = mock(Message.class);
    group3 = mock(Group.class);
    message12 = mock(Message.class);
    group12 = mock(Group.class);
    message15 = mock(Message.class);
    group15 = mock(Group.class);

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
    LorURI lorURI = new LorURI(mainURI, url1);

    assertTrue(lorURI.isTrueLorUrl());
    assertTrue(lorURI.isMessageUrl());
    assertTrue(lorURI.isCommentUrl());

    assertEquals(6753486, lorURI.getMessageId());
    assertEquals(6753612, lorURI.getCommentId());
    assertEquals("http://127.0.0.1:8080/news/debian/6753486#comment-6753612", lorURI.fixScheme(false));
    assertEquals("https://127.0.0.1:8080/news/debian/6753486#comment-6753612", lorURI.fixScheme(true));
    assertEquals("http://127.0.0.1:8080/news/debian/6753486?cid=6753612", lorURI.formatJump(messageDao, false));
    assertEquals("https://127.0.0.1:8080/news/debian/6753486?cid=6753612", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test1n() throws Exception {
    LorURI lorURI = new LorURI(mainURI, url1n);

    assertTrue(lorURI.isTrueLorUrl());
    assertTrue(lorURI.isMessageUrl());
    assertTrue(lorURI.isCommentUrl());

    assertEquals(6753486, lorURI.getMessageId());
    assertEquals(6753612, lorURI.getCommentId());
    assertEquals("http://127.0.0.1:8080/news/debian/6753486?cid=6753612", lorURI.fixScheme(false));
    assertEquals("https://127.0.0.1:8080/news/debian/6753486?cid=6753612", lorURI.fixScheme(true));
    assertEquals("http://127.0.0.1:8080/news/debian/6753486?cid=6753612", lorURI.formatJump(messageDao, false));
    assertEquals("https://127.0.0.1:8080/news/debian/6753486?cid=6753612", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test2() throws Exception {
    LorURI lorURI = new LorURI(mainURI, url2);
    assertEquals(6893165, lorURI.getMessageId());
    assertEquals(0, lorURI.getCommentId());
    assertTrue(lorURI.isTrueLorUrl());
    assertTrue(lorURI.isMessageUrl());
    assertTrue(!lorURI.isCommentUrl());
    assertEquals("http://127.0.0.1:8080/forum/talks/6893165?lastmod=1319027964738", lorURI.fixScheme(false));
    assertEquals("https://127.0.0.1:8080/forum/talks/6893165?lastmod=1319027964738", lorURI.fixScheme(true));
    assertEquals("http://127.0.0.1:8080/forum/talks/6893165", lorURI.formatJump(messageDao, false));
    assertEquals("https://127.0.0.1:8080/forum/talks/6893165", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test3() throws Exception {
    LorURI lorURI = new LorURI(mainURI, url3);
    assertEquals(6890857, lorURI.getMessageId());
    assertEquals(6892917, lorURI.getCommentId());
    assertTrue(lorURI.isTrueLorUrl());
    assertTrue(lorURI.isMessageUrl());
    assertTrue(lorURI.isCommentUrl());
    assertEquals("http://127.0.0.1:8080/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917", lorURI.fixScheme(false));
    assertEquals("https://127.0.0.1:8080/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917", lorURI.fixScheme(true));
    assertEquals("http://127.0.0.1:8080/forum/general/6890857?cid=6892917", lorURI.formatJump(messageDao, false));
    assertEquals("https://127.0.0.1:8080/forum/general/6890857?cid=6892917", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test4() throws Exception {
    LorURI lorURI = new LorURI(mainURI, url4);
    assertEquals(0, lorURI.getMessageId());
    assertEquals(0, lorURI.getCommentId());
    assertTrue(lorURI.isTrueLorUrl());
    assertTrue(!lorURI.isMessageUrl());
    assertTrue(!lorURI.isCommentUrl());
    assertEquals("", lorURI.formatJump(messageDao, false));
    assertEquals("", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test5() throws Exception {
    LorURI lorURI = new LorURI(mainURI, url5);
    assertEquals(0, lorURI.getMessageId());
    assertEquals(0, lorURI.getCommentId());
    assertTrue(!lorURI.isTrueLorUrl());
    assertTrue(!lorURI.isMessageUrl());
    assertTrue(!lorURI.isCommentUrl());
    assertEquals("", lorURI.formatJump(messageDao, false));
    assertEquals("", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test6() throws Exception {
    LorURI lorURI = new LorURI(mainURI, url6);
    assertEquals(0, lorURI.getMessageId());
    assertEquals(0, lorURI.getCommentId());
    assertTrue(lorURI.isTrueLorUrl());
    assertTrue(!lorURI.isMessageUrl());
    assertTrue(!lorURI.isCommentUrl());
    assertEquals("", lorURI.formatJump(messageDao, false));
    assertEquals("", lorURI.formatJump(messageDao, true));
    assertEquals("http://127.0.0.1:8080/search.jsp?q=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on", lorURI.fixScheme(false));
    assertEquals("https://127.0.0.1:8080/search.jsp?q=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on", lorURI.fixScheme(true));
  }

  @Test
  public void test7() throws Exception {
    LorURI lorURI = new LorURI(mainURI, url7);
    assertEquals(0, lorURI.getMessageId());
    assertEquals(0, lorURI.getCommentId());
    assertTrue(lorURI.isTrueLorUrl());
    assertTrue(!lorURI.isMessageUrl());
    assertTrue(!lorURI.isCommentUrl());
    assertEquals("", lorURI.formatJump(messageDao, false));
    assertEquals("", lorURI.formatJump(messageDao, true));
    assertEquals("http://127.0.0.1:8080/search.jsp?q=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on", lorURI.fixScheme(false));
    assertEquals("https://127.0.0.1:8080/search.jsp?q=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on", lorURI.fixScheme(true));
  }

  @Test
  public void test8() throws Exception {
    boolean result = false;
    try {
      LorURI lorURI = new LorURI(mainURI, failurl8);
    } catch (URIException e) {
      result = true;
    }
    assertTrue(result);
  }

  @Test
  public void test9() throws Exception {
    boolean result = false;
    try {
      LorURI lorURI = new LorURI(mainURI, failurl9);
    } catch (URIException e) {
      result = true;
    }
    assertTrue(result);
  }

  @Test
  public void test10() throws Exception {
    boolean result = false;
    try {
      LorURI lorURI = new LorURI(mainURI, failurl10);
    } catch (Exception e) {
      result=true;
    }
    assertTrue(result);
  }

  @Test
  public void test11() throws Exception {
    boolean result = false;
    try {
      LorURI lorURI = new LorURI(mainURI, failurl11);
    } catch (Exception e) {
      result=true;
    }
    assertTrue(result);
  }

  @Test
  public void test12() throws Exception {
    LorURI lorURI = new LorURI(mainURI, url12);
    assertEquals(1948661, lorURI.getMessageId());
    assertEquals(1948668, lorURI.getCommentId());
    assertTrue(lorURI.isTrueLorUrl());
    assertTrue(lorURI.isMessageUrl());
    assertTrue(lorURI.isCommentUrl());
    assertEquals("http://127.0.0.1:8080/forum/security/1948661?cid=1948668", lorURI.formatJump(messageDao, false));
    assertEquals("https://127.0.0.1:8080/forum/security/1948661?cid=1948668", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test13() throws Exception {
    String url13_1 = "http://www.linux.org.ru/view-news.jsp?tag=c%2B%2B";
    String url13_2 = "http://www.linux.org.ru/view-news.jsp?tag=c++";
    String url13_3 = "http://www.linux.org.ru/view-news.jsp?tag=c+c";
    LorURI lorURI1 = new LorURI(mainLORURI, url13_1);
    LorURI lorURI2 = new LorURI(mainLORURI, url13_2);
    LorURI lorURI3 = new LorURI(mainLORURI, url13_3);
    assertEquals("https://www.linux.org.ru/view-news.jsp?tag=c++", lorURI1.fixScheme(true));
    assertEquals("http://www.linux.org.ru/view-news.jsp?tag=c++", lorURI1.fixScheme(false));
    assertEquals("https://www.linux.org.ru/view-news.jsp?tag=c%20%20", lorURI2.fixScheme(true));
    assertEquals("http://www.linux.org.ru/view-news.jsp?tag=c%20%20", lorURI2.fixScheme(false));
    assertEquals("http://www.linux.org.ru/view-news.jsp?tag=c%20c", lorURI3.fixScheme(false));
    assertEquals("https://www.linux.org.ru/view-news.jsp?tag=c%20c", lorURI3.fixScheme(true));
  }

  @Test
  public void test14() throws Exception {
    String url14_1 = "https://www.linux.org.ru/jump-message.jsp?msgid=6890857&amp;cid=6892917";
    String url14_2 = "https://127.0.0.1:8080/jump-message.jsp?msgid=6890857&amp;cid=6892917";
    LorURI lorURI1 = new LorURI(mainLORURI, url14_1);
    LorURI lorURI2 = new LorURI(mainURI, url14_2);

    assertEquals(6890857, lorURI1.getMessageId());
    assertEquals(6892917, lorURI1.getCommentId());
    assertTrue(lorURI1.isTrueLorUrl());
    assertTrue(lorURI1.isMessageUrl());
    assertTrue(lorURI1.isCommentUrl());
    assertEquals("http://www.linux.org.ru/jump-message.jsp?msgid=6890857&amp;cid=6892917", lorURI1.fixScheme(false));
    assertEquals("https://www.linux.org.ru/jump-message.jsp?msgid=6890857&amp;cid=6892917", lorURI1.fixScheme(true));
    assertEquals("http://www.linux.org.ru/forum/general/6890857?cid=6892917", lorURI1.formatJump(messageDao, false));
    assertEquals("https://www.linux.org.ru/forum/general/6890857?cid=6892917", lorURI1.formatJump(messageDao, true));

    assertEquals(6890857, lorURI2.getMessageId());
    assertEquals(6892917, lorURI2.getCommentId());
    assertTrue(lorURI2.isTrueLorUrl());
    assertTrue(lorURI2.isMessageUrl());
    assertTrue(lorURI2.isCommentUrl());
    assertEquals("http://127.0.0.1:8080/jump-message.jsp?msgid=6890857&amp;cid=6892917", lorURI2.fixScheme(false));
    assertEquals("https://127.0.0.1:8080/jump-message.jsp?msgid=6890857&amp;cid=6892917", lorURI2.fixScheme(true));
    assertEquals("http://127.0.0.1:8080/forum/general/6890857?cid=6892917", lorURI2.formatJump(messageDao, false));
    assertEquals("https://127.0.0.1:8080/forum/general/6890857?cid=6892917", lorURI2.formatJump(messageDao, true));
  }

  @Test
  public void test15() throws Exception {
    String url15_1 = "https://www.linux.org.ru/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831";
    String url15_2 = "https://127.0.0.1:8080/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831";
    LorURI lorURI1 = new LorURI(mainLORURI, url15_1);
    LorURI lorURI2 = new LorURI(mainURI, url15_2);

    assertEquals(6944260, lorURI1.getMessageId());
    assertEquals(6944831, lorURI1.getCommentId());
    assertTrue(lorURI1.isTrueLorUrl());
    assertTrue(lorURI1.isMessageUrl());
    assertTrue(lorURI1.isCommentUrl());
    assertEquals("http://www.linux.org.ru/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831", lorURI1.fixScheme(false));
    assertEquals("https://www.linux.org.ru/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831", lorURI1.fixScheme(true));
    assertEquals("http://www.linux.org.ru/forum/linux-org-ru/6944260?cid=6944831", lorURI1.formatJump(messageDao, false));
    assertEquals("https://www.linux.org.ru/forum/linux-org-ru/6944260?cid=6944831", lorURI1.formatJump(messageDao, true));

    assertEquals(6944260, lorURI2.getMessageId());
    assertEquals(6944831, lorURI2.getCommentId());
    assertTrue(lorURI2.isTrueLorUrl());
    assertTrue(lorURI2.isMessageUrl());
    assertTrue(lorURI2.isCommentUrl());
    assertEquals("http://127.0.0.1:8080/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831", lorURI2.fixScheme(false));
    assertEquals("https://127.0.0.1:8080/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831", lorURI2.fixScheme(true));
    assertEquals("http://127.0.0.1:8080/forum/linux-org-ru/6944260?cid=6944831", lorURI2.formatJump(messageDao, false));
    assertEquals("https://127.0.0.1:8080/forum/linux-org-ru/6944260?cid=6944831", lorURI2.formatJump(messageDao, true));
  }


  @Test
  public void testForumatUrlBody() throws Exception {
    // url == mainURL и mainURL host:port
    LorURI uri1 = new LorURI(mainURI, "http://127.0.0.1:8080/forum/security/1948661?cid=1948668");
    assertEquals("127.0.0.1:8080/...", uri1.formatUrlBody(10));
    assertEquals("127.0.0.1:8080/forum...", uri1.formatUrlBody(20));
    assertEquals("127.0.0.1:8080/forum/security/1948661?cid=1948668", uri1.formatUrlBody(80));
    // url == mainURL и mainURL host
    LorURI uri2 = new LorURI(mainLORURI, "https://www.linux.org.ru/search.jsp?q=%D0%B1%D0%BB%D1%8F&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on");
    assertEquals("www.linux.org.ru/...", uri2.formatUrlBody(10));
    assertEquals("www.linux.org.ru/sea...", uri2.formatUrlBody(20));
    assertEquals("www.linux.org.ru/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertopic=...", uri2.formatUrlBody(80));
    // unescaped url == mainURL и mainURL host
    LorURI uri3 = new LorURI(mainLORURI, "https://www.linux.org.ru/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on");
    assertEquals("www.linux.org.ru/...", uri3.formatUrlBody(10));
    assertEquals("www.linux.org.ru/sea...", uri3.formatUrlBody(20));
    assertEquals("www.linux.org.ru/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertopic=...", uri3.formatUrlBody(80));

    // unescaped url != mainURL и mainURL host
    LorURI uri4 = new LorURI(mainLORURI, "https://example.com/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on");
    assertEquals("example.com/...", uri4.formatUrlBody(10));
    assertEquals("example.com/search.j...", uri4.formatUrlBody(20));
    assertEquals("example.com/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on", uri4.formatUrlBody(80));

    // escaped url != mainURL и mainURL host
    LorURI uri5 = new LorURI(mainLORURI, "https://example.com/search.jsp?q=%D0%B1%D0%BB%D1%8F&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on");
    assertEquals("example.com/...", uri5.formatUrlBody(10));
    assertEquals("example.com/search.j...", uri5.formatUrlBody(20));
    assertEquals("example.com/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on", uri5.formatUrlBody(80));
  }

}
