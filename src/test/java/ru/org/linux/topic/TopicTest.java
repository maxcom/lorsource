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

package ru.org.linux.topic;

import org.junit.Test;
import junit.framework.Assert;
import ru.org.linux.section.Section;
import ru.org.linux.user.User;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 8/30/11
 * Time: 4:27 PM
 */
public class TopicTest {
  private final TopicPermissionService permissionService = new TopicPermissionService();

  /**
   * Проверка что пользователь МОЖЕТ удалить топик автором которого он является
   * и прошло меньше часа с момента почтинга
   * @throws Exception
   */
  @Test
  public void isDeletableByUserTest1() throws Exception {
    Calendar calendar = Calendar.getInstance();

    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getInt("postscore")).thenReturn(-9999);
    when(resultSet.wasNull()).thenReturn(false);
    calendar.setTime(new Date());
    calendar.add(Calendar.MINUTE, -10);
    // commitdate, lastmod
    when(resultSet.getTimestamp(anyString())).thenReturn(new Timestamp(calendar.getTimeInMillis()));
    // commitby, sectionid, stat1, ua_id,
    when(resultSet.getInt(anyString())).thenReturn(13);
    // gtitle, urlname, message, postip,
    when(resultSet.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, havelink, bbcode, resolved, minor
    when(resultSet.getBoolean(anyString())).thenReturn(false);

    when(resultSet.getInt("section")).thenReturn(3); // Галлерея
    when(resultSet.getBoolean("moderate")).thenReturn(true);

    User user = mock(User.class);
    when(user.isModerator()).thenReturn(false);
    when(user.getId()).thenReturn(13);

    Topic message = new Topic(resultSet);

    Assert.assertEquals(false, user.isModerator());
    Assert.assertTrue(user.getId() == resultSet.getInt("userid"));
    Assert.assertTrue(user.getId() == message.getUid());

    Assert.assertTrue(permissionService.isDeletableByUser(message, user));
  }
  /**
   * Проверка что пользователь НЕМОЖЕТ удалить топик автором которого он является
   * и прошло больше часа с момента почтинга
   * @throws Exception
   */
  @Test
  public void isDeletableByUserTest2() throws Exception {
    Calendar calendar = Calendar.getInstance();

    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getInt("postscore")).thenReturn(-9999);
    when(resultSet.wasNull()).thenReturn(false);
    calendar.setTime(new Date());
    calendar.add(Calendar.MINUTE, -70);
    // commitdate, lastmod
    when(resultSet.getTimestamp(anyString())).thenReturn(new Timestamp(calendar.getTimeInMillis()));
    // commitby, sectionid, stat1, ua_id,
    when(resultSet.getInt(anyString())).thenReturn(13);
    // gtitle, urlname, message, postip,
    when(resultSet.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, havelink, bbcode, resolved, minor
    when(resultSet.getBoolean(anyString())).thenReturn(false);

    when(resultSet.getInt("section")).thenReturn(3); // Галлерея
    when(resultSet.getBoolean("moderate")).thenReturn(true);

    User user = mock(User.class);
    when(user.isModerator()).thenReturn(false);
    when(user.getId()).thenReturn(13);

    Topic message = new Topic(resultSet);

    Assert.assertEquals(false, user.isModerator());
    Assert.assertTrue(user.getId() == resultSet.getInt("userid"));
    Assert.assertTrue(user.getId() == message.getUid());

    Assert.assertFalse(permissionService.isDeletableByUser(message, user));
  }

  /**
   * Проверка что пользователь НЕМОЖЕТ удалить топик автором которого он неявляется
   * и прошло больше часа с момента постинга
   * @throws Exception
   */
  @Test
  public void isDeletableByUserTest3() throws Exception {
    Calendar calendar = Calendar.getInstance();

    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getInt("postscore")).thenReturn(-9999);
    when(resultSet.wasNull()).thenReturn(false);
    calendar.setTime(new Date());
    calendar.add(Calendar.MINUTE, -70);
    // commitdate, lastmod
    when(resultSet.getTimestamp(anyString())).thenReturn(new Timestamp(calendar.getTimeInMillis()));
    // commitby, sectionid, stat1, ua_id,
    when(resultSet.getInt(anyString())).thenReturn(13);
    // gtitle, urlname, message, postip,
    when(resultSet.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, havelink, bbcode, resolved, minor
    when(resultSet.getBoolean(anyString())).thenReturn(false);

    when(resultSet.getInt("section")).thenReturn(3); // Галлерея
    when(resultSet.getBoolean("moderate")).thenReturn(true);

    User user = mock(User.class);
    when(user.isModerator()).thenReturn(false);
    when(user.getId()).thenReturn(14);

    Topic message = new Topic(resultSet);

    Assert.assertEquals(false, user.isModerator());
    Assert.assertFalse(user.getId() == resultSet.getInt("userid"));
    Assert.assertFalse(user.getId() == message.getUid());

    Assert.assertFalse(permissionService.isDeletableByUser(message, user));
  }

