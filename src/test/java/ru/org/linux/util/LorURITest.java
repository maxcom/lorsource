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

  private String mainUrl = "http://127.0.0.1:8080/";
  private String url1 = "http://127.0.0.1:8080/news/debian/6753486#comment-6753612";
  private String url2 = "https://127.0.0.1:8080/forum/talks/6893165?lastmod=1319027964738";
  private String url3 = "https://127.0.0.1:8080/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917";

  private String url4 = "https://127.0.0.1:8080/news"; // not message url
  private String url5 = "https://example.com"; // not lorsource url

  @Before
  public void initTest() throws Exception {
    messageDao = mock(MessageDao.class);
    message1 = mock(Message.class);
    group1 = mock(Group.class);
    message2 = mock(Message.class);
    group2 = mock(Group.class);
    message3 = mock(Message.class);
    group3 = mock(Group.class);


    when(group1.getUrl()).thenReturn("/news/debian/");
    when(group2.getUrl()).thenReturn("/forum/talks/");
    when(group3.getUrl()).thenReturn("/forum/general/");
    when(messageDao.getGroup(message1)).thenReturn(group1);
    when(messageDao.getGroup(message2)).thenReturn(group2);
    when(messageDao.getGroup(message3)).thenReturn(group3);
    when(messageDao.getById(6753486)).thenReturn(message1);
    when(messageDao.getById(6893165)).thenReturn(message2);
    when(messageDao.getById(6890857)).thenReturn(message3);
  }

  @Test
  public void test1() throws Exception {
    LorURI lorURI = new LorURI(mainUrl, url1);
    assertEquals(6753486, lorURI.getMessageId());
    assertEquals(6753612, lorURI.getCommentId());
    assertTrue(lorURI.isTrueLorUrl());
    assertTrue(lorURI.isMessageUrl());
    assertTrue(lorURI.isCommentUrl());
    assertEquals("http://127.0.0.1:8080/news/debian/6753486?cid=6753612", lorURI.formatJump(messageDao, false));
    assertEquals("https://127.0.0.1:8080/news/debian/6753486?cid=6753612", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test2() throws Exception {
    LorURI lorURI = new LorURI(mainUrl, url2);
    assertEquals(6893165, lorURI.getMessageId());
    assertEquals(0, lorURI.getCommentId());
    assertTrue(lorURI.isTrueLorUrl());
    assertTrue(lorURI.isMessageUrl());
    assertTrue(!lorURI.isCommentUrl());
    assertEquals("http://127.0.0.1:8080/forum/talks/6893165", lorURI.formatJump(messageDao, false));
    assertEquals("https://127.0.0.1:8080/forum/talks/6893165", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test3() throws Exception {
    LorURI lorURI = new LorURI(mainUrl, url3);
    assertEquals(6890857, lorURI.getMessageId());
    assertEquals(6892917, lorURI.getCommentId());
    assertTrue(lorURI.isTrueLorUrl());
    assertTrue(lorURI.isMessageUrl());
    assertTrue(lorURI.isCommentUrl());
    assertEquals("http://127.0.0.1:8080/forum/general/6890857?cid=6892917", lorURI.formatJump(messageDao, false));
    assertEquals("https://127.0.0.1:8080/forum/general/6890857?cid=6892917", lorURI.formatJump(messageDao, true));
  }

  @Test
  public void test4() throws Exception {
    LorURI lorURI = new LorURI(mainUrl, url4);
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
    LorURI lorURI = new LorURI(mainUrl, url5);
    assertEquals(0, lorURI.getMessageId());
    assertEquals(0, lorURI.getCommentId());
    assertTrue(!lorURI.isTrueLorUrl());
    assertTrue(!lorURI.isMessageUrl());
    assertTrue(!lorURI.isCommentUrl());
    assertEquals("", lorURI.formatJump(messageDao, false));
    assertEquals("", lorURI.formatJump(messageDao, true));
  }
}
