/*
 * Copyright 1998-2026 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.rights

import munit.FunSuite
import ru.org.linux.auth.{AuthorizedSession, IpBlockInfo}
import ru.org.linux.group.Group
import ru.org.linux.markup.MarkupType
import ru.org.linux.reaction.Reactions
import ru.org.linux.section.{Section, SectionScrollModeEnum}
import ru.org.linux.topic.TopicPermissionService.*
import ru.org.linux.topic.{PreparedTopic, Topic}
import ru.org.linux.user.{Profile, User}

import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.jdk.CollectionConverters.*

class EditTopicCheckerTest extends FunSuite:

  private val articlesSection =
    Section(
      name = "Articles",
      imagepost = false,
      premoderated = true,
      id = Section.Articles,
      votepoll = false,
      scrollMode = SectionScrollModeEnum.NO_SCROLL,
      topicsRestriction = POSTSCORE_UNRESTRICTED,
      imageAllowed = false
    )

  private val forumSection =
    Section(
      name = "Forum",
      imagepost = false,
      premoderated = false,
      id = Section.Forum,
      votepoll = false,
      scrollMode = SectionScrollModeEnum.NO_SCROLL,
      topicsRestriction = POSTSCORE_UNRESTRICTED,
      imageAllowed = false
    )

  private val defaultGroup =
    Group(
      premoderated = false,
      pollPostAllowed = false,
      linksAllowed = false,
      sectionId = Section.Articles,
      defaultLinkText = "",
      urlName = "test",
      image = "",
      topicRestriction = POSTSCORE_UNRESTRICTED,
      commentsRestriction = POSTSCORE_UNRESTRICTED,
      id = 1,
      stat3 = 0,
      resolvable = false,
      title = "Test Group",
      info = "",
      longInfo = ""
    )

  private def makeUser(
      id: Int = 1,
      nick: String = "author",
      corrector: Boolean = false,
      moderator: Boolean = false,
      administrator: Boolean = false,
      blocked: Boolean = false,
      anonymous: Boolean = false,
      score: Int = 100): User =
    User(
      nick = nick,
      id = id,
      canmod = moderator,
      candel = administrator,
      anonymous = anonymous,
      corrector = corrector,
      blocked = blocked,
      password = "password",
      score = score,
      maxScore = score,
      photo = null,
      email = null,
      fullName = null,
      unreadEvents = 0,
      frozenUntil = null,
      activated = true
    )

  private def makeSession(
      user: User,
      corrector: Boolean = false,
      moderator: Boolean = false,
      administrator: Boolean = false): AuthorizedSession =
    AuthorizedSession(
      user = user,
      corrector = corrector,
      moderator = moderator,
      administrator = administrator,
      profile = Profile.DEFAULT,
      ipBlockInfo = IpBlockInfo("127.0.0.1"))

  private def makeTopic(
      authorUserId: Int = 1,
      sectionId: Int = Section.Articles,
      postscore: Int = POSTSCORE_UNRESTRICTED,
      commited: Boolean = false,
      sticky: Boolean = false,
      draft: Boolean = false,
      deleted: Boolean = false,
      expired: Boolean = false,
      postdate: Timestamp = Timestamp.from(Instant.now()),
      commitDate: Timestamp = null): Topic =
    Topic(
      id = 1,
      postscore = postscore,
      sticky = sticky,
      linktext = null,
      url = null,
      title = "title",
      authorUserId = authorUserId,
      groupId = 1,
      deleted = deleted,
      expired = expired,
      commitby =
        if commited then
          2
        else
          0
      ,
      postdate = postdate,
      commitDate = commitDate,
      groupUrl = "test",
      lastModified = Timestamp.from(Instant.now()),
      sectionId = sectionId,
      commentCount = 0,
      commited = commited,
      notop = false,
      userAgentId = 0,
      postIP = "127.0.0.1",
      resolved = false,
      minor = false,
      draft = draft,
      allowAnonymous = false,
      reactions = Reactions.empty,
      expireDate = null,
      openWarnings = 0
    )

  private def makePrepared(topic: Topic, section: Section, author: User): PreparedTopic =
    PreparedTopic(
      message = topic,
      author = author,
      deleteInfo = null,
      deleteUser = null,
      processedMessage = "",
      poll = null,
      commiter = null,
      tags = java.util.Collections.emptyList(),
      group = defaultGroup.copy(sectionId = section.id),
      section = section,
      markupType = MarkupType.Lorcode,
      image = null,
      postscoreInfo = "",
      remark = null,
      showRegisterInvite = false,
      userAgent = null,
      reactions = null,
      warnings = java.util.Collections.emptyList(),
      additionalImages = java.util.Collections.emptyList()
    )

  // === Bug 2: committed-Articles edit deadline anchored on commitDate ===

  test("committed Articles: author can edit while commitDate + 14d in future even if postdate is long ago"):
    val now = Instant.now()
    val postdate = Timestamp.from(now.minus(30, ChronoUnit.DAYS))
    val commitDate = Timestamp.from(now.minus(1, ChronoUnit.DAYS))
    val author = makeUser(id = 7)
    val topic = makeTopic(
      authorUserId = 7,
      sectionId = Section.Articles,
      commited = true,
      postdate = postdate,
      commitDate = commitDate)
    val prepared = makePrepared(topic, articlesSection, author)
    val session = makeSession(author)

    val result = EditTopicChecker.checkContentEdit(prepared)(using session)

    assert(result.permitted, s"expected permitted, got $result")

  test("committed Articles: author cannot edit once commitDate + 14d has elapsed"):
    val now = Instant.now()
    val postdate = Timestamp.from(now.minus(60, ChronoUnit.DAYS))
    val commitDate = Timestamp.from(now.minus(30, ChronoUnit.DAYS))
    val author = makeUser(id = 7)
    val topic = makeTopic(
      authorUserId = 7,
      sectionId = Section.Articles,
      commited = true,
      postdate = postdate,
      commitDate = commitDate)
    val prepared = makePrepared(topic, articlesSection, author)
    val session = makeSession(author)

    val result = EditTopicChecker.checkContentEdit(prepared)(using session)

    assert(result.restricted)
    assertEquals(result.reason, "истек срок редактирования топика")

  // === Postscore tightening (intentional new behavior) ===

  test("postscore tightening: corrector cannot edit NO_COMMENTS topic in premoderated section"):
    val someoneElse = makeUser(id = 99)
    val corrector = makeUser(id = 5, corrector = true)
    val topic = makeTopic(
      authorUserId = 99,
      sectionId = Section.Articles,
      postscore = POSTSCORE_NO_COMMENTS,
      commited = false)
    val prepared = makePrepared(topic, articlesSection, someoneElse)
    val session = makeSession(corrector, corrector = true)

    val result = EditTopicChecker.checkContentEdit(prepared)(using session)

    assert(result.restricted)
    assertEquals(result.reason, "нельзя править топики с выключенными комментариями")

  test("postscore tightening: author cannot edit own committed Article with NO_COMMENTS"):
    val author = makeUser(id = 7)
    val now = Instant.now()
    val topic = makeTopic(
      authorUserId = 7,
      sectionId = Section.Articles,
      postscore = POSTSCORE_NO_COMMENTS,
      commited = true,
      postdate = Timestamp.from(now.minus(30, ChronoUnit.DAYS)),
      commitDate = Timestamp.from(now.minus(1, ChronoUnit.DAYS))
    )
    val prepared = makePrepared(topic, articlesSection, author)
    val session = makeSession(author)

    val result = EditTopicChecker.checkContentEdit(prepared)(using session)

    assert(result.restricted)
    assertEquals(result.reason, "нельзя править топики с выключенными комментариями")

  // === Sanity: normal author edits ===

  test("author can edit own non-committed forum topic within 14d"):
    val author = makeUser(id = 7)
    val topic = makeTopic(
      authorUserId = 7,
      sectionId = Section.Forum,
      commited = false,
      postdate = Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)))
    val prepared = makePrepared(topic, forumSection, author)
    val session = makeSession(author)

    val result = EditTopicChecker.checkContentEdit(prepared)(using session)

    assert(result.permitted, s"expected permitted, got $result")

  test("non-author cannot edit another user's topic"):
    val author = makeUser(id = 7)
    val other = makeUser(id = 8)
    val topic = makeTopic(
      authorUserId = 7,
      sectionId = Section.Forum,
      commited = false,
      postdate = Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)))
    val prepared = makePrepared(topic, forumSection, author)
    val session = makeSession(other)

    val result = EditTopicChecker.checkContentEdit(prepared)(using session)

    assert(result.restricted)
    assertEquals(result.reason, "нельзя править чужие топики")