  /**
   * Проверка что пользователь НЕМОЖЕТ удалить топик автором которого он неявляется
   * и прошло больше часа с момента постинга
   * @throws Exception
   */
  @Test
  public void isDeletableByUserTest4() throws Exception {
    Calendar calendar = Calendar.getInstance();

    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getInt("postscore")).thenReturn(-9999);
    when(resultSet.wasNull()).thenReturn(false);
    calendar.setTime(new Date());
    calendar.add(Calendar.MINUTE, -5);
    // commitdate, lastmod
    when(resultSet.getTimestamp(anyString())).thenReturn(new Timestamp(calendar.getTimeInMillis()));
    // commitby, sectionid, stat1, ua_id,
    when(resultSet.getInt(anyString())).thenReturn(13);
    // gtitle, urlname, message, postip,
    when(resultSet.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, havelink, bbcode, resolved, minor
    when(resultSet.getBoolean(anyString())).thenReturn(false);

    when(resultSet.getInt("section")).thenReturn(3); // Галлерея
    when(resultSet.getBoolean("moderate")).thenReturn(true);

    User user = mock(User.class);
    when(user.isModerator()).thenReturn(false);
    when(user.getId()).thenReturn(14);

    Topic message = new Topic(resultSet);

    Assert.assertEquals(false, user.isModerator());
    Assert.assertFalse(user.getId() == resultSet.getInt("userid"));
    Assert.assertFalse(user.getId() == message.getUid());

    Assert.assertFalse(permissionService.isDeletableByUser(message, user));
  }

  /**
   * Проверка для модератора
   * @throws Exception
   */
  @Test
  public void isDeletableByModeratorTest() throws Exception {

    Calendar calendar = Calendar.getInstance();

    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -2);
    long oldTime = calendar.getTimeInMillis();

    calendar.setTime(new Date());
    calendar.add(Calendar.DAY_OF_MONTH, -2);
    long newTime = calendar.getTimeInMillis();


