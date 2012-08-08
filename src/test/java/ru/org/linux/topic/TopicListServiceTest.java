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

package ru.org.linux.topic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.org.linux.group.Group;
import ru.org.linux.section.Section;
import ru.org.linux.tag.TagNotFoundException;
import ru.org.linux.tag.TagService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("unit-tests-context.xml")
public class TopicListServiceTest {
  @Autowired
  TopicListService topicListService;

  @Autowired
  TopicListDto topicListDto;

  @Autowired
  TagService tagService;

  Section section1 = new Section("testSection", false, true, 11, false, "NO_SCROLL", TopicPermissionService.POSTSCORE_UNRESTRICTED);
  Section section2 = new Section("testSection 2", false, false, 12, false, "NO_SCROLL", TopicPermissionService.POSTSCORE_UNRESTRICTED);
  Group group = new Group(false, false, false, 11,
    "", "", "", 1, 1,
    111, 1, 1, false);

  @Test
  public void getTopicsFeedYear_commonTest()
    throws UserErrorException, TagNotFoundException {
    when(tagService.getTagId("LOR")).thenReturn(123);

    List<Topic> topicList = topicListService.getTopicsFeed(
      section1, group, "LOR", null, 2000, 11
    );

    assertEquals(1, topicListDto.getSections().size());
    Integer sectionId = topicListDto.getSections().iterator().next();
    assertEquals(Integer.valueOf(11), sectionId);

    assertEquals(111, topicListDto.getGroup());
    assertEquals(123, topicListDto.getTag());

    assertEquals(TopicListDto.DateLimitType.BETWEEN, topicListDto.getDateLimitType());
    assertEquals(TopicListDao.CommitMode.COMMITED_ONLY, topicListDto.getCommitMode());
  }

  @Test
  public void getTopicsFeedYear_monthAndYearTest()
    throws UserErrorException, TagNotFoundException {

    List<Topic> topicList = topicListService.getTopicsFeed(
      section1, group, "LOR", null, 2000, 11
    );

    assertEquals(TopicListDto.DateLimitType.BETWEEN, topicListDto.getDateLimitType());
    Calendar calendar = Calendar.getInstance();
    calendar.set(2000, 10, 1, 0, 0, 0);
    assertEquals(calendar.getTime().getTime() / 1000, topicListDto.getFromDate().getTime() / 1000);
    calendar.set(2000, 11, 1, 0, 0, 0);
    assertEquals(calendar.getTime().getTime() / 1000, topicListDto.getToDate().getTime() / 1000);
    assertNull(topicListDto.getLimit());

    topicList = topicListService.getTopicsFeed(
      section2, null, null, 0, null, null
    );
    assertEquals(TopicListDto.DateLimitType.MONTH_AGO, topicListDto.getDateLimitType());
    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -6);
    assertEquals(calendar.getTime().getTime() / 1000, topicListDto.getFromDate().getTime() / 1000);
    assertNull(topicListDto.getToDate());
    assertEquals(Integer.valueOf(20), topicListDto.getLimit());
    assertNull(topicListDto.getOffset());
  }

  @Test
  public void getUserTopicsFeedTest() {

    User user = mock(User.class);
    when(user.getId()).thenReturn(12345);

    List<Topic> topicList = topicListService.getUserTopicsFeed(
      user, 123, true, false
    );

    assertEquals(Integer.valueOf(20), topicListDto.getLimit());
    assertEquals(Integer.valueOf(123), topicListDto.getOffset());
    assertEquals(TopicListDao.CommitMode.ALL, topicListDto.getCommitMode());
    assertEquals(12345, topicListDto.getUserId());
    assertTrue(topicListDto.isUserFavs());
  }

  @Test
  public void getUserTopicsFeedWithSectionAndGroupTest() {

    User user = mock(User.class);
    when(user.getId()).thenReturn(12345);

    List<Topic> topicList = topicListService.getUserTopicsFeed(
        user, section1, group, 123, true, false
    );

    assertEquals(1, topicListDto.getSections().size());
    Integer sectionId = topicListDto.getSections().iterator().next();
    assertEquals(Integer.valueOf(11), sectionId);

    assertEquals(111, topicListDto.getGroup());

    assertEquals(Integer.valueOf(20), topicListDto.getLimit());
    assertEquals(Integer.valueOf(123), topicListDto.getOffset());
    assertEquals(TopicListDao.CommitMode.ALL, topicListDto.getCommitMode());
    assertEquals(12345, topicListDto.getUserId());
    assertTrue(topicListDto.isUserFavs());
  }

  @Test
  public void getRssTopicsFeedTest() {

    List<Topic> topicList = topicListService.getRssTopicsFeed(
      section1, group, new Date(), false, true, false
    );

    assertEquals(1, topicListDto.getSections().size());
    Integer sectionId = topicListDto.getSections().iterator().next();
    assertEquals(Integer.valueOf(11), sectionId);

    assertEquals(111, topicListDto.getGroup());
    assertEquals(TopicListDto.DateLimitType.MONTH_AGO, topicListDto.getDateLimitType());
    assertEquals(new Date().getTime() / 1000, topicListDto.getFromDate().getTime() / 1000);
    assertFalse(topicListDto.isNotalks());
    assertTrue(topicListDto.isTech());
    assertEquals(Integer.valueOf(20), topicListDto.getLimit());
    assertEquals(TopicListDao.CommitMode.COMMITED_ONLY, topicListDto.getCommitMode());


    topicList = topicListService.getRssTopicsFeed(
      section2, group, new Date(), false, true, false
    );
    assertEquals(TopicListDao.CommitMode.POSTMODERATED_ONLY, topicListDto.getCommitMode());
  }

  @Test
  public void getAllTopicsFeedTest() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2000, 10, 1, 0, 0, 0);
    List<Topic> topicList = topicListService.getAllTopicsFeed(
      section1, calendar.getTime()
    );

    assertEquals(TopicListDao.CommitMode.UNCOMMITED_ONLY, topicListDto.getCommitMode());

    assertEquals(1, topicListDto.getSections().size());
    Integer sectionId = topicListDto.getSections().iterator().next();
    assertEquals(Integer.valueOf(11), sectionId);

    assertEquals(TopicListDto.DateLimitType.MONTH_AGO, topicListDto.getDateLimitType());

    assertEquals(calendar.getTime().getTime() / 1000, topicListDto.getFromDate().getTime() / 1000);
  }

  @Test
  public void getMainPageFeedTest() {
    List<Topic> topicList = topicListService.getMainPageFeed(false);

    assertEquals(1, topicListDto.getSections().size());
    Integer sectionId = topicListDto.getSections().iterator().next();
    assertEquals(Integer.valueOf(Section.SECTION_NEWS), sectionId);

    assertEquals(Integer.valueOf(20), topicListDto.getLimit());
    assertEquals(TopicListDto.DateLimitType.MONTH_AGO, topicListDto.getDateLimitType());

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -1);
    assertEquals(calendar.getTime().getTime() / 1000, topicListDto.getFromDate().getTime() / 1000);

    assertEquals(TopicListDao.CommitMode.COMMITED_ONLY, topicListDto.getCommitMode());

    topicList = topicListService.getMainPageFeed(true);

    assertEquals(2, topicListDto.getSections().size());
  }

}
