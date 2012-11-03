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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;
import ru.org.linux.group.Group;
import ru.org.linux.section.Section;
import ru.org.linux.tag.TagNotFoundException;
import ru.org.linux.tag.TagService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ContextConfiguration("unit-tests-context.xml")
public class TopicListServiceTest extends AbstractTestNGSpringContextTests {
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

    Assert.assertEquals(1, topicListDto.getSections().size());
    Integer sectionId = topicListDto.getSections().iterator().next();
    Assert.assertEquals(Integer.valueOf(11), sectionId);

    Assert.assertEquals(111, topicListDto.getGroup());
    Assert.assertEquals(123, topicListDto.getTag());

    Assert.assertEquals(TopicListDto.DateLimitType.BETWEEN, topicListDto.getDateLimitType());
    Assert.assertEquals(TopicListDao.CommitMode.COMMITED_ONLY, topicListDto.getCommitMode());
  }

  @Test
  public void getTopicsFeedYear_monthAndYearTest()
    throws UserErrorException, TagNotFoundException {

    List<Topic> topicList = topicListService.getTopicsFeed(
      section1, group, "LOR", null, 2000, 11
    );

    Assert.assertEquals(TopicListDto.DateLimitType.BETWEEN, topicListDto.getDateLimitType());
    Calendar calendar = Calendar.getInstance();
    calendar.set(2000, 10, 1, 0, 0, 0);
    Assert.assertEquals(calendar.getTime().getTime() / 1000, topicListDto.getFromDate().getTime() / 1000);
    calendar.set(2000, 11, 1, 0, 0, 0);
    Assert.assertEquals(calendar.getTime().getTime() / 1000, topicListDto.getToDate().getTime() / 1000);
    Assert.assertNull(topicListDto.getLimit());

    topicList = topicListService.getTopicsFeed(
      section2, null, null, 0, null, null
    );
    Assert.assertEquals(TopicListDto.DateLimitType.MONTH_AGO, topicListDto.getDateLimitType());
    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -6);
    Assert.assertEquals(calendar.getTime().getTime() / 1000, topicListDto.getFromDate().getTime() / 1000);
    Assert.assertNull(topicListDto.getToDate());
    Assert.assertEquals(Integer.valueOf(20), topicListDto.getLimit());
    Assert.assertNull(topicListDto.getOffset());
  }

  @Test
  public void getUserTopicsFeedTest() {

    User user = mock(User.class);
    when(user.getId()).thenReturn(12345);

    List<Topic> topicList = topicListService.getUserTopicsFeed(
      user, 123, true, false
    );

    Assert.assertEquals(Integer.valueOf(20), topicListDto.getLimit());
    Assert.assertEquals(Integer.valueOf(123), topicListDto.getOffset());
    Assert.assertEquals(TopicListDao.CommitMode.ALL, topicListDto.getCommitMode());
    Assert.assertEquals(12345, topicListDto.getUserId());
    Assert.assertTrue(topicListDto.isUserFavs());
  }

  @Test
  public void getUserTopicsFeedWithSectionAndGroupTest() {

    User user = mock(User.class);
    when(user.getId()).thenReturn(12345);

    List<Topic> topicList = topicListService.getUserTopicsFeed(
        user, section1, group, 123, true, false
    );

    Assert.assertEquals(1, topicListDto.getSections().size());
    Integer sectionId = topicListDto.getSections().iterator().next();
    Assert.assertEquals(Integer.valueOf(11), sectionId);

    Assert.assertEquals(111, topicListDto.getGroup());

    Assert.assertEquals(Integer.valueOf(20), topicListDto.getLimit());
    Assert.assertEquals(Integer.valueOf(123), topicListDto.getOffset());
    Assert.assertEquals(TopicListDao.CommitMode.ALL, topicListDto.getCommitMode());
    Assert.assertEquals(12345, topicListDto.getUserId());
    Assert.assertTrue(topicListDto.isUserFavs());
  }

  @Test
  public void getRssTopicsFeedTest() {

    List<Topic> topicList = topicListService.getRssTopicsFeed(
      section1, group, new Date(), false, true, false
    );

    Assert.assertEquals(1, topicListDto.getSections().size());
    Integer sectionId = topicListDto.getSections().iterator().next();
    Assert.assertEquals(Integer.valueOf(11), sectionId);

    Assert.assertEquals(111, topicListDto.getGroup());
    Assert.assertEquals(TopicListDto.DateLimitType.MONTH_AGO, topicListDto.getDateLimitType());
    Assert.assertEquals(new Date().getTime() / 1000, topicListDto.getFromDate().getTime() / 1000);
    Assert.assertFalse(topicListDto.isNotalks());
    Assert.assertTrue(topicListDto.isTech());
    Assert.assertEquals(Integer.valueOf(20), topicListDto.getLimit());
    Assert.assertEquals(TopicListDao.CommitMode.COMMITED_ONLY, topicListDto.getCommitMode());


    topicList = topicListService.getRssTopicsFeed(
      section2, group, new Date(), false, true, false
    );
    Assert.assertEquals(TopicListDao.CommitMode.POSTMODERATED_ONLY, topicListDto.getCommitMode());
  }

  @Test
  public void getAllTopicsFeedTest() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2000, 10, 1, 0, 0, 0);
    List<Topic> topicList = topicListService.getAllTopicsFeed(
      section1, calendar.getTime()
    );

    Assert.assertEquals(TopicListDao.CommitMode.UNCOMMITED_ONLY, topicListDto.getCommitMode());

    Assert.assertEquals(1, topicListDto.getSections().size());
    Integer sectionId = topicListDto.getSections().iterator().next();
    Assert.assertEquals(Integer.valueOf(11), sectionId);

    Assert.assertEquals(TopicListDto.DateLimitType.MONTH_AGO, topicListDto.getDateLimitType());

    Assert.assertEquals(calendar.getTime().getTime() / 1000, topicListDto.getFromDate().getTime() / 1000);
  }

  @Test
  public void getMainPageFeedTest() {
    List<Topic> topicList = topicListService.getMainPageFeed(false);

    Assert.assertEquals(1, topicListDto.getSections().size());
    Integer sectionId = topicListDto.getSections().iterator().next();
    Assert.assertEquals(Integer.valueOf(Section.SECTION_NEWS), sectionId);

    Assert.assertEquals(Integer.valueOf(20), topicListDto.getLimit());
    Assert.assertEquals(TopicListDto.DateLimitType.MONTH_AGO, topicListDto.getDateLimitType());

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -1);
    Assert.assertEquals(calendar.getTime().getTime() / 1000, topicListDto.getFromDate().getTime() / 1000);

    Assert.assertEquals(TopicListDao.CommitMode.COMMITED_ONLY, topicListDto.getCommitMode());

    topicList = topicListService.getMainPageFeed(true);

    Assert.assertEquals(2, topicListDto.getSections().size());
  }

}
