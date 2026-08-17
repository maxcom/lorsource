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

package ru.org.linux.util

import munit.FunSuite
import org.apache.commons.httpclient.{URI, URIException}
import org.mockito.Mockito.{mock, when}
import ru.org.linux.group.{Group, GroupService}
import ru.org.linux.reaction.Reactions
import ru.org.linux.topic.{Topic, TopicDao}

import java.sql.Timestamp
import java.time.Instant
import scala.compiletime.uninitialized

class LorURITest extends FunSuite:
  private var messageDao: TopicDao = uninitialized
  private var groupService: GroupService = uninitialized
  private var mainURI: URI = uninitialized
  private var mainLORURI: URI = uninitialized
  private var canon: URI = uninitialized
  private val Now = Timestamp.from(Instant.now())

  private def mkTopic(id: Int, groupId: Int): Topic =
    Topic(id = id, postscore = 0, sticky = false, linktext = null, url = null, title = "",
      authorUserId = 0, groupId = groupId, deleted = false, expired = false, commitby = 0,
      postdate = Now, commitDate = null, groupUrl = "", lastModified = Now, sectionId = 0,
      commentCount = 0, commited = false, notop = false, userAgentId = 0, postIP = "",
      resolved = false, minor = false, draft = false, allowAnonymous = false,
      reactions = Reactions.empty, expireDate = null, openWarnings = 0)

  private def mkGroup(id: Int, sectionId: Int, urlName: String): Group =
    Group(premoderated = false, pollPostAllowed = false, linksAllowed = false, sectionId = sectionId,
      defaultLinkText = "", urlName = urlName, image = "", topicRestriction = 0,
      commentsRestriction = 0, id = id, stat3 = 0, resolvable = true, title = "", info = "",
      longInfo = "")

  override def beforeEach(context: BeforeEach): Unit =
    mainURI = new URI("http://127.0.0.1:8080/", true, "UTF-8")
    mainLORURI = new URI("http://www.linux.org.ru/", true, "UTF-8")
    canon = new URI("https://127.0.0.1:8085/", true)

    messageDao = mock(classOf[TopicDao])
    groupService = mock(classOf[GroupService])

    val message1 = mkTopic(6753486, groupId = 1)
    val group1 = mkGroup(id = 1, sectionId = 1, urlName = "debian")
    val message2 = mkTopic(6893165, groupId = 2)
    val group2 = mkGroup(id = 2, sectionId = 2, urlName = "talks")
    val message3 = mkTopic(6890857, groupId = 3)
    val group3 = mkGroup(id = 3, sectionId = 2, urlName = "general")
    val message12 = mkTopic(1948661, groupId = 12)
    val group12 = mkGroup(id = 12, sectionId = 2, urlName = "security")
    val message15 = mkTopic(6944260, groupId = 15)
    val group15 = mkGroup(id = 15, sectionId = 2, urlName = "linux-org-ru")

    when(groupService.getGroup(message1.groupId)).thenReturn(group1)
    when(groupService.getGroup(message2.groupId)).thenReturn(group2)
    when(groupService.getGroup(message3.groupId)).thenReturn(group3)
    when(groupService.getGroup(message12.groupId)).thenReturn(group12)
    when(groupService.getGroup(message15.groupId)).thenReturn(group15)
    when(messageDao.getById(6753486)).thenReturn(message1)
    when(messageDao.getById(6893165)).thenReturn(message2)
    when(messageDao.getById(6890857)).thenReturn(message3)
    when(messageDao.getById(1948661)).thenReturn(message12)
    when(messageDao.getById(6944260)).thenReturn(message15)

  test("test1"):
    val url1 = "http://127.0.0.1:8080/news/debian/6753486#comment-6753612"
    val lorURI = new LorURL(mainURI, url1)

    assert(lorURI.isTrueLorUrl)
    assert(lorURI.isMessageUrl)
    assert(lorURI.isCommentUrl)

    assertEquals(6753486, lorURI.getMessageId)
    assertEquals(6753612, lorURI.getCommentId)
    assertEquals("https://127.0.0.1:8085/news/debian/6753486#comment-6753612", lorURI.canonize(canon))
    assertEquals("https://127.0.0.1:8085/news/debian/6753486?cid=6753612",
      lorURI.formatJump(messageDao, groupService, canon))

  test("test1n"):
    val url1n = "http://127.0.0.1:8080/news/debian/6753486?cid=6753612"
    val lorURI = new LorURL(mainURI, url1n)

    assert(lorURI.isTrueLorUrl)
    assert(lorURI.isMessageUrl)
    assert(lorURI.isCommentUrl)

    assertEquals(6753486, lorURI.getMessageId)
    assertEquals(6753612, lorURI.getCommentId)
    assertEquals("https://127.0.0.1:8085/news/debian/6753486?cid=6753612", lorURI.canonize(canon))
    assertEquals("https://127.0.0.1:8085/news/debian/6753486?cid=6753612",
      lorURI.formatJump(messageDao, groupService, canon))

  test("test2"):
    val url2 = "https://127.0.0.1:8080/forum/talks/6893165?lastmod=1319027964738"
    val lorURI = new LorURL(mainURI, url2)
    assertEquals(6893165, lorURI.getMessageId)
    assertEquals(-1, lorURI.getCommentId)
    assert(lorURI.isTrueLorUrl)
    assert(lorURI.isMessageUrl)
    assert(!lorURI.isCommentUrl)
    assertEquals("https://127.0.0.1:8085/forum/talks/6893165?lastmod=1319027964738", lorURI.canonize(canon))
    assertEquals("https://127.0.0.1:8085/forum/talks/6893165", lorURI.formatJump(messageDao, groupService, canon))

  test("test3"):
    val url3 = "https://127.0.0.1:8080/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917"
    val lorURI = new LorURL(mainURI, url3)
    assertEquals(6890857, lorURI.getMessageId)
    assertEquals(6892917, lorURI.getCommentId)
    assert(lorURI.isTrueLorUrl)
    assert(lorURI.isMessageUrl)
    assert(lorURI.isCommentUrl)
    assertEquals("https://127.0.0.1:8085/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917",
      lorURI.canonize(canon))
    assertEquals("https://127.0.0.1:8085/forum/general/6890857?cid=6892917",
      lorURI.formatJump(messageDao, groupService, canon))

  test("test4"):
    // not message url
    val url4 = "https://127.0.0.1:8080/news"
    val lorURI = new LorURL(mainURI, url4)
    assertEquals(-1, lorURI.getMessageId)
    assertEquals(-1, lorURI.getCommentId)
    assert(lorURI.isTrueLorUrl)
    assert(!lorURI.isMessageUrl)
    assert(!lorURI.isCommentUrl)
    assertEquals("", lorURI.formatJump(messageDao, groupService, canon))

  test("test5"):
    // not lorsource url
    val url5 = "https://example.com"
    val lorURI = new LorURL(mainURI, url5)
    assertEquals(-1, lorURI.getMessageId)
    assertEquals(-1, lorURI.getCommentId)
    assert(!lorURI.isTrueLorUrl)
    assert(!lorURI.isMessageUrl)
    assert(!lorURI.isCommentUrl)
    assertEquals("", lorURI.formatJump(messageDao, groupService, canon))

  test("test6"):
    // search url
    val url6 = "http://127.0.0.1:8080/search.jsp?q=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on"
    val lorURI = new LorURL(mainURI, url6)
    assertEquals(-1, lorURI.getMessageId)
    assertEquals(-1, lorURI.getCommentId)
    assert(lorURI.isTrueLorUrl)
    assert(!lorURI.isMessageUrl)
    assert(!lorURI.isCommentUrl)
    assertEquals("", lorURI.formatJump(messageDao, groupService, canon))
    assertEquals("https://127.0.0.1:8085/search.jsp?q=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on",
      lorURI.canonize(canon))

  test("test7"):
    // search url unescaped
    val url7 = "http://127.0.0.1:8080/search.jsp?q=привет&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on"
    val lorURI = new LorURL(mainURI, url7)
    assertEquals(-1, lorURI.getMessageId)
    assertEquals(-1, lorURI.getCommentId)
    assert(lorURI.isTrueLorUrl)
    assert(!lorURI.isMessageUrl)
    assert(!lorURI.isCommentUrl)
    assertEquals("", lorURI.formatJump(messageDao, groupService, canon))
    assertEquals("https://127.0.0.1:8085/search.jsp?q=%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on",
      lorURI.canonize(canon))

  test("test8"):
    intercept[URIException]:
      new LorURL(mainURI, "some crap")

  test("test9"):
    intercept[URIException]:
      new LorURL(mainURI, "")

  test("test11"):
    intercept[Exception]:
      new LorURL(mainURI, "127.0.0.1:8080/news/debian/6753486#comment-6753612")

  test("test12"):
    val url12 = "http://127.0.0.1:8080/forum/security/1948661?lastmod=1319623223360#comment-1948668"
    val lorURI = new LorURL(mainURI, url12)
    assertEquals(1948661, lorURI.getMessageId)
    assertEquals(1948668, lorURI.getCommentId)
    assert(lorURI.isTrueLorUrl)
    assert(lorURI.isMessageUrl)
    assert(lorURI.isCommentUrl)
    assertEquals("https://127.0.0.1:8085/forum/security/1948661?cid=1948668",
      lorURI.formatJump(messageDao, groupService, canon))

  test("test13"):
    val url13_1 = "http://www.linux.org.ru/view-news.jsp?tag=c%2B%2B"
    val url13_2 = "http://www.linux.org.ru/view-news.jsp?tag=c++"
    val url13_3 = "http://www.linux.org.ru/view-news.jsp?tag=c+c"
    val lorURI1 = new LorURL(mainLORURI, url13_1)
    val lorURI2 = new LorURL(mainLORURI, url13_2)
    val lorURI3 = new LorURL(mainLORURI, url13_3)
    assertEquals("https://127.0.0.1:8085/view-news.jsp?tag=c++", lorURI1.canonize(canon))
    assertEquals("https://127.0.0.1:8085/view-news.jsp?tag=c++", lorURI2.canonize(canon))
    assertEquals("https://127.0.0.1:8085/view-news.jsp?tag=c+c", lorURI3.canonize(canon))

  test("test14"):
    val url14_1 = "https://www.linux.org.ru/jump-message.jsp?msgid=6890857&amp;cid=6892917"
    val url14_2 = "https://127.0.0.1:8080/jump-message.jsp?msgid=6890857&amp;cid=6892917"
    val lorURI1 = new LorURL(mainLORURI, url14_1)
    val lorURI2 = new LorURL(mainURI, url14_2)

    assertEquals(6890857, lorURI1.getMessageId)
    assertEquals(6892917, lorURI1.getCommentId)
    assert(lorURI1.isTrueLorUrl)
    assert(lorURI1.isMessageUrl)
    assert(lorURI1.isCommentUrl)
    assertEquals("https://127.0.0.1:8085/jump-message.jsp?msgid=6890857&amp;cid=6892917", lorURI1.canonize(canon))
    assertEquals("https://127.0.0.1:8085/forum/general/6890857?cid=6892917",
      lorURI1.formatJump(messageDao, groupService, canon))

    assertEquals(6890857, lorURI2.getMessageId)
    assertEquals(6892917, lorURI2.getCommentId)
    assert(lorURI2.isTrueLorUrl)
    assert(lorURI2.isMessageUrl)
    assert(lorURI2.isCommentUrl)
    assertEquals("https://127.0.0.1:8085/jump-message.jsp?msgid=6890857&amp;cid=6892917", lorURI2.canonize(canon))
    assertEquals("https://127.0.0.1:8085/forum/general/6890857?cid=6892917",
      lorURI2.formatJump(messageDao, groupService, canon))

  test("test15"):
    val url15_1 = "https://www.linux.org.ru/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831"
    val url15_2 = "https://127.0.0.1:8080/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831"
    val lorURI1 = new LorURL(mainLORURI, url15_1)
    val lorURI2 = new LorURL(mainURI, url15_2)

    assertEquals(6944260, lorURI1.getMessageId)
    assertEquals(6944831, lorURI1.getCommentId)
    assert(lorURI1.isTrueLorUrl)
    assert(lorURI1.isMessageUrl)
    assert(lorURI1.isCommentUrl)
    assertEquals("https://127.0.0.1:8085/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831",
      lorURI1.canonize(canon))
    assertEquals("https://127.0.0.1:8085/forum/linux-org-ru/6944260?cid=6944831",
      lorURI1.formatJump(messageDao, groupService, canon))

    assertEquals(6944260, lorURI2.getMessageId)
    assertEquals(6944831, lorURI2.getCommentId)
    assert(lorURI2.isTrueLorUrl)
    assert(lorURI2.isMessageUrl)
    assert(lorURI2.isCommentUrl)
    assertEquals("https://127.0.0.1:8085/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831",
      lorURI2.canonize(canon))
    assertEquals("https://127.0.0.1:8085/forum/linux-org-ru/6944260?cid=6944831",
      lorURI2.formatJump(messageDao, groupService, canon))

  test("testForumatUrlBody"):
    // url == mainURL и mainURL host:port
    val uri1 = new LorURL(mainURI, "http://127.0.0.1:8080/forum/security/1948661?cid=1948668")
    assertEquals("127.0.0.1:8080/...", uri1.formatUrlBody(10))
    assertEquals("127.0.0.1:8080/fo...", uri1.formatUrlBody(20))
    assertEquals(20, uri1.formatUrlBody(20).length)
    assertEquals("127.0.0.1:8080/forum/security/1948661?cid=1948668", uri1.formatUrlBody(80))
    // url == mainURL и mainURL host
    val uri2 = new LorURL(mainLORURI, "https://www.linux.org.ru/search.jsp?q=%D0%B1%D0%BB%D1%8F&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on")
    assertEquals("www.linux.org.ru/...", uri2.formatUrlBody(10))
    assertEquals("www.linux.org.ru/...", uri2.formatUrlBody(20))
    assertEquals(20, uri2.formatUrlBody(20).length)
    assertEquals("www.linux.org.ru/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertop...", uri2.formatUrlBody(80))
    assertEquals(80, uri2.formatUrlBody(80).length)
    // unescaped url == mainURL и mainURL host
    val uri3 = new LorURL(mainLORURI, "https://www.linux.org.ru/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on")
    assertEquals("www.linux.org.ru/...", uri3.formatUrlBody(10))
    assertEquals("www.linux.org.ru/...", uri3.formatUrlBody(20))
    assertEquals(20, uri3.formatUrlBody(20).length)
    assertEquals("www.linux.org.ru/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertop...", uri3.formatUrlBody(80))
    assertEquals(80, uri3.formatUrlBody(80).length)

    // unescaped url != mainURL и mainURL host
    val uri4 = new LorURL(mainLORURI, "https://example.com/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on")
    assertEquals("https:/...", uri4.formatUrlBody(10))
    assertEquals(10, uri4.formatUrlBody(10).length)
    assertEquals("https://example.c...", uri4.formatUrlBody(20))
    assertEquals(20, uri4.formatUrlBody(20).length)
    assertEquals("https://example.com/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_user...", uri4.formatUrlBody(80))
    assertEquals(80, uri4.formatUrlBody(80).length)

    // escaped url != mainURL и mainURL host
    val uri5 = new LorURL(mainLORURI, "https://example.com/search.jsp?q=%D0%B1%D0%BB%D1%8F&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on")
    assertEquals("https:/...", uri5.formatUrlBody(10))
    assertEquals(10, uri5.formatUrlBody(10).length)
    assertEquals("https://example.c...", uri5.formatUrlBody(20))
    assertEquals(20, uri5.formatUrlBody(20).length)
    assertEquals("https://example.com/search.jsp?q=бля&oldQ=&range=ALL&interval=ALL&user=&_user...", uri5.formatUrlBody(80))
    assertEquals(80, uri5.formatUrlBody(80).length)

  test("testBadId"):
    val uri = new LorURL(mainLORURI, "http://www.linux.org.ru/forum/talks/12345678910")

    assert(uri.isTrueLorUrl)
    assert(!uri.isMessageUrl)
    assert(!uri.isCommentUrl)

  test("testCppTag"):
    val uri = new LorURL(mainLORURI, "http://www.linux.org.ru/tags/c++")
    assert(uri.isTrueLorUrl)

    assertEquals("http://www.linux.org.ru/tags/c++", uri.canonize(mainLORURI))