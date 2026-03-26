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
package ru.org.linux.section

import munit.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.{ContextConfiguration, TestContextManager}

@ContextConfiguration(Array("integration-tests-context.xml"))
class SectionDaoIntegrationTest extends FunSuite:
  new TestContextManager(this.getClass).prepareTestInstance(this)

  @Autowired
  var sectionDao: SectionDao = scala.compiletime.uninitialized

  private def getSectionById(sections: Seq[Section], id: Int) = sections.find(_.id == id).orNull

  test("sectionsScrollModeTest"):
    val sectionList = sectionDao.getAllSections

    var section = getSectionById(sectionList, 1)
    assert(section != null)
    assertEquals(SectionScrollModeEnum.SECTION, section.scrollMode)

    section = getSectionById(sectionList, 2)
    assert(section != null)
    assertEquals(SectionScrollModeEnum.GROUP, section.scrollMode)

    section = getSectionById(sectionList, 3)
    assert(section != null)
    assertEquals(SectionScrollModeEnum.SECTION, section.scrollMode)

    section = getSectionById(sectionList, 5)
    assert(section != null)
    assertEquals(SectionScrollModeEnum.SECTION, section.scrollMode)

  test("getNewsViewerLinkTest"):
    val sectionList = sectionDao.getAllSections

    val polls = getSectionById(sectionList, Section.Polls)
    assert(polls != null)
    assertEquals("/polls/", polls.getNewsViewerLink)

    val forum = getSectionById(sectionList, Section.Forum)
    assert(forum != null)
    assertEquals("/forum/lenta/", forum.getNewsViewerLink)

  test("getUrlNameTest"):
    assertEquals("news", Section.getUrlName(Section.News))
    assertEquals("forum", Section.getUrlName(Section.Forum))
    assertEquals("gallery", Section.getUrlName(Section.Gallery))
    assertEquals("polls", Section.getUrlName(Section.Polls))
    assertEquals("articles", Section.getUrlName(Section.Articles))

  test("getArchiveLinkTest"):
    val sectionList = sectionDao.getAllSections

    val forum = getSectionById(sectionList, Section.Forum)
    assert(forum != null)
    assertEquals(null, forum.getArchiveLink)

    val gallery = getSectionById(sectionList, Section.Gallery)
    assert(gallery != null)
    assertEquals("/gallery/archive/", gallery.getArchiveLink)

    val polls = getSectionById(sectionList, Section.Polls)
    assert(polls != null)
    assertEquals("/polls/archive/", polls.getArchiveLink)

    val news = getSectionById(sectionList, Section.News)
    assert(news != null)
    assertEquals("/news/archive/", news.getArchiveLink)

    val articles = getSectionById(sectionList, Section.Articles)
    assert(articles != null)
    assertEquals("/articles/archive/", articles.getArchiveLink)

  test("getArchiveLinkWithYearMonthTest"):
    val sectionList = sectionDao.getAllSections

    val gallery = getSectionById(sectionList, Section.Gallery)
    assert(gallery != null)
    assertEquals("/gallery/archive/2024/5/", gallery.getArchiveLink(2024, 5))
