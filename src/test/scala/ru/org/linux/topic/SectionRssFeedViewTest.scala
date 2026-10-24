/*
 *  Copyright 1998-2026 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.org.linux.topic

import munit.FunSuite
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}
import ru.org.linux.gallery.Image
import ru.org.linux.group.Group
import ru.org.linux.markup.MarkupType
import ru.org.linux.poll.{Poll, PollVariant, PollVariantResult, PreparedPoll}
import ru.org.linux.reaction.Reactions
import ru.org.linux.section.{Section, SectionScrollModeEnum}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.tag.TagRef
import ru.org.linux.user.User
import ru.org.linux.util.image.ImageInfo

import java.sql.Timestamp
import java.time.Instant
import java.util.{Collections, Properties}
import scala.jdk.CollectionConverters.*

class SectionRssFeedViewTest extends FunSuite:

  private val pollSection =
    Section(
      name = "Голосования",
      imagepost = false,
      premoderated = true,
      id = Section.Polls,
      votepoll = true,
      scrollMode = SectionScrollModeEnum.SECTION,
      topicsRestriction = 0,
      imageAllowed = false
    )

  private def render(topics: Seq[PreparedTopic], ptitle: String = "Голосования"): String =
    val properties = new Properties()
    properties.setProperty("MainUrl", "https://www.linux.org.ru/")
    properties.setProperty("SecureUrl", "https://www.linux.org.ru/")

    val view = SectionRssFeedView(SiteConfig(properties))
    view.setContentTypes(Map("rss" -> "application/rss+xml").asJava)
    view.setFeedTypes(Map("rss" -> "rss_2.0").asJava)

    val model = new java.util.HashMap[String, AnyRef]()
    model.put("feed-type", "rss")
    model.put("ptitle", ptitle)
    model.put("messages", topics.asJava)

    val response = MockHttpServletResponse()
    view.render(model, MockHttpServletRequest(), response)
    response.getContentAsString

  private def makeTopic(commitDate: Timestamp = null): Topic =
    Topic(
      id = 12345,
      postscore = 0,
      sticky = false,
      linktext = null,
      url = null,
      rawTitle = "Заголовок & <тест>",
      authorUserId = 1,
      groupId = 1,
      deleted = false,
      expired = false,
      commitby = 0,
      postdate = Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")),
      commitDate = commitDate,
      groupUrl = "vote",
      lastModified = Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")),
      sectionId = Section.Polls,
      commentCount = 0,
      commited = true,
      notop = false,
      userAgentId = 0,
      postIP = "127.0.0.1",
      resolved = false,
      minor = false,
      draft = false,
      allowAnonymous = false,
      reactions = Reactions.empty,
      expireDate = null,
      openWarnings = 0
    )

  private def makePreparedTopic(
      topic: Topic,
      processedMessage: String,
      poll: PreparedPoll = null,
      tags: java.util.List[TagRef] = Collections.emptyList(),
      section: Section = pollSection,
      images: java.util.List[PreparedImage] = Collections.emptyList()): PreparedTopic =

    val author = User(
      nick = "author",
      id = 1,
      canmod = false,
      candel = false,
      anonymous = false,
      corrector = false,
      blocked = false,
      password = "password",
      score = 100,
      maxScore = 100,
      photo = null,
      email = null,
      fullName = null,
      unreadEvents = 0,
      frozenUntil = null,
      activated = true
    )

    PreparedTopic(
      message = topic,
      author = author,
      deleteInfo = null,
      deleteUser = null,
      processedMessage = processedMessage,
      poll = poll,
      commiter = null,
      tags = tags,
      group = Group(
        id = 1,
        sectionId = pollSection.id,
        title = "test",
        info = "",
        longInfo = "",
        urlName = "vote",
        image = "",
        stat3 = 0,
        premoderated = true,
        pollPostAllowed = true,
        linksAllowed = false,
        defaultLinkText = "",
        topicRestriction = 0,
        commentsRestriction = 0,
        resolvable = false
      ),
      section = section,
      markupType = MarkupType.Lorcode,
      postscoreInfo = "",
      remark = null,
      showRegisterInvite = false,
      userAgent = null,
      reactions = null,
      warnings = Collections.emptyList(),
      images = images
    )

  test("channel metadata and entry fields"):
    val commitDate = Timestamp.from(Instant.parse("2026-02-01T00:00:00Z"))
    val prepared = makePreparedTopic(makeTopic(commitDate = commitDate), "<p>текст</p>")

    val xml = render(Seq(prepared))

    assert(xml.contains("<rss"), "rss root")
    assert(xml.contains("<title>Linux.org.ru: Голосования</title>"), "channel title")
    assert(xml.contains("<language>ru</language>"), "channel language")
    assert(xml.contains("https://www.linux.org.ru/"), "channel/entry link")

    assert(xml.contains("<dc:creator>author</dc:creator>"), "entry author")
    assert(xml.contains("https://www.linux.org.ru/polls/vote/12345"), "entry link")
    assert(xml.contains("<title>Заголовок &amp; &lt;тест&gt;</title>"), "entry title escaped")
    assert(xml.contains("<pubDate>Sun, 01 Feb 2026 00:00:00 GMT</pubDate>"), "commitDate preferred over postdate")

  test("postdate used when commitDate is null"):
    val prepared = makePreparedTopic(makeTopic(commitDate = null), "")
    val xml = render(Seq(prepared))

    assert(xml.contains("<pubDate>Thu, 01 Jan 2026 00:00:00 GMT</pubDate>"), "postdate used")

  test("description contains message, poll and escaped tags"):
    val poll = PreparedPoll(
      poll = Poll(id = 1, topic = 12345, multiSelect = false, variants = Seq(PollVariant(1, "вариант"))),
      totalOfVotesPerson = 1,
      variants1 = Seq(PollVariantResult(id = 1, label = "вариант", votes = 1, userVoted = false))
    )
    val tags = Seq(TagRef("линукс", Some("/tag/linux"))).asJava
    val prepared = makePreparedTopic(makeTopic(), "<p>текст &amp; сообщение</p>", poll = poll, tags = tags)

    val xml = render(Seq(prepared))

    assert(xml.contains("&lt;p&gt;текст &amp;amp; сообщение&lt;/p&gt;"), "processed message escaped")
    assert(xml.contains("Всего голосов:"), "poll rendered")
    assert(xml.contains("&lt;a class=tag rel=tag href=\"/tag/linux\"&gt;"), "tag link rendered")

  test("description contains simplified image markup when present"):
    val info = ImageInfo("src/test/resources/images/i_want_to_be_a_hero__apng_animated__by_tamalesyatole-d5ht8eu.png")
    val image = PreparedImage(
      mediumName = "images/1/500px.jpg",
      mediumInfo = info,
      fullName = "images/1/full.jpg",
      fullInfo = info,
      image = Image(id = 1, topicId = 12345, original = "images/photo.jpg", deleted = false),
      lazyLoad = false
    )

    val prepared = makePreparedTopic(makeTopic(), "<p>текст</p>", images = Seq(image).asJava)

    val xml = render(Seq(prepared))

    val expectedImg =
      """&lt;img src="images/1/500px.jpg" """ +
        """srcset="images/1/500px.jpg 500w, images/1/1000px.jpg 1000w, images/1/full.jpg 1275w"&gt;"""
    assert(xml.contains(expectedImg), s"image markup not found in:\n$xml")

  test("description is stripped of invalid XML characters"):
    val prepared = makePreparedTopic(makeTopic(), "<p>тек\u0000ст</p>")

    val xml = render(Seq(prepared))

    assert(xml.contains("&lt;p&gt;текст&lt;/p&gt;"), "control char removed, rest kept")
    assert(!xml.contains("&#0;") && !xml.contains("\u0000"), "no raw control char in output")

  test("poll is not rendered when section does not allow polls"):
    val prepared = makePreparedTopic(
      makeTopic(),
      "<p>текст</p>",
      poll = PreparedPoll(Poll(1, 12345, false, Seq.empty), 0, variants1 = Seq.empty),
      section = pollSection.copy(votepoll = false))

    val xml = render(Seq(prepared))

    assert(!xml.contains("Всего голосов"), "poll omitted")
