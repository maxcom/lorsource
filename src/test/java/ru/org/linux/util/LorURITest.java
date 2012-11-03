/*
 * Copyright 1998-2012 Linux.org.ru
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
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.org.linux.group.Group;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.user.User;

import java.net.URLEncoder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LorURITest {
  private TopicDao messageDao;
  private Topic message1;
  private Group group1;
  private Topic message2;
  private Group group2;
  private Topic message3;
  private Group group3;
  private Topic message12;
  private Group group12;
  private Topic message15;
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

  @BeforeMethod
  public void initTest() throws Exception {
    mainURI = new URI("http://127.0.0.1:8080/", true, "UTF-8");
    mainLORURI = new URI("http://www.linux.org.ru/", true, "UTF-8");

    messageDao = mock(TopicDao.class);
    message1 = mock(Topic.class);
    group1 = mock(Group.class);
    message2 = mock(Topic.class);
    group2 = mock(Group.class);
    message3 = mock(Topic.class);
    group3 = mock(Group.class);
    message12 = mock(Topic.class);
    group12 = mock(Group.class);
    message15 = mock(Topic.class);
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
    LorURL lorURI = new LorURL(mainURI, url1);

    Assert.assertTrue(lorURI.isTrueLorUrl());
    Assert.assertTrue(lorURI.isMessageUrl());
    Assert.assertTrue(lorURI.isCommentUrl());

    Assert.assertEquals(6753486, lorURI.getMessageId());
    Assert.assertEquals(6753612, lorURI.getCommentId());
    Assert.assertEquals("http://127.0.0.1:8080/news/debian/6753486#comment-6753612", lorURI.fixScheme(false));
    Assert.assertEquals("https://127.0.0.1:8080/news/debian/6753486#comment-6753612", lorURI.fixScheme(true));
    Assert.assertEquals("http://127.0.0.1:8080/news/debian/6753486?cid=6753612", lorURI.formatJump(messageDao, false));
    Assert.assertEquals("https://127.0.0.1:8080/news/debian/6753486?cid=6753612", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test1n() throws Exception {
    LorURL lorURI = new LorURL(mainURI, url1n);

    Assert.assertTrue(lorURI.isTrueLorUrl());
    Assert.assertTrue(lorURI.isMessageUrl());
    Assert.assertTrue(lorURI.isCommentUrl());

    Assert.assertEquals(6753486, lorURI.getMessageId());
    Assert.assertEquals(6753612, lorURI.getCommentId());
    Assert.assertEquals("http://127.0.0.1:8080/news/debian/6753486?cid=6753612", lorURI.fixScheme(false));
    Assert.assertEquals("https://127.0.0.1:8080/news/debian/6753486?cid=6753612", lorURI.fixScheme(true));
    Assert.assertEquals("http://127.0.0.1:8080/news/debian/6753486?cid=6753612", lorURI.formatJump(messageDao, false));
    Assert.assertEquals("https://127.0.0.1:8080/news/debian/6753486?cid=6753612", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test2() throws Exception {
    LorURL lorURI = new LorURL(mainURI, url2);
    Assert.assertEquals(6893165, lorURI.getMessageId());
    Assert.assertEquals(-1, lorURI.getCommentId());
    Assert.assertTrue(lorURI.isTrueLorUrl());
    Assert.assertTrue(lorURI.isMessageUrl());
    Assert.assertTrue(!lorURI.isCommentUrl());
    Assert.assertEquals("http://127.0.0.1:8080/forum/talks/6893165?lastmod=1319027964738", lorURI.fixScheme(false));
    Assert.assertEquals("https://127.0.0.1:8080/forum/talks/6893165?lastmod=1319027964738", lorURI.fixScheme(true));
    Assert.assertEquals("http://127.0.0.1:8080/forum/talks/6893165", lorURI.formatJump(messageDao, false));
    Assert.assertEquals("https://127.0.0.1:8080/forum/talks/6893165", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test3() throws Exception {
    LorURL lorURI = new LorURL(mainURI, url3);
    Assert.assertEquals(6890857, lorURI.getMessageId());
    Assert.assertEquals(6892917, lorURI.getCommentId());
    Assert.assertTrue(lorURI.isTrueLorUrl());
    Assert.assertTrue(lorURI.isMessageUrl());
    Assert.assertTrue(lorURI.isCommentUrl());
    Assert.assertEquals("http://127.0.0.1:8080/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917", lorURI.fixScheme(false));
    Assert.assertEquals("https://127.0.0.1:8080/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917", lorURI.fixScheme(true));
    Assert.assertEquals("http://127.0.0.1:8080/forum/general/6890857?cid=6892917", lorURI.formatJump(messageDao, false));
    Assert.assertEquals("https://127.0.0.1:8080/forum/general/6890857?cid=6892917", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test4() throws Exception {
    LorURL lorURI = new LorURL(mainURI, url4);
    Assert.assertEquals(-1, lorURI.getMessageId());
    Assert.assertEquals(-1, lorURI.getCommentId());
    Assert.assertTrue(lorURI.isTrueLorUrl());
    Assert.assertTrue(!lorURI.isMessageUrl());
    Assert.assertTrue(!lorURI.isCommentUrl());
    Assert.assertEquals("", lorURI.formatJump(messageDao, false));
    Assert.assertEquals("", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test5() throws Exception {
    LorURL lorURI = new LorURL(mainURI, url5);
    Assert.assertEquals(-1, lorURI.getMessageId());
    Assert.assertEquals(-1, lorURI.getCommentId());
    Assert.assertTrue(!lorURI.isTrueLorUrl());
    Assert.assertTrue(!lorURI.isMessageUrl());
    Assert.assertTrue(!lorURI.isCommentUrl());
    Assert.assertEquals("", lorURI.formatJump(messageDao, false));
    Assert.assertEquals("", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test6() throws Exception {
    LorURL lorURI = new LorURL(mainURI, url6);
    Assert.assertEquals(-1, lorURI.getMessageId());
    Assert.assertEquals(-1, lorURI.getCommentId());
    Assert.assertTrue(lorURI.isTrueLorUrl());
    Assert.assertTrue(!lorURI.isMessageUrl());
    Assert.assertTrue(!lorURI.isCommentUrl());
    Assert.assertEquals("", lorURI.formatJump(messageDao, false));
    Assert.assertEquals("", lorURI.formatJump(messageDao, true));
    Assert.assertEquals("http://127.0.0.1:8080/search.jsp?q=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on", lorURI.fixScheme(false));
    Assert.assertEquals("https://127.0.0.1:8080/search.jsp?q=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on", lorURI.fixScheme(true));
  }

  @Test
  public void test7() throws Exception {
    LorURL lorURI = new LorURL(mainURI, url7);
    Assert.assertEquals(-1, lorURI.getMessageId());
    Assert.assertEquals(-1, lorURI.getCommentId());
    Assert.assertTrue(lorURI.isTrueLorUrl());
    Assert.assertTrue(!lorURI.isMessageUrl());
    Assert.assertTrue(!lorURI.isCommentUrl());
    Assert.assertEquals("", lorURI.formatJump(messageDao, false));
    Assert.assertEquals("", lorURI.formatJump(messageDao, true));
    Assert.assertEquals("http://127.0.0.1:8080/search.jsp?q=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on", lorURI.fixScheme(false));
    Assert.assertEquals("https://127.0.0.1:8080/search.jsp?q=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on", lorURI.fixScheme(true));
  }

  @Test
  public void test8() throws Exception {
    boolean result = false;
    try {
      LorURL lorURI = new LorURL(mainURI, failurl8);
    } catch (URIException e) {
      result = true;
    }
    Assert.assertTrue(result);
  }

  @Test
  public void test9() throws Exception {
    boolean result = false;
    try {
      LorURL lorURI = new LorURL(mainURI, failurl9);
    } catch (URIException e) {
      result = true;
    }
    Assert.assertTrue(result);
  }

  @Test
  public void test10() throws Exception {
    boolean result = false;
    try {
      LorURL lorURI = new LorURL(mainURI, failurl10);
    } catch (Exception e) {
      result=true;
    }
    Assert.assertTrue(result);
  }

  @Test
  public void test11() throws Exception {
    boolean result = false;
    try {
      LorURL lorURI = new LorURL(mainURI, failurl11);
    } catch (Exception e) {
      result=true;
    }
    Assert.assertTrue(result);
  }

  @Test
  public void test12() throws Exception {
    LorURL lorURI = new LorURL(mainURI, url12);
    Assert.assertEquals(1948661, lorURI.getMessageId());
    Assert.assertEquals(1948668, lorURI.getCommentId());
    Assert.assertTrue(lorURI.isTrueLorUrl());
    Assert.assertTrue(lorURI.isMessageUrl());
    Assert.assertTrue(lorURI.isCommentUrl());
    Assert.assertEquals("http://127.0.0.1:8080/forum/security/1948661?cid=1948668", lorURI.formatJump(messageDao, false));
    Assert.assertEquals("https://127.0.0.1:8080/forum/security/1948661?cid=1948668", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test13() throws Exception {
    String url13_1 = "http://www.linux.org.ru/view-news.jsp?tag=c%2B%2B";
    String url13_2 = "http://www.linux.org.ru/view-news.jsp?tag=c++";
    String url13_3 = "http://www.linux.org.ru/view-news.jsp?tag=c+c";
    LorURL lorURI1 = new LorURL(mainLORURI, url13_1);
    LorURL lorURI2 = new LorURL(mainLORURI, url13_2);
    LorURL lorURI3 = new LorURL(mainLORURI, url13_3);
    Assert.assertEquals("https://www.linux.org.ru/view-news.jsp?tag=c++", lorURI1.fixScheme(true));
    Assert.assertEquals("http://www.linux.org.ru/view-news.jsp?tag=c++", lorURI1.fixScheme(false));
    Assert.assertEquals("https://www.linux.org.ru/view-news.jsp?tag=c%20%20", lorURI2.fixScheme(true));
    Assert.assertEquals("http://www.linux.org.ru/view-news.jsp?tag=c%20%20", lorURI2.fixScheme(false));
    Assert.assertEquals("http://www.linux.org.ru/view-news.jsp?tag=c%20c", lorURI3.fixScheme(false));
    Assert.assertEquals("https://www.linux.org.ru/view-news.jsp?tag=c%20c", lorURI3.fixScheme(true));
  }

  @Test
  public void test14() throws Exception {
    String url14_1 = "https://www.linux.org.ru/jump-message.jsp?msgid=6890857&amp;cid=6892917";
    String url14_2 = "https://127.0.0.1:8080/jump-message.jsp?msgid=6890857&amp;cid=6892917";
    LorURL lorURI1 = new LorURL(mainLORURI, url14_1);
    LorURL lorURI2 = new LorURL(mainURI, url14_2);

    Assert.assertEquals(6890857, lorURI1.getMessageId());
    Assert.assertEquals(6892917, lorURI1.getCommentId());
    Assert.assertTrue(lorURI1.isTrueLorUrl());
    Assert.assertTrue(lorURI1.isMessageUrl());
    Assert.assertTrue(lorURI1.isCommentUrl());
    Assert.assertEquals("http://www.linux.org.ru/jump-message.jsp?msgid=6890857&amp;cid=6892917", lorURI1.fixScheme(false));
    Assert.assertEquals("https://www.linux.org.ru/jump-message.jsp?msgid=6890857&amp;cid=6892917", lorURI1.fixScheme(true));
    Assert.assertEquals("http://www.linux.org.ru/forum/general/6890857?cid=6892917", lorURI1.formatJump(messageDao, false));
    Assert.assertEquals("https://www.linux.org.ru/forum/general/6890857?cid=6892917", lorURI1.formatJump(messageDao, true));

    Assert.assertEquals(6890857, lorURI2.getMessageId());
    Assert.assertEquals(6892917, lorURI2.getCommentId());
    Assert.assertTrue(lorURI2.isTrueLorUrl());
    Assert.assertTrue(lorURI2.isMessageUrl());
    Assert.assertTrue(lorURI2.isCommentUrl());
    Assert.assertEquals("http://127.0.0.1:8080/jump-message.jsp?msgid=6890857&amp;cid=6892917", lorURI2.fixScheme(false));
    Assert.assertEquals("https://127.0.0.1:8080/jump-message.jsp?msgid=6890857&amp;cid=6892917", lorURI2.fixScheme(true));
    Assert.assertEquals("http://127.0.0.1:8080/forum/general/6890857?cid=6892917", lorURI2.formatJump(messageDao, false));
    Assert.assertEquals("https://127.0.0.1:8080/forum/general/6890857?cid=6892917", lorURI2.formatJump(messageDao, true));
  }

  @Test
  public void test15() throws Exception {
    String url15_1 = "https://www.linux.org.ru/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831";
    String url15_2 = "https://127.0.0.1:8080/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831";
    LorURL lorURI1 = new LorURL(mainLORURI, url15_1);
    LorURL lorURI2 = new LorURL(mainURI, url15_2);

    Assert.assertEquals(6944260, lorURI1.getMessageId());
    Assert.assertEquals(6944831, lorURI1.getCommentId());
    Assert.assertTrue(lorURI1.isTrueLorUrl());
    Assert.assertTrue(lorURI1.isMessageUrl());
    Assert.assertTrue(lorURI1.isCommentUrl());
    Assert.assertEquals("http://www.linux.org.ru/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831", lorURI1.fixScheme(false));
    Assert.assertEquals("https://www.linux.org.ru/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831", lorURI1.fixScheme(true));
    Assert.assertEquals("http://www.linux.org.ru/forum/linux-org-ru/6944260?cid=6944831", lorURI1.formatJump(messageDao, false));
    Assert.assertEquals("https://www.linux.org.ru/forum/linux-org-ru/6944260?cid=6944831", lorURI1.formatJump(messageDao, true));

    Assert.assertEquals(6944260, lorURI2.getMessageId());
    Assert.assertEquals(6944831, lorURI2.getCommentId());
    Assert.assertTrue(lorURI2.isTrueLorUrl());
    Assert.assertTrue(lorURI2.isMessageUrl());
    Assert.assertTrue(lorURI2.isCommentUrl());
    Assert.assertEquals("http://127.0.0.1:8080/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831", lorURI2.fixScheme(false));
    Assert.assertEquals("https://127.0.0.1:8080/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831", lorURI2.fixScheme(true));
    Assert.assertEquals("http://127.0.0.1:8080/forum/linux-org-ru/6944260?cid=6944831", lorURI2.formatJump(messageDao, false));
    Assert.assertEquals("https://127.0.0.1:8080/forum/linux-org-ru/6944260?cid=6944831", lorURI2.formatJump(messageDao, true));
  }


  @Test
  public void testForumatUrlBody() throws Exception {
    // url == mainURL и mainURL host:port
    LorURL uri1 = new LorURL(mainURI, "http://127.0.0.1:8080/forum/security/1948661?cid=1948668");
    Assert.assertEquals("127.0.0.1:8080/...", uri1.formatUrlBody(10));
    Assert.assertEquals("127.0.0.1:8080/fo...", uri1.formatUrlBody(20));
    Assert.assertEquals(20, uri1.formatUrlBody(20).length());
    Assert.assertEquals("127.0.0.1:8080/forum/security/1948661?cid=1948668", uri1.formatUrlBody(80));
    // url == mainURL и mainURL host
    LorURL uri2 = new LorURL(mainLORURI, "https://www.linux.org.ru/search.jsp?q=%D0%B1%D0%BB%D1%8F&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on");
    Assert.assertEquals("www.linux.org.ru/...", uri2.formatUrlBody(10));
    Assert.assertEquals("www.linux.org.ru/...", uri2.formatUrlBody(20));
    Assert.assertEquals(20, uri2.formatUrlBody(20).length());
    Assert.assertEquals("www.linux.org.ru/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertop...", uri2.formatUrlBody(80));
    Assert.assertEquals(80, uri2.formatUrlBody(80).length());
    // unescaped url == mainURL и mainURL host
    LorURL uri3 = new LorURL(mainLORURI, "https://www.linux.org.ru/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on");
    Assert.assertEquals("www.linux.org.ru/...", uri3.formatUrlBody(10));
    Assert.assertEquals("www.linux.org.ru/...", uri3.formatUrlBody(20));
    Assert.assertEquals(20, uri3.formatUrlBody(20).length());
    Assert.assertEquals("www.linux.org.ru/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertop...", uri3.formatUrlBody(80));
    Assert.assertEquals(80, uri3.formatUrlBody(80).length());

    // unescaped url != mainURL и mainURL host
    LorURL uri4 = new LorURL(mainLORURI, "https://example.com/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on");
    Assert.assertEquals("https:/...", uri4.formatUrlBody(10));
    Assert.assertEquals(10, uri4.formatUrlBody(10).length());
    Assert.assertEquals("https://example.c...", uri4.formatUrlBody(20));
    Assert.assertEquals(20, uri4.formatUrlBody(20).length());
    Assert.assertEquals("https://example.com/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_user...", uri4.formatUrlBody(80));
    Assert.assertEquals(80, uri4.formatUrlBody(80).length());

    // escaped url != mainURL и mainURL host
    LorURL uri5 = new LorURL(mainLORURI, "https://example.com/search.jsp?q=%D0%B1%D0%BB%D1%8F&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on");
    Assert.assertEquals("https:/...", uri5.formatUrlBody(10));
    Assert.assertEquals(10, uri5.formatUrlBody(10).length());
    Assert.assertEquals("https://example.c...", uri5.formatUrlBody(20));
    Assert.assertEquals(20, uri5.formatUrlBody(20).length());
    Assert.assertEquals("https://example.com/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_user...", uri5.formatUrlBody(80));
    Assert.assertEquals(80, uri5.formatUrlBody(80).length());
  }
  
  @Test
  public void testUrlEncode() throws Exception {
    Assert.assertEquals(User.EMPTY_GRAVATAR_SECURE_URL,  URLEncoder.encode("https://www.linux.org.ru/img/p.gif", "UTF-8"));
    Assert.assertEquals(User.EMPTY_GRAVATAR_URL,  URLEncoder.encode("http://www.linux.org.ru/img/p.gif", "UTF-8"));
  }

}
