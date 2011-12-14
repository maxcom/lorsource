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
package ru.org.linux.admin.ipmanage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"unit-test-context.xml"})
public class SameIpServiceTest {
  @Autowired
  SameIpService sameIpService;

  @Autowired
  MockDaoFactory mockDaoFactory;

  @Test
  public void getBlockIpInfoTest() {
    IpBlockDao ipBlockDao = mockDaoFactory.getIpBlockDao();
    IpBlockInfo ipBlockInfo = new IpBlockInfo();
    ipBlockInfo.setReason("test reason");
    ipBlockInfo.setBanDate(new Timestamp(0));
    ipBlockInfo.setModerator(1);
    ipBlockInfo.setOriginalDate(new Timestamp(10000000));
    when(ipBlockDao.getBlockInfo("127.0.0.1")).thenReturn(ipBlockInfo);

    UserDao userDao = mockDaoFactory.getUserDao();
    User user = new User();
    user.setNick("super_moder");
    try {
      when(userDao.getUserCached(1)).thenReturn(user);
    } catch (Exception ignored) {
    }


    SameIp.BlockInfo blockInfo = sameIpService.getBlockInfo("127.0.0.1");

    Assert.assertEquals("super_moder", blockInfo.getModeratorNick());
    Assert.assertEquals(new Timestamp(0), blockInfo.getBanDate());
    Assert.assertEquals(new Timestamp(10000000), blockInfo.getOriginalDate());
    Assert.assertEquals("test reason", blockInfo.getReason());
    Assert.assertTrue(blockInfo.isBlocked());
    Assert.assertTrue(blockInfo.isBlockExpired());
  }

  @Test
  public void getForumMessagesTest() {
    SameIpDao sameIpDao = mockDaoFactory.getSameIpDao();
    List<SameIpDto.TopicItem> topicItemsDto = new ArrayList<SameIpDto.TopicItem>();
    SameIpDto.TopicItem topicItem = new SameIpDto.TopicItem();
    topicItem.setTopicId(12345);
    topicItem.setCommentId(67890);
    topicItem.setDeleted(true);
    topicItem.setGtitle("1111");
    topicItem.setPtitle("2222");
    topicItem.setTitle("333");
    topicItem.setPostdate(new Timestamp(10000000));
    topicItemsDto.add(topicItem);
    when(sameIpDao.getComments("127.0.0.1")).thenReturn(topicItemsDto);

    List<SameIpDto.TopicItem> topicItemsDto2 = new ArrayList<SameIpDto.TopicItem>();
    topicItem = new SameIpDto.TopicItem();
    topicItem.setTopicId(54321);
    topicItem.setCommentId(0);
    topicItem.setDeleted(false);
    topicItem.setGtitle("9999");
    topicItem.setPtitle("8888");
    topicItem.setTitle("7777");
    topicItem.setPostdate(new Timestamp(9999999));
    topicItemsDto2.add(topicItem);
    when(sameIpDao.getTopics("127.0.0.1")).thenReturn(topicItemsDto2);

    List<SameIp.TopicItem> topicItemList = sameIpService.getForumMessages("127.0.0.1", true);

    Assert.assertEquals(1, topicItemList.size());
    SameIp.TopicItem topicItem1 = topicItemList.get(0);
    Assert.assertEquals(12345, topicItem1.getTopicId());
    Assert.assertEquals(67890, topicItem1.getCommentId());
    Assert.assertTrue(topicItem1.isDeleted());
    Assert.assertEquals("1111", topicItem1.getGtitle());
    Assert.assertEquals("2222", topicItem1.getPtitle());
    Assert.assertEquals("333", topicItem1.getTitle());
    Assert.assertEquals(new Timestamp(10000000), topicItem1.getPostdate());

    topicItemList = sameIpService.getForumMessages("127.0.0.1", false);
    Assert.assertEquals(1, topicItemList.size());
    topicItem1 = topicItemList.get(0);
    Assert.assertEquals(54321, topicItem1.getTopicId());
    Assert.assertEquals(0, topicItem1.getCommentId());
    Assert.assertFalse(topicItem1.isDeleted());
    Assert.assertEquals("9999", topicItem1.getGtitle());
    Assert.assertEquals("8888", topicItem1.getPtitle());
    Assert.assertEquals("7777", topicItem1.getTitle());
    Assert.assertEquals(new Timestamp(9999999), topicItem1.getPostdate());
  }

  @Test
  public void getUsersTest() {
    SameIpDao sameIpDao = mockDaoFactory.getSameIpDao();
    List<SameIpDto.UserItem> userItemDtoList = new ArrayList<SameIpDto.UserItem>();
    SameIpDto.UserItem userItemDto = new SameIpDto.UserItem();

    userItemDto.setLastdate(new Timestamp(9999999));
    userItemDto.setNick("test_user");
    userItemDto.setUaId(123456);
    userItemDto.setUserAgent("Some Use Agent String");
    userItemDtoList.add(userItemDto);
    when(sameIpDao.getUsers("127.0.0.1")).thenReturn(userItemDtoList);

    List<SameIp.UserItem> userItemList = sameIpService.getUsers("127.0.0.1", 123457);
    Assert.assertEquals(1, userItemList.size());

    SameIp.UserItem userItem = userItemList.get(0);

    Assert.assertEquals(new Timestamp(9999999), userItem.getLastdate());
    Assert.assertEquals("test_user", userItem.getNick());
    Assert.assertEquals("Some Use Agent String", userItem.getUserAgent());
    Assert.assertFalse(userItem.isSameUa());

    userItemList = sameIpService.getUsers("127.0.0.1", 123456);
    Assert.assertEquals(1, userItemList.size());

    userItem = userItemList.get(0);

    Assert.assertEquals(new Timestamp(9999999), userItem.getLastdate());
    Assert.assertEquals("test_user", userItem.getNick());
    Assert.assertEquals("Some Use Agent String", userItem.getUserAgent());
    Assert.assertTrue(userItem.isSameUa());

  }

}