    ResultSet resultSetModerateOld = mock(ResultSet.class);
    when(resultSetModerateOld.getInt("postscore")).thenReturn(-9999);
    when(resultSetModerateOld.wasNull()).thenReturn(false);
    // commitdate, lastmod
    when(resultSetModerateOld.getTimestamp(anyString())).thenReturn(new Timestamp(oldTime));
    // commitby, sectionid, stat1, ua_id,
    when(resultSetModerateOld.getInt(anyString())).thenReturn(13);
    // gtitle, urlname, message, postip,
    when(resultSetModerateOld.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, havelink, bbcode, resolved, minor
    when(resultSetModerateOld.getBoolean(anyString())).thenReturn(false);
    when(resultSetModerateOld.getBoolean("moderate")).thenReturn(true);
    when(resultSetModerateOld.getTimestamp("postdate")).thenReturn(new Timestamp(oldTime));


    ResultSet resultSetNotModerateOld = mock(ResultSet.class);
    when(resultSetNotModerateOld.getInt("postscore")).thenReturn(-9999);
    when(resultSetNotModerateOld.wasNull()).thenReturn(false);
    // commitdate, lastmod
    when(resultSetNotModerateOld.getTimestamp(anyString())).thenReturn(new Timestamp(oldTime));
    // commitby, sectionid, stat1, ua_id,
    when(resultSetNotModerateOld.getInt(anyString())).thenReturn(13);
    // gtitle, urlname, message, postip,
    when(resultSetNotModerateOld.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, havelink, bbcode, resolved, minor
    when(resultSetNotModerateOld.getBoolean(anyString())).thenReturn(false);
    when(resultSetNotModerateOld.getBoolean("moderate")).thenReturn(false);
    when(resultSetNotModerateOld.getTimestamp("postdate")).thenReturn(new Timestamp(oldTime));


    ResultSet resultSetModerateNew = mock(ResultSet.class);
    when(resultSetModerateNew.getInt("postscore")).thenReturn(-9999);
    when(resultSetModerateNew.wasNull()).thenReturn(false);
    // commitdate, lastmod
    when(resultSetModerateNew.getTimestamp(anyString())).thenReturn(new Timestamp(newTime));
    // commitby, sectionid, stat1, ua_id,
    when(resultSetModerateNew.getInt(anyString())).thenReturn(13);
    // gtitle, urlname, message, postip,
    when(resultSetModerateNew.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, havelink, bbcode, resolved, minor
    when(resultSetModerateNew.getBoolean(anyString())).thenReturn(false);
    when(resultSetModerateNew.getBoolean("moderate")).thenReturn(true);
    when(resultSetModerateNew.getTimestamp("postdate")).thenReturn(new Timestamp(newTime));


    ResultSet resultSetNotModerateNew = mock(ResultSet.class);
    when(resultSetNotModerateNew.getInt("postscore")).thenReturn(-9999);
    when(resultSetNotModerateNew.wasNull()).thenReturn(false);
    // commitdate, lastmod
    when(resultSetNotModerateNew.getTimestamp(anyString())).thenReturn(new Timestamp(newTime));
    // commitby, sectionid, stat1, ua_id,
    when(resultSetNotModerateNew.getInt(anyString())).thenReturn(13);
    // gtitle, urlname, message, postip,
    when(resultSetNotModerateNew.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, havelink, bbcode, resolved, minor
    when(resultSetNotModerateNew.getBoolean(anyString())).thenReturn(false);
    when(resultSetNotModerateNew.getBoolean("moderate")).thenReturn(false);
    when(resultSetNotModerateNew.getTimestamp("postdate")).thenReturn(new Timestamp(newTime));


    User user = mock(User.class);
    when(user.isModerator()).thenReturn(true);
    when(user.getId()).thenReturn(13);

    // проверка что данные в mock user верные
    Assert.assertEquals(true, user.isModerator());

    Section sectionModerate = mock(Section.class);
    when(sectionModerate.isPremoderated()).thenReturn(true);
    Section sectionNotModerate = mock(Section.class);
    when(sectionNotModerate.isPremoderated()).thenReturn(false);

    // проверка что данные в mock resultSet верные
    Assert.assertEquals(true, resultSetModerateNew.getBoolean("moderate"));
    Assert.assertEquals(true, resultSetModerateOld.getBoolean("moderate"));
    Assert.assertEquals(false, resultSetNotModerateNew.getBoolean("moderate"));
    Assert.assertEquals(false, resultSetNotModerateOld.getBoolean("moderate"));

    Assert.assertTrue((new Timestamp(newTime)).compareTo(resultSetModerateNew.getTimestamp("postdate")) == 0);
    Assert.assertTrue((new Timestamp(oldTime)).compareTo(resultSetModerateOld.getTimestamp("postdate")) == 0);
    Assert.assertTrue((new Timestamp(newTime)).compareTo(resultSetNotModerateNew.getTimestamp("postdate")) == 0);
    Assert.assertTrue((new Timestamp(oldTime)).compareTo(resultSetNotModerateOld.getTimestamp("postdate")) == 0);


    Topic messageModerateOld = new Topic(resultSetModerateOld);
    Topic messageNotModerateOld = new Topic(resultSetNotModerateOld);
    Topic messageModerateNew = new Topic(resultSetModerateNew);
    Topic messageNotModerateNew = new Topic(resultSetNotModerateNew);

    // проверка что данные в mock message верные
    Assert.assertEquals(true, messageModerateNew.isCommited());
    Assert.assertEquals(true, messageModerateOld.isCommited());
    Assert.assertEquals(false, messageNotModerateNew.isCommited());
    Assert.assertEquals(false, messageNotModerateOld.isCommited());

    Assert.assertTrue((new Timestamp(newTime)).compareTo(messageModerateNew.getPostdate()) == 0);
    Assert.assertTrue((new Timestamp(oldTime)).compareTo(messageModerateOld.getPostdate()) == 0);
    Assert.assertTrue((new Timestamp(newTime)).compareTo(messageNotModerateNew.getPostdate()) == 0);
    Assert.assertTrue((new Timestamp(oldTime)).compareTo(messageNotModerateOld.getPostdate()) == 0);
    
    // нельзя удалять старые подтвержденные топики в премодерируемом разделе
    Assert.assertFalse(permissionService.isDeletableByModerator(messageModerateOld, user, sectionModerate));
    // можно удалять старые подтвержденные топики в непремодерируемом разделе
    Assert.assertTrue(permissionService.isDeletableByModerator(messageModerateOld, user, sectionNotModerate));
    // можно удалять старые не подтвержденные топики в премодерируемом разделе
    Assert.assertTrue(permissionService.isDeletableByModerator(messageNotModerateOld, user, sectionModerate));
    // можно удалять старые не подтвержденные топики в непремодерируемом разделе
    Assert.assertTrue(permissionService.isDeletableByModerator(messageNotModerateOld, user, sectionNotModerate));

    // можно удалять новые подтвержденные топики в премодерируемом разделе
    Assert.assertTrue(permissionService.isDeletableByModerator(messageModerateNew, user, sectionModerate));
    // можно удалять новые подтвержденные топики в непремодерируемом разделе
    Assert.assertTrue(permissionService.isDeletableByModerator(messageModerateNew, user, sectionNotModerate));
    // можно удалять новые не подтвержденные топики в премодерируемом разделе
    Assert.assertTrue(permissionService.isDeletableByModerator(messageNotModerateNew, user, sectionModerate));
    // можно удалять новые не подтвержденные топики в непремодерируемом разделе
    Assert.assertTrue(permissionService.isDeletableByModerator(messageNotModerateNew, user, sectionNotModerate));
  }

}
