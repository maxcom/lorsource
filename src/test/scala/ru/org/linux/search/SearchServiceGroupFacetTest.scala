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
package ru.org.linux.search

import munit.FunSuite
import org.mockito.Mockito.{mock, when}
import org.opensearch.client.opensearch._types.aggregations.{Aggregate, StringTermsAggregate, StringTermsBucket}
import ru.org.linux.group.{Group, GroupService}
import ru.org.linux.section.{Section, SectionScrollModeEnum, SectionService}

import scala.jdk.CollectionConverters.*

class SearchServiceGroupFacetTest extends FunSuite:
  private val DeadGroup = "linux-install"

  private def makeSection: Section =
    Section(
      name = "forum",
      imagepost = false,
      premoderated = true,
      id = Section.Forum,
      votepoll = false,
      scrollMode = SectionScrollModeEnum.SECTION,
      topicsRestriction = 0,
      imageAllowed = true
    )

  private def makeGroup(urlName: String, title: String): Group =
    Group(
      premoderated = false,
      pollPostAllowed = false,
      linksAllowed = false,
      sectionId = Section.Forum,
      defaultLinkText = "",
      urlName = urlName,
      image = "",
      topicRestriction = 0,
      commentsRestriction = 0,
      id = 1,
      stat3 = 0,
      resolvable = false,
      title = title,
      info = "",
      longInfo = ""
    )

  private def makeServices: (Section, SectionService, GroupService) =
    val section = makeSection
    val sectionService = mock(classOf[SectionService])
    when(sectionService.getSectionByName("forum")).thenReturn(section)
    val groupService = mock(classOf[GroupService])
    (section, sectionService, groupService)

  private def makeService(sectionService: SectionService, groupService: GroupService): SearchService =
    new SearchService(null, null, null, sectionService, groupService)

  private def groupBucket(name: String, count: Long): StringTermsBucket =
    StringTermsBucket.of(b => b.key(name).docCount(count))

  private def sectionBucket(sectionName: String, docCount: Long, groups: StringTermsBucket*): StringTermsBucket =
    val groupsAgg: Aggregate = StringTermsAggregate.of(a => a.buckets(bs => bs.array(groups.asJava))).toAggregate()

    StringTermsBucket.of(b => b.key(sectionName).docCount(docCount).aggregations("groups", groupsAgg))

  test("group facet skips facet groups that no longer exist in the database"):
    val (section, sectionService, groupService) = makeServices
    when(groupService.getGroupOpt(section, "general", false)).thenReturn(Some(makeGroup("general", "General")))
    when(groupService.getGroupOpt(section, "talks", false)).thenReturn(Some(makeGroup("talks", "Talks")))
    when(groupService.getGroupOpt(section, DeadGroup, false)).thenReturn(None)

    val bucket = sectionBucket(
      "forum",
      13,
      groupBucket("general", 6),
      groupBucket("talks", 4),
      groupBucket(DeadGroup, 3))

    val facet = makeService(sectionService, groupService).buildGroupFacet(Some(bucket), None)

    assertEquals(facet.map(_.map(_.key)), Some(Seq("", "general", "talks")))
    assertEquals(facet.map(_.map(_.label)), Some(Seq("все (13)", "general (6)", "talks (4)")))

  test("group facet ignores selected group that no longer exists"):
    val (section, sectionService, groupService) = makeServices
    when(groupService.getGroupOpt(section, DeadGroup, false)).thenReturn(None)

    val facet = makeService(sectionService, groupService).buildGroupFacet(None, Some("forum" -> DeadGroup))

    assertEquals(facet.map(_.map(_.key)), Some(Seq("")))

  test("group facet ignores dead selected group when it is also present in facet buckets"):
    val (section, sectionService, groupService) = makeServices
    when(groupService.getGroupOpt(section, "general", false)).thenReturn(Some(makeGroup("general", "General")))
    when(groupService.getGroupOpt(section, DeadGroup, false)).thenReturn(None)

    val bucket = sectionBucket("forum", 13, groupBucket("general", 10), groupBucket(DeadGroup, 3))

    val facet = makeService(sectionService, groupService).buildGroupFacet(Some(bucket), Some("forum" -> DeadGroup))

    assertEquals(facet.map(_.map(_.key)), Some(Seq("", "general")))
