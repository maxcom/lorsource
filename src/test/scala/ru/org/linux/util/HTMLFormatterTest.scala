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
import org.apache.commons.httpclient.URI
import org.mockito.Mockito.{mock, when}
import ru.org.linux.comment.{Comment, CommentDao}
import ru.org.linux.group.{Group, GroupService}
import ru.org.linux.markup.{MarkupType, MessageTextService}
import ru.org.linux.msgbase.MessageText
import ru.org.linux.reaction.Reactions
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.{Topic, TopicDao}
import ru.org.linux.user.User
import ru.org.linux.user.UserService
import ru.org.linux.util.bbcode.LorCodeService
import ru.org.linux.util.bbcode.tags.QuoteTag.{citeFooter, citeHeader}
import ru.org.linux.util.formatter.ToHtmlFormatter
import ru.org.linux.util.markdown.FlexmarkMarkdownFormatter

import java.sql.Timestamp
import java.time.Instant

class HTMLFormatterTest extends FunSuite:
  private val Text1 = "Here is www.linux.org.ru, have fun! :-)"
  private val Result1 = "Here is <a href=\"https://www.linux.org.ru\">www.linux.org.ru</a>, have fun! :-)"

  private val Text2 = "Here is http://linux.org.ru, have fun! :-)"
  private val Result2 = "Here is <a href=\"http://linux.org.ru\">http://linux.org.ru</a>, have fun! :-)"

  private val Text3 = "Long url: http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651"
  private val Result3 =
    "Long url: <a href=\"https://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651\">www.linux.org.ru/...</a>"

  private val Text8 = "Long url: http://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651&a=b"
  private val Result8 =
    "Long url: <a href=\"https://www.linux.org.ru/profile/maxcom/view-message.jsp?msgid=1993651&amp;a=b\">www.linux.org.ru/...</a>"

  private val Text9 = "(http://ru.wikipedia.org/wiki/Blah_(blah))"
  private val Result9 =
    "(<a href=\"http://ru.wikipedia.org/wiki/Blah_(blah)\">http://ru.wikipedia.org/wiki/Blah_(blah)</a>)"

  private val Text10 = "Twitter url: https://twitter.com/#!/l_o_r"
  private val Result10 = "Twitter url: <a href=\"https://twitter.com/#!/l_o_r\">https://twitter.com/#!/l_o_r</a>"

  private val Text11 =
    "Long url: http://www.google.com.ua/search?client=opera&rls=en&q=InsireData&sourceid=opera&ie=utf-8&oe=utf-8&channel=suggest#sclient=psy-ab&hl=uk&client=opera&hs=kZt&rls=en&channel=suggest&source=hp&q=InsireData+lisp&pbx=1&oq=InsireData+lisp&aq=f&aqi=&aql=1&gs_sm=e&gs_upl=3936l5946l0l6137l13l9l3l0l0l0l253l1481l0.6.2l10l0&bav=on.2,or.r_gc.r_pw.,cf.osb&fp=35c95703241399bd&biw=1271&bih=694"
  private val Result11 =
    "Long url: <a href=\"http://www.google.com.ua/search?client=opera&amp;rls=en&amp;q=InsireData&amp;sourceid=opera&amp;ie=utf-8&amp;oe=utf-8&amp;channel=suggest#sclient=psy-ab&amp;hl=uk&amp;client=opera&amp;hs=kZt&amp;rls=en&amp;channel=suggest&amp;source=hp&amp;q=InsireData+lisp&amp;pbx=1&amp;oq=InsireData+lisp&amp;aq=f&amp;aqi=&amp;aql=1&amp;gs_sm=e&amp;gs_upl=3936l5946l0l6137l13l9l3l0l0l0l253l1481l0.6.2l10l0&amp;bav=on.2,or.r_gc.r_pw.,cf.osb&amp;fp=35c95703241399bd&amp;biw=1271&amp;bih=694\">http://www.google.com.ua/search?client=opera&amp;rls=en&amp;q=InsireData&amp;...</a>"

  private val Text12 = "with login: ftp://olo:olor@o.example.org/olo/2a-ep4ce22/2a-ep4ce22_introduction.pdf"
  private val Result12 =
    "with login: <a href=\"ftp://olo:olor@o.example.org/olo/2a-ep4ce22/2a-ep4ce22_introduction.pdf\">ftp://olo:olor@o.example.org/olo/2a-ep4ce22/2a-ep4ce22_introduction.pdf</a>"

  private val Text13 = "with login: ftp://olor@o.example.org/olo/2a-ep4ce22/2a-ep4ce22_introduction.pdf"
  private val Result13 =
    "with login: <a href=\"ftp://olor@o.example.org/olo/2a-ep4ce22/2a-ep4ce22_introduction.pdf\">ftp://olor@o.example.org/olo/2a-ep4ce22/2a-ep4ce22_introduction.pdf</a>"

  private val Text14 = "with first one symbol: http://a.test.com"
  private val Result14 = "with first one symbol: <a href=\"http://a.test.com\">http://a.test.com</a>"

  private val Text15 = "with www: www.test.com"
  private val Result15 = "with www: <a href=\"http://www.test.com\">http://www.test.com</a>"

  private val Text16 = "with ftp: ftp.test.com"
  private val Result16 = "with ftp: <a href=\"ftp://ftp.test.com\">ftp://ftp.test.com</a>"

  private val Text17 = "http://translate.google.com/?sl=en&tl=ru#ru|en|%D0%BE%D1%81%D1%91%D0%BB"
  private val Result17 =
    "<a href=\"http://translate.google.com/?sl=en&amp;tl=ru#ru|en|%D0%BE%D1%81%D1%91%D0%BB\">http://translate.google.com/?sl=en&amp;tl=ru#ru|en|осёл</a>"

  private val Text17_2 = "http://translate.google.com/?sl=en&amp;tl=ru#ru|en|осёл"
  private val Result17_2 =
    "<a href=\"http://translate.google.com/?sl=en&amp;tl=ru#ru%7Cen%7C%D0%BE%D1%81%D1%91%D0%BB\">http://translate.google.com/?sl=en&amp;tl=ru#ru|en|осёл</a>"

  private val Text18 =
    "http://smartphonebenchmarks.com/index.php?filter_model[]=all&filter_cpu[]=Qualcomm+Snapdragon+MSM8255&filter_cpu[]=Texas+Instrument+OMAP+3610"
  private val Result18 =
    "<a href=\"http://smartphonebenchmarks.com/index.php?filter_model%5B%5D=all&amp;filter_cpu%5B%5D=Qualcomm+Snapdragon+MSM8255&amp;filter_cpu%5B%5D=Texas+Instrument+OMAP+3610\">http://smartphonebenchmarks.com/index.php?filter_model[]=all&amp;filter_cpu[]...</a>"

  private val Text19 = "Test *.myftp.org test"
  private val Result19 = "Test *.myftp.org test"

  private val GuaranteedCrash = "\"http://www.google.com/\""

  private val LinkWithUnderscore = "http://www.phoronix.com/scan.php?page=article&item=intel_core_i7&num=1"
  private val LinkWithParamOnly = "http://www.phoronix.com/scan.php?page=article#anchor"
  private val Rfc1738 =
    "http://www.phoronix.com/scan.php?page=article&item=intel_core_i7&Мама_мыла_раму&$-_.+!*'(,)=$-_.+!*'(),#anchor"
  private val CyrLink =
    "http://ru.wikipedia.org/wiki/Литературный_'негр'(Fran\u00C7ais\u0152uvre_\u05d0)?негр=эфиоп&эфиоп"
  private val GoogleCache =
    "http://74.125.95.132/search?q=cache:fTsc8ze3IxIJ:forum.springsource.org/showthread.php%3Ft%3D53418+spring+security+openid&cd=1&hl=en&ct=clnk&gl=us"

  private val UrlWithAt = "http://www.mail-archive.com/samba@lists.samba.org/msg58308.html"
  private val Latin1Supplement = "http://de.wikipedia.org/wiki/Großes_ß#Unicode"
  private val Greek = "http://el.wikipedia.org/wiki/άλλες"
  private val Qp = "http://www.ozon.ru/?context=search&text=%D8%E8%EB%E4%F2"
  private val Qp2 = "http://ru.wikipedia.org/wiki/%C4%E5%ED%FC_%EF%EE%EB%EE%F2%E5%ED%F6%E0"
  private val EmptyAnchor = "http://www.google.com/#"
  private val SlashAfterAmp =
    "http://extensions.joomla.org/extensions/communities-&-groupware/ratings-&-reviews/5483/details"

  private val Now = Timestamp.from(Instant.now())

  private def createTopic(id: Int, groupId: Int, title: String): Topic =
    Topic(
      id = id,
      postscore = 0,
      sticky = false,
      linktext = null,
      url = null,
      title = title,
      authorUserId = 0,
      groupId = groupId,
      deleted = false,
      expired = false,
      commitby = 0,
      postdate = Now,
      commitDate = null,
      groupUrl = "",
      lastModified = Now,
      sectionId = 0,
      commentCount = 0,
      commited = true,
      notop = false,
      userAgentId = 0,
      postIP = "",
      resolved = false,
      minor = false,
      draft = false,
      allowAnonymous = false,
      reactions = Reactions.empty,
      expireDate = null,
      openWarnings = 0
    )

  private def createComment(id: Int): Comment =
    Comment(
      id = id,
      title = "",
      userid = 1,
      replyTo = 0,
      topicId = 0,
      deleted = false,
      postdate = Now,
      userAgentId = 0,
      postIP = null,
      editorId = 0,
      editDate = null,
      editCount = 0,
      reactions = Reactions.empty
    )

  private def createGroup(url: String): Group =
    val g = mock(classOf[Group])
    when(g.getUrl).thenReturn(url)
    g

  private def createUser(nick: String): User =
    User(
      nick = nick,
      id = 1,
      canmod = false,
      candel = false,
      anonymous = false,
      corrector = false,
      blocked = false,
      password = "",
      score = 0,
      maxScore = 0,
      photo = null,
      email = null,
      fullName = null,
      unreadEvents = 0,
      frozenUntil = null,
      activated = true
    )

  private lazy val (toHtmlFormatter, toHtmlFormatter20, lorCodeService, textService) = initServices()

  private def initServices(): (ToHtmlFormatter, ToHtmlFormatter, LorCodeService, MessageTextService) =
    val mainURI = new URI("http://www.linux.org.ru/", true, "UTF-8")
    val secureURI = new URI("https://www.linux.org.ru/", true, "UTF-8")

    val topicDao = mock(classOf[TopicDao])
    val groupService = mock(classOf[GroupService])
    val commentDao = mock(classOf[CommentDao])

    val message1 = createTopic(6753486, 1, "привет1")
    val message2 = createTopic(6893165, 2, "привет2")
    val message3 = createTopic(6890857, 3, "привет3")
    val message12 = createTopic(1948661, 12, "привет12")
    val message15 = createTopic(6944260, 15, "привет15")
    val messageHistory = createTopic(6992532, 99, "привет история")

    val group1 = createGroup("/news/debian/")
    val group2 = createGroup("/forum/talks/")
    val group3 = createGroup("/forum/general/")
    val group12 = createGroup("/forum/security/")
    val group15 = createGroup("/forum/linux-org-ru/")
    val groupHistory = createGroup("/news/kernel/")

    val comment = createComment(0)

    when(groupService.getGroup(1)).thenReturn(group1)
    when(groupService.getGroup(2)).thenReturn(group2)
    when(groupService.getGroup(3)).thenReturn(group3)
    when(groupService.getGroup(12)).thenReturn(group12)
    when(groupService.getGroup(15)).thenReturn(group15)
    when(groupService.getGroup(99)).thenReturn(groupHistory)
    when(topicDao.getById(6753486)).thenReturn(message1)
    when(topicDao.getById(6893165)).thenReturn(message2)
    when(topicDao.getById(6890857)).thenReturn(message3)
    when(topicDao.getById(1948661)).thenReturn(message12)
    when(topicDao.getById(6944260)).thenReturn(message15)
    when(topicDao.getById(6992532)).thenReturn(messageHistory)
    when(commentDao.getById(6892917)).thenReturn(comment)
    when(commentDao.getById(1948675)).thenReturn(comment)
    when(commentDao.getById(6944831)).thenReturn(comment)

    val siteConfig = mock(classOf[SiteConfig])
    when(siteConfig.getMainURI).thenReturn(mainURI)
    when(siteConfig.getSecureURI).thenReturn(secureURI)

    val fmt = new ToHtmlFormatter
    fmt.setSiteConfig(siteConfig)
    fmt.setTopicDao(topicDao)
    fmt.setGroupService(groupService)
    fmt.setCommentDao(commentDao)

    val fmt20 = new ToHtmlFormatter
    fmt20.setSiteConfig(siteConfig)
    fmt20.setTopicDao(topicDao)
    fmt20.setMaxLength(20)
    fmt20.setCommentDao(commentDao)

    val lcs = new LorCodeService(null, fmt)
    val ms =
      new MessageTextService(
        lcs,
        new FlexmarkMarkdownFormatter(siteConfig, topicDao, commentDao, mock(classOf[UserService]), fmt))

    (fmt, fmt20, lcs, ms)

  test("testToHtmlFormatter") {
    assertEquals(toHtmlFormatter.format(Text1, false), Result1)
    assertEquals(toHtmlFormatter.format(Text2, false), Result2)
    assertEquals(toHtmlFormatter20.format(Text3, false), Result3)
    assertEquals(toHtmlFormatter20.format(Text8, false), Result8)
    assertEquals(toHtmlFormatter.format(Text9, false), Result9)
    assertEquals(toHtmlFormatter.format(Text10, false), Result10)
    assertEquals(toHtmlFormatter.format(Text11, false), Result11)
    assertEquals(toHtmlFormatter.format(Text12, false), Result12)
    assertEquals(toHtmlFormatter.format(Text13, false), Result13)
    assertEquals(toHtmlFormatter.format(Text14, false), Result14)
    assertEquals(toHtmlFormatter.format(Text15, false), Result15)
    assertEquals(toHtmlFormatter.format(Text16, false), Result16)
    assertEquals(toHtmlFormatter.format(Text17, false), Result17)
    assertEquals(toHtmlFormatter.format(Text17_2, false), Result17_2)
    assertEquals(toHtmlFormatter.format(Text18, false), Result18)
    assertEquals(toHtmlFormatter.format(Text19, false), Result19)
    assert(toHtmlFormatter.format(LinkWithUnderscore, false).endsWith("</a>"))
    assert(toHtmlFormatter.format(LinkWithParamOnly, false).endsWith("</a>"))
    assert(toHtmlFormatter.format(Rfc1738, false).endsWith("</a>"))
    assert(toHtmlFormatter.format(CyrLink, false).endsWith("</a>"))
    assert(toHtmlFormatter.format(GoogleCache, false).endsWith("</a>"))
    assert(toHtmlFormatter.format(UrlWithAt, false).endsWith("</a>"))
    assert(toHtmlFormatter.format(Latin1Supplement, false).endsWith("</a>"))
    assert(toHtmlFormatter.format(Greek, false).endsWith("</a>"))
    assert(toHtmlFormatter.format(Qp, false).endsWith("</a>"))
    assert(toHtmlFormatter.format(EmptyAnchor, false).endsWith("</a>"))
    assert(toHtmlFormatter.format(SlashAfterAmp, false).endsWith("</a>"))
  }

  test("testUrlParse") {
    assertEquals(
      toHtmlFormatter.format(Greek, false),
      "<a href=\"http://el.wikipedia.org/wiki/%CE%AC%CE%BB%CE%BB%CE%B5%CF%82\">http://el.wikipedia.org/wiki/άλλες</a>"
    )
    assertEquals(
      toHtmlFormatter.format(Rfc1738, false),
      "<a href=\"http://www.phoronix.com/scan.php?page=article&amp;item=intel_core_i7&amp;%D0%9C%D0%B0%D0%BC%D0%B0_%D0%BC%D1%8B%D0%BB%D0%B0_%D1%80%D0%B0%D0%BC%D1%83&amp;$-_.+!*'(,)=$-_.+!*'(),#anchor\">http://www.phoronix.com/scan.php?page=article&amp;item=intel_core_i7&amp;Мама...</a>"
    )
    assertEquals(
      toHtmlFormatter.format(CyrLink, false),
      "<a href=\"http://ru.wikipedia.org/wiki/%D0%9B%D0%B8%D1%82%D0%B5%D1%80%D0%B0%D1%82%D1%83%D1%80%D0%BD%D1%8B%D0%B9_'%D0%BD%D0%B5%D0%B3%D1%80'(Fran%C3%87ais%C5%92uvre_%D7%90)?%D0%BD%D0%B5%D0%B3%D1%80=%D1%8D%D1%84%D0%B8%D0%BE%D0%BF&amp;%D1%8D%D1%84%D0%B8%D0%BE%D0%BF\">http://ru.wikipedia.org/wiki/Литературный_'негр'(FranÇaisŒuvre_א)?негр=эфиоп&amp;...</a>"
    )
    assertEquals(
      toHtmlFormatter.format(GoogleCache, false),
      "<a href=\"http://74.125.95.132/search?q=cache:fTsc8ze3IxIJ:forum.springsource.org/showthread.php%3Ft%3D53418+spring+security+openid&amp;cd=1&amp;hl=en&amp;ct=clnk&amp;gl=us\">http://74.125.95.132/search?q=cache:fTsc8ze3IxIJ:forum.springsource.org/showt...</a>"
    )
    assertEquals(
      toHtmlFormatter.format(Qp, false),
      "<a href=\"http://www.ozon.ru/?context=search&amp;text=%D8%E8%EB%E4%F2\">http://www.ozon.ru/?context=search&amp;text=%D8%E8%EB%E4%F2</a>"
    )
    assertEquals(
      toHtmlFormatter.format(Qp2, false),
      "<a href=\"http://ru.wikipedia.org/wiki/%C4%E5%ED%FC_%EF%EE%EB%EE%F2%E5%ED%F6%E0\">http://ru.wikipedia.org/wiki/%C4%E5%ED%FC_%EF%EE%EB%EE%F2%E5%ED%F6%E0</a>"
    )
  }

  test("testURLs") {
    val url1 = "http://www.linux.org.ru/forum/general/6890857/page2?lastmod=1319022386177#comment-6892917"
    assertEquals(
      toHtmlFormatter.format(url1, false),
      "<a href=\"https://www.linux.org.ru/forum/general/6890857?cid=6892917\" title=\"привет3\">привет3 (комментарий)</a>"
    )
    val url3 = "http://www.linux.org.ru/jump-message.jsp?msgid=1948661&cid=1948675"
    assertEquals(
      toHtmlFormatter.format(url3, false),
      "<a href=\"https://www.linux.org.ru/forum/security/1948661?cid=1948675\" title=\"привет12\">привет12 (комментарий)</a>"
    )
    val url15 = "https://www.linux.org.ru/forum/linux-org-ru/6944260/page4?lastmod=1320084656912#comment-6944831"
    assertEquals(
      toHtmlFormatter.format(url15, false),
      "<a href=\"https://www.linux.org.ru/forum/linux-org-ru/6944260?cid=6944831\" title=\"привет15\">привет15 (комментарий)</a>"
    )
    val urlHistory = "http://www.linux.org.ru/news/kernel/6992532/history"
    assertEquals(
      toHtmlFormatter.format(urlHistory, true),
      "<a href=\"https://www.linux.org.ru/news/kernel/6992532/history\">www.linux.org.ru/news/kernel/6992532/history</a>"
    )
    assertEquals(
      toHtmlFormatter.format("www.linux.org.ru/forum/lor-source/6992532/comments", true),
      "<a href=\"https://www.linux.org.ru/forum/lor-source/6992532/comments\">www.linux.org.ru/forum/lor-source/6992532/comments</a>"
    )
  }

  test("testCrash") {
    assertEquals(
      toHtmlFormatter.format(GuaranteedCrash, false),
      "&quot;<a href=\"http://www.google.com/&quot;\">http://www.google.com/&quot;</a>")
  }

  test("testHTMLEscape") {
    val str1 = "This is an entity &#1999;"
    val s1 = ToHtmlFormatter.strangeEscapeHtml(str1)
    assertEquals(s1, str1, "String should remain unescaped")

    val str2 = "a&b"
    val s2 = ToHtmlFormatter.strangeEscapeHtml(str2)
    assertEquals(s2, "a&amp;b", "Ampersand should be escaped")

    assertEquals(ToHtmlFormatter.strangeEscapeHtml("<script>"), "&lt;script&gt;")
    assertEquals(ToHtmlFormatter.strangeEscapeHtml("&nbsp;"), "&nbsp;")
    assertEquals(ToHtmlFormatter.strangeEscapeHtml("&#41;&#41;&#41;"), "&#41;&#41;&#41;")
  }

  test("testToLorCodeFormatter2") {
    val text = Array(
      ">one\n",
      ">one\n>one\n",
      ">>one\n>teo\n",
      "due>>one\n>teo\n>>neo\nwuf?\nok",
      "due\n>>one\n>teo\n>>neo\nwuf?\nok",
      ">one\n\n\n\n>one")
    val bbTex = Array(
      "[quote]one[/quote]",
      "[quote]one[br]one[/quote]",
      "[quote][quote]one[br][/quote]teo[/quote]",
      "due>>one\n[quote]teo[br][quote]neo[br][/quote][/quote]wuf?\nok",
      "due\n[quote][quote]one[br][/quote]teo[br][quote]neo[br][/quote][/quote]wuf?\nok",
      "[quote]one[br][/quote]\n\n[quote]one[/quote]"
    )
    val bb = Array(
      "[quote]one[/quote]",
      "[quote]one[br]one[/quote]",
      "[quote][quote]one[br][/quote]teo[/quote]",
      "due>>one[br][quote]teo[br][quote]neo[br][/quote][/quote]wuf?[br]ok",
      "due[br][quote][quote]one[br][/quote]teo[br][quote]neo[br][/quote][/quote]wuf?[br]ok",
      "[quote]one[br][/quote][br][br][quote]one[/quote]"
    )

    val htmlTex = Array(
      citeHeader + "<p>one</p>" + citeFooter,
      citeHeader + "<p>one<br>one</p>" + citeFooter,
      citeHeader + citeHeader + "<p>one<br></p>" + citeFooter + "<p>teo</p>" + citeFooter,
      "<p>due&gt;&gt;one\n</p>" + citeHeader + "<p>teo<br></p>" + citeHeader + "<p>neo<br></p>" + citeFooter +
        citeFooter + "<p>wuf?\nok</p>",
      "<p>due\n</p>" + citeHeader + citeHeader + "<p>one<br></p>" + citeFooter + "<p>teo<br></p>" + citeHeader +
        "<p>neo<br></p>" + citeFooter + citeFooter + "<p>wuf?\nok</p>",
      citeHeader + "<p>one<br></p>" + citeFooter + citeHeader + "<p>one</p>" + citeFooter
    )

    val html = Array(
      citeHeader + "<p>one</p>" + citeFooter,
      citeHeader + "<p>one<br>one</p>" + citeFooter,
      citeHeader + citeHeader + "<p>one<br></p>" + citeFooter + "<p>teo</p>" + citeFooter,
      "<p>due&gt;&gt;one<br></p>" + citeHeader + "<p>teo<br></p>" + citeHeader + "<p>neo<br></p>" + citeFooter +
        citeFooter + "<p>wuf?<br>ok</p>",
      "<p>due<br></p>" + citeHeader + citeHeader + "<p>one<br></p>" + citeFooter + "<p>teo<br></p>" + citeHeader +
        "<p>neo<br></p>" + citeFooter + citeFooter + "<p>wuf?<br>ok</p>",
      citeHeader + "<p>one<br></p>" + citeFooter + "<p><br><br></p>" + citeHeader + "<p>one</p>" + citeFooter
    )

    for i <- text.indices do
      val entry = text(i)
      assertEquals(LorCodeService.prepareLorcode(entry), bbTex(i))
      assertEquals(lorCodeService.parseComment(entry, false, LorCodeService.Lorcode), htmlTex(i))
      assertEquals(LorCodeService.prepareUlb(entry), bb(i))
      assertEquals(lorCodeService.parseComment(entry, false, LorCodeService.Ulb), html(i))
  }

  test("inCodeEscape") {
    assertEquals(
      lorCodeService.parseTopic("[code]&#9618;[/code]", false, LorCodeService.Plain),
      "<div class=\"code\"><pre class=\"no-highlight\"><code>&amp;#9618;</code></pre></div>"
    )
    assertEquals(lorCodeService.parseTopic("&#9618;", false, LorCodeService.Plain), "<p>&#9618;</p>")
  }

  test("listTest2") {
    val a = "[list]\n[*]one\n[*]two\n[*]three\n[/list]"
    assertEquals(LorCodeService.prepareUlb(a), "[list][br][*]one[br][*]two[br][*]three[br][/list]")

    assertEquals(
      lorCodeService.parseComment(a, false, LorCodeService.Ulb),
      "<p><br></p><ul><li>one<br></li><li>two<br></li><li>three<br></li></ul>")
    assertEquals(
      lorCodeService.parseComment(a, false, LorCodeService.Ulb),
      "<p><br></p><ul><li>one<br></li><li>two<br></li><li>three<br></li></ul>")
    assertEquals(
      lorCodeService.parseTopic(a, false, LorCodeService.Ulb),
      "<p><br></p><ul><li>one<br></li><li>two<br></li><li>three<br></li></ul>")
    assertEquals(
      lorCodeService.parseTopic(a, false, LorCodeService.Ulb),
      "<p><br></p><ul><li>one<br></li><li>two<br></li><li>three<br></li></ul>")

    assertEquals(
      lorCodeService.parseComment(a, false, LorCodeService.Plain),
      "<ul><li>one\n</li><li>two\n</li><li>three\n</li></ul>")
    assertEquals(
      lorCodeService.parseComment(a, false, LorCodeService.Plain),
      "<ul><li>one\n</li><li>two\n</li><li>three\n</li></ul>")
    assertEquals(
      lorCodeService.parseTopic(a, false, LorCodeService.Plain),
      "<ul><li>one\n</li><li>two\n</li><li>three\n</li></ul>")
    assertEquals(
      lorCodeService.parseTopic(a, false, LorCodeService.Plain),
      "<ul><li>one\n</li><li>two\n</li><li>three\n</li></ul>")
  }

  test("listTest3") {
    val a = "[list]\n[*]one\n\n[*]two\n[*]three\n[/list]"
    assertEquals(LorCodeService.prepareUlb(a), "[list][br][*]one[br][br][*]two[br][*]three[br][/list]")

    assertEquals(
      lorCodeService.parseComment(a, false, LorCodeService.Ulb),
      "<p><br></p><ul><li>one<br><br></li><li>two<br></li><li>three<br></li></ul>")
    assertEquals(
      lorCodeService.parseComment(a, false, LorCodeService.Ulb),
      "<p><br></p><ul><li>one<br><br></li><li>two<br></li><li>three<br></li></ul>")
    assertEquals(
      lorCodeService.parseTopic(a, false, LorCodeService.Ulb),
      "<p><br></p><ul><li>one<br><br></li><li>two<br></li><li>three<br></li></ul>")
    assertEquals(
      lorCodeService.parseTopic(a, false, LorCodeService.Ulb),
      "<p><br></p><ul><li>one<br><br></li><li>two<br></li><li>three<br></li></ul>")

    assertEquals(
      lorCodeService.parseComment(a, false, LorCodeService.Plain),
      "<ul><li>one</li><li>two\n</li><li>three\n</li></ul>")
    assertEquals(
      lorCodeService.parseComment(a, false, LorCodeService.Plain),
      "<ul><li>one</li><li>two\n</li><li>three\n</li></ul>")
    assertEquals(
      lorCodeService.parseTopic(a, false, LorCodeService.Plain),
      "<ul><li>one</li><li>two\n</li><li>three\n</li></ul>")
    assertEquals(
      lorCodeService.parseTopic(a, false, LorCodeService.Plain),
      "<ul><li>one</li><li>two\n</li><li>three\n</li></ul>")
  }

  test("listTest4") {
    val a = "[list]\n[*]one\n\ncrap\n[*]two\n[*]three\n[/list]"
    assertEquals(LorCodeService.prepareUlb(a), "[list][br][*]one[br][br]crap[br][*]two[br][*]three[br][/list]")

    assertEquals(
      lorCodeService.parseComment(a, false, LorCodeService.Ulb),
      "<p><br></p><ul><li>one<br><br>crap<br></li><li>two<br></li><li>three<br></li></ul>")
    assertEquals(
      lorCodeService.parseComment(a, false, LorCodeService.Ulb),
      "<p><br></p><ul><li>one<br><br>crap<br></li><li>two<br></li><li>three<br></li></ul>")
    assertEquals(
      lorCodeService.parseTopic(a, false, LorCodeService.Ulb),
      "<p><br></p><ul><li>one<br><br>crap<br></li><li>two<br></li><li>three<br></li></ul>")
    assertEquals(
      lorCodeService.parseTopic(a, false, LorCodeService.Ulb),
      "<p><br></p><ul><li>one<br><br>crap<br></li><li>two<br></li><li>three<br></li></ul>")

    assertEquals(
      lorCodeService.parseComment(a, false, LorCodeService.Plain),
      "<ul><li>one<p>crap\n</p></li><li>two\n</li><li>three\n</li></ul>")
    assertEquals(
      lorCodeService.parseComment(a, false, LorCodeService.Plain),
      "<ul><li>one<p>crap\n</p></li><li>two\n</li><li>three\n</li></ul>")
    assertEquals(
      lorCodeService.parseTopic(a, false, LorCodeService.Plain),
      "<ul><li>one<p>crap\n</p></li><li>two\n</li><li>three\n</li></ul>")
    assertEquals(
      lorCodeService.parseTopic(a, false, LorCodeService.Plain),
      "<ul><li>one<p>crap\n</p></li><li>two\n</li><li>three\n</li></ul>")
  }

  test("testOg") {
    assertEquals(lorCodeService.extractPlain("hello", LorCodeService.Plain), "hello")

    assertEquals(
      lorCodeService.extractPlain(
        """[list]
          |[*]one
          |
          |crap
          |[*]two
          |[*]three
          |[/list]""".stripMargin,
        LorCodeService.Plain
      ),
      "one crap two three"
    )
    assertEquals(
      lorCodeService.extractPlain(
        "due\n[quote][quote]one[br][/quote]teo[br][quote]neo[br][/quote][/quote]wuf?\nok",
        LorCodeService.Plain),
      "due ««one» teo «neo»» wuf?\nok"
    )
    assertEquals(
      MessageTextService.trimPlainText(
        lorCodeService.extractPlain("[code]&#9618;[/code]", LorCodeService.Plain),
        250,
        true),
      "&amp;#9618;")

    val txt =
      "many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]many many [b]texxt [/b]"
    assertEquals(
      MessageTextService.trimPlainText(lorCodeService.extractPlain(txt, LorCodeService.Plain), 250, true),
      "many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  texxt many many  t..."
    )
    assertEquals(
      MessageTextService.trimPlainText(lorCodeService.extractPlain(txt, LorCodeService.Plain), 250, true).length,
      250 + 3)
  }

  test("testCropLinkBody") {
    assertEquals(
      toHtmlFormatter.format("Ссылка: http://www.opera.com/browser/download/?os=linux-x86-64&ver=12.00&local=y", false),
      "Ссылка: <a href=\"http://www.opera.com/browser/download/?os=linux-x86-64&amp;ver=12.00&amp;local=y\">http://www.opera.com/browser/download/?os=linux-x86-64&amp;ver=12.00&amp;local=y</a>"
    )
    assertEquals(
      toHtmlFormatter.format(
        "http://www.linux.org.ru/test/tost/holokoust/12345678/?parameter=unknown&option=true",
        true),
      "<a href=\"https://www.linux.org.ru/test/tost/holokoust/12345678/?parameter=unknown&amp;option=true\">www.linux.org.ru/test/tost/holokoust/12345678/?parameter=unknown&amp;option=true</a>"
    )
  }

  test("testMDash") {
    assertEquals(
      lorCodeService.parseComment(
        "[list][*][url=http://www.freebsd.org/doc/en_US.ISO8859-1/books/pmake/index.html]PMake -- A Tutorial[/url][/list]",
        false,
        LorCodeService.Plain),
      "<ul><li><a href=\"http://www.freebsd.org/doc/en_US.ISO8859-1/books/pmake/index.html\">PMake&nbsp;&mdash; A Tutorial</a></li></ul>"
    )
  }

  test("testInCodeQuotes") {
    assertEquals(
      lorCodeService.parseComment(
        "Smth about \"quotes\"? Look here: [code]I love to eat \"white\" icecream[/code]",
        false,
        LorCodeService.Plain),
      "<p>Smth about &#171;quotes&#187;? Look here: <div class=\"code\"><pre class=\"no-highlight\"><code>I love to eat &quot;white&quot; icecream</code></pre></div></p>"
    )
  }

  test("testLocalBuffer") {
    assertEquals(
      lorCodeService.parseComment("This is simple \"local [u]buffer[/u]\" test ", false, LorCodeService.Plain),
      "<p>This is simple &#171;local <u>buffer</u>&#187; test </p>"
    )
  }

  test("testUrlQuotes") {
    assertEquals(
      lorCodeService.parseComment("www.linux.org.ru/search.jsp?q=\"100%\"", false, LorCodeService.Plain),
      "<p><a href=\"https://www.linux.org.ru/search.jsp?q=&quot;100%25&quot;\">www.linux.org.ru/search.jsp?q=&quot;100%&quot;</a></p>"
    )
    assertEquals(
      lorCodeService.parseComment("http://www.olo.org.ru/search.jsp?q=&quot;privet&quot;", false, LorCodeService.Plain),
      "<p><a href=\"http://www.olo.org.ru/search.jsp?q=&quot;privet&quot;\">http://www.olo.org.ru/search.jsp?q=&quot;privet&quot;</a></p>"
    )
    assertEquals(
      lorCodeService.parseComment(
        "http://127.0.0.1:8080/search.jsp?q=%22%D1%82%D0%B5%D1%81%D1%82-%D1%82%D0%BE%D1%81%D1%82%22&oldQ=&range=ALL&interval=ALL&user=&_usertopic=on&csrf=TccXeqgBc10MvJ786lZFQQ%3D%3D",
        false,
        LorCodeService.Plain
      ),
      "<p><a href=\"http://127.0.0.1:8080/search.jsp?q=%22%D1%82%D0%B5%D1%81%D1%82-%D1%82%D0%BE%D1%81%D1%82%22&amp;oldQ=&amp;range=ALL&amp;interval=ALL&amp;user=&amp;_usertopic=on&amp;csrf=TccXeqgBc10MvJ786lZFQQ%3D%3D\">http://127.0.0.1:8080/search.jsp?q=&quot;тест-тост&quot;&amp;oldQ=&amp;range=ALL&amp;in...</a></p>"
    )
  }

  test("testEmpty") {
    assert(textService.isEmpty(MessageText("[br]", MarkupType.Lorcode)))
    assert(textService.isEmpty(MessageText("[br] ", MarkupType.Lorcode)))
    assert(textService.isEmpty(MessageText("[b] [br][/b][u] ", MarkupType.Lorcode)))
    assert(textService.isEmpty(MessageText("[list][*][br][br][*][u][/u][/list]", MarkupType.Lorcode)))
    assert(
      textService.isEmpty(MessageText("[url]   [/url][list][*][br][br][*][u][/u][/list][/url]", MarkupType.Lorcode)))
    assert(!textService.isEmpty(MessageText("[code]text[/code]", MarkupType.Lorcode)))
  }

  test("testQuotes") {
    assertEquals(
      lorCodeService.parseComment(
        "--new-file (-N) и --undirectional-new-file позволяют сравнивать с \"-\". Если стандартный ввод закрыт, то это воспринимается как несуществующий файл;",
        false,
        LorCodeService.Plain
      ),
      "<p>--new-file (-N) и --undirectional-new-file позволяют сравнивать с &quot;-&quot;. Если стандартный ввод закрыт, то это воспринимается как несуществующий файл;</p>"
    )
    assertEquals(
      lorCodeService.parseComment(
        "--new-file (-N) и --undirectional-new-file позволяют сравнивать с &quot;-&quot;. Если стандартный ввод закрыт, то это воспринимается как несуществующий файл;",
        false,
        LorCodeService.Plain
      ),
      "<p>--new-file (-N) и --undirectional-new-file позволяют сравнивать с &quot;-&quot;. Если стандартный ввод закрыт, то это воспринимается как несуществующий файл;</p>"
    )
  }

  test("encodeLorUrl") {
    assertEquals(
      lorCodeService.parseComment("www.linux.org.ru/forum/linux%3C%3E-org-ru/", false, LorCodeService.Plain),
      "<p><a href=\"https://www.linux.org.ru/forum/linux%3C%3E-org-ru/\">www.linux.org.ru/forum/linux&lt;&gt;-org-ru/</a></p>"
    )
  }

  test("moveInfoMarkdownEscapesLinktext") {
    val user = createUser("maxcom")
    // попытка вырваться из [text](url) и подсунуть ссылку на evil
    val linktext = "a](http://evil)[b"
    val snippet = textService.moveInfo(MarkupType.Markdown, "http://example.com", linktext, user, "/forum/talks/")
    val rendered = textService.renderTopic(MessageText(snippet, MarkupType.Markdown), minimizeCut = false, nofollow = false, "")

    // отдельной ссылки на evil быть не должно
    assert(!rendered.contains("href=\"http://evil"), rendered)
    // легитимная ссылка на месте
    assert(rendered.contains("href=\"http://example.com\""), rendered)
    // текст ссылки выведен буквально (без инъекции)
    assert(rendered.contains(">a](http://evil)[b<"), rendered)
  }

  test("moveInfoMarkdownPreservesNormalLinktext") {
    val user = createUser("maxcom")
    val snippet = textService.moveInfo(MarkupType.Markdown, "http://example.com", "Linux.org.ru", user, "/forum/talks/")
    val rendered = textService.renderTopic(MessageText(snippet, MarkupType.Markdown), minimizeCut = false, nofollow = false, "")

    assert(rendered.contains("<a href=\"http://example.com\">Linux.org.ru</a>"), rendered)
  }

  test("moveInfoHtmlEscapesLinktextAndUrl") {
    val user = createUser("maxcom")
    val snippet = textService.moveInfo(MarkupType.Html, "http://example.com?a=1&b=2", "<b>bold</b>", user, "/forum/talks/")
    val rendered = textService.renderTopic(MessageText(snippet, MarkupType.Html), minimizeCut = false, nofollow = false, "")

    assert(rendered.contains("href=\"http://example.com?a=1&amp;b=2\""), rendered)
    assert(rendered.contains("&lt;b&gt;bold&lt;/b&gt;"), rendered)
    assert(!rendered.contains("<b>bold</b>"), rendered)
  }

  test("moveInfoHtmlNullLinktextFallsBackToDefault") {
    val user = createUser("maxcom")
    val snippet = textService.moveInfo(MarkupType.Html, "http://example.com", null, user, "/forum/talks/")
    val rendered = textService.renderTopic(MessageText(snippet, MarkupType.Html), minimizeCut = false, nofollow = false, "")

    assert(rendered.contains("<a href=\"http://example.com\">Подробности</a>"), rendered)
  }

  test("moveInfoHtmlEmptyLinktextFallsBackToDefault") {
    val user = createUser("maxcom")
    val snippet = textService.moveInfo(MarkupType.Html, "http://example.com", "", user, "/forum/talks/")
    val rendered = textService.renderTopic(MessageText(snippet, MarkupType.Html), minimizeCut = false, nofollow = false, "")

    assert(rendered.contains("<a href=\"http://example.com\">Подробности</a>"), rendered)
  }

  test("moveInfoMarkdownNullLinktextFallsBackToDefault") {
    val user = createUser("maxcom")
    val snippet = textService.moveInfo(MarkupType.Markdown, "http://example.com", null, user, "/forum/talks/")
    val rendered = textService.renderTopic(MessageText(snippet, MarkupType.Markdown), minimizeCut = false, nofollow = false, "")

    assert(rendered.contains("<a href=\"http://example.com\">Подробности</a>"), rendered)
  }

  test("htmlMarkupStripsScriptsAndForms") {
    val raw =
      """<p>text <a href="http://example.com">link</a> <img src="http://example.com/i.png" alt="img"> <b>bold</b> <i>italic</i></p>
        |<script>alert(1)</script>
        |<form action="http://evil.com"><input name="x"><button>go</button></form>
        |<div onmouseover="alert(2)">x</div>
        |<iframe src="http://evil.com"></iframe>
        |<style>body{}</style>
        |<svg/onload=alert(3)>
        |""".stripMargin

    val rendered = textService.renderCommentText(MessageText(raw, MarkupType.Html), nofollow = false)

    assert(!rendered.contains("<script"), rendered)
    assert(!rendered.contains("<form"), rendered)
    assert(!rendered.contains("<input"), rendered)
    assert(!rendered.contains("<button"), rendered)
    assert(!rendered.contains("<iframe"), rendered)
    assert(!rendered.contains("<style"), rendered)
    assert(!rendered.contains("<svg"), rendered)
    assert(!rendered.contains("onmouseover"), rendered)
    assert(!rendered.contains("alert"), rendered)
    assert(rendered.contains("<a href=\"http://example.com\">link</a>"), rendered)
    assert(rendered.contains("<img"), rendered)
    assert(rendered.contains("src=\"http://example.com/i.png\""), rendered)
    assert(rendered.contains("alt=\"img\""), rendered)
    assert(rendered.contains("<b>bold</b>"), rendered)
    assert(rendered.contains("<i>italic</i>"), rendered)
  }

  test("htmlMarkupStripsForTopicToo") {
    val raw = "<p>x</p><script>alert(1)</script><form><button>go</button></form><img src=\"http://e/p.png\">"

    val rendered = textService.renderTopic(MessageText(raw, MarkupType.Html), minimizeCut = false, nofollow = false, "")

    assert(!rendered.contains("<script"), rendered)
    assert(!rendered.contains("<form"), rendered)
    assert(!rendered.contains("<button"), rendered)
    assert(rendered.contains("<img src=\"http://e/p.png\">"), rendered)
  }
end HTMLFormatterTest
