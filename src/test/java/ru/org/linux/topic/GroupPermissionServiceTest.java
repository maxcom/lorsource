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
import ru.org.linux.group.GroupPermissionService;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.user.User;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 8/30/11
 * Time: 4:27 PM
 */
public class GroupPermissionServiceTest {
  private final GroupPermissionService permissionService = new GroupPermissionService();

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
    // vote, sticky, expired, bbcode, resolved, minor
    when(resultSet.getBoolean(anyString())).thenReturn(false);

    when(resultSet.getInt("section")).thenReturn(3); // Галлерея
    when(resultSet.getBoolean("moderate")).thenReturn(true);

    User user = mock(User.class);
    when(user.isModerator()).thenReturn(false);
    when(user.getId()).thenReturn(13);

    Topic message = new Topic(resultSet);

    assertFalse(user.isModerator());
    assertEquals(user.getId(), resultSet.getInt("userid"));
    assertEquals(user.getId(), message.getUid());

    assertTrue(permissionService.isDeletable(message, user));
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
    // vote, sticky, expired, bbcode, resolved, minor
    when(resultSet.getBoolean(anyString())).thenReturn(false);

    when(resultSet.getInt("section")).thenReturn(3); // Галлерея
    when(resultSet.getBoolean("moderate")).thenReturn(true);

    User user = mock(User.class);
    when(user.isModerator()).thenReturn(false);
    when(user.getId()).thenReturn(13);

    Topic message = new Topic(resultSet);

    assertFalse(user.isModerator());
    assertEquals(user.getId(), resultSet.getInt("userid"));
    assertEquals(user.getId(), message.getUid());

    assertFalse(permissionService.isDeletable(message, user));
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
    // vote, sticky, expired, bbcode, resolved, minor
    when(resultSet.getBoolean(anyString())).thenReturn(false);

    when(resultSet.getInt("section")).thenReturn(3); // Галлерея
    when(resultSet.getBoolean("moderate")).thenReturn(true);

    User user = mock(User.class);
    when(user.isModerator()).thenReturn(false);
    when(user.getId()).thenReturn(14);

    Topic message = new Topic(resultSet);

    assertFalse(user.isModerator());
    assertFalse(user.getId() == resultSet.getInt("userid"));
    assertFalse(user.getId() == message.getUid());

    assertFalse(permissionService.isDeletable(message, user));
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
    // vote, sticky, expired, bbcode, resolved, minor
    when(resultSet.getBoolean(anyString())).thenReturn(false);

    when(resultSet.getInt("section")).thenReturn(3); // Галлерея
    when(resultSet.getBoolean("moderate")).thenReturn(true);

    User user = mock(User.class);
    when(user.isModerator()).thenReturn(false);
    when(user.getId()).thenReturn(14);

    Topic message = new Topic(resultSet);

    assertFalse(user.isModerator());
    assertFalse(user.getId() == resultSet.getInt("userid"));
    assertFalse(user.getId() == message.getUid());

    assertFalse(permissionService.isDeletable(message, user));
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
    when(resultSetModerateOld.getInt("section")).thenReturn(1);    
    // gtitle, urlname, message, postip,
    when(resultSetModerateOld.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, bbcode, resolved, minor
    when(resultSetModerateOld.getBoolean(anyString())).thenReturn(false);
    when(resultSetModerateOld.getBoolean("moderate")).thenReturn(true);
    when(resultSetModerateOld.getTimestamp("postdate")).thenReturn(new Timestamp(oldTime));


    ResultSet resultSetNotModerateOld = mock(ResultSet.class);
    when(resultSetNotModerateOld.getInt("postscore")).thenReturn(-9999);
    when(resultSetNotModerateOld.wasNull()).thenReturn(false);
    // commitdate, lastmod
    when(resultSetNotModerateOld.getTimestamp(anyString())).thenReturn(new Timestamp(oldTime));
    // commitby, sectionid, stat1, ua_id,
    when(resultSetNotModerateOld.getInt(anyString())).thenReturn(2);
    // gtitle, urlname, message, postip,
    when(resultSetNotModerateOld.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, bbcode, resolved, minor
    when(resultSetNotModerateOld.getBoolean(anyString())).thenReturn(false);
    when(resultSetNotModerateOld.getBoolean("moderate")).thenReturn(false);
    when(resultSetNotModerateOld.getTimestamp("postdate")).thenReturn(new Timestamp(oldTime));


    ResultSet resultSetModerateNew = mock(ResultSet.class);
    when(resultSetModerateNew.getInt("postscore")).thenReturn(-9999);
    when(resultSetModerateNew.wasNull()).thenReturn(false);
    // commitdate, lastmod
    when(resultSetModerateNew.getTimestamp(anyString())).thenReturn(new Timestamp(newTime));
    // commitby, sectionid, stat1, ua_id,
    when(resultSetModerateNew.getInt(anyString())).thenReturn(1);
    // gtitle, urlname, message, postip,
    when(resultSetModerateNew.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, bbcode, resolved, minor
    when(resultSetModerateNew.getBoolean(anyString())).thenReturn(false);
    when(resultSetModerateNew.getBoolean("moderate")).thenReturn(true);
    when(resultSetModerateNew.getTimestamp("postdate")).thenReturn(new Timestamp(newTime));


    ResultSet resultSetNotModerateNew = mock(ResultSet.class);
    when(resultSetNotModerateNew.getInt("postscore")).thenReturn(-9999);
    when(resultSetNotModerateNew.wasNull()).thenReturn(false);
    // commitdate, lastmod
    when(resultSetNotModerateNew.getTimestamp(anyString())).thenReturn(new Timestamp(newTime));
    // commitby, sectionid, stat1, ua_id,
    when(resultSetNotModerateNew.getInt(anyString())).thenReturn(1);
    // gtitle, urlname, message, postip,
    when(resultSetNotModerateNew.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, bbcode, resolved, minor
    when(resultSetNotModerateNew.getBoolean(anyString())).thenReturn(false);
    when(resultSetNotModerateNew.getBoolean("moderate")).thenReturn(false);
    when(resultSetNotModerateNew.getTimestamp("postdate")).thenReturn(new Timestamp(newTime));


    User user = mock(User.class);
    when(user.isModerator()).thenReturn(true);
    when(user.getId()).thenReturn(13);

    // проверка что данные в mock user верные
    assertTrue(user.isModerator());

    Section sectionModerate = mock(Section.class);
    when(sectionModerate.isPremoderated()).thenReturn(true);
    Section sectionNotModerate = mock(Section.class);
    when(sectionNotModerate.isPremoderated()).thenReturn(false);

    SectionService sectionService = mock(SectionService.class);
    when(sectionService.getSection(1)).thenReturn(sectionModerate);
    when(sectionService.getSection(2)).thenReturn(sectionNotModerate);

    permissionService.setSectionService(sectionService);

    // проверка что данные в mock resultSet верные
    assertTrue(resultSetModerateNew.getBoolean("moderate"));
    assertTrue(resultSetModerateOld.getBoolean("moderate"));
    assertFalse(resultSetNotModerateNew.getBoolean("moderate"));
    assertFalse(resultSetNotModerateOld.getBoolean("moderate"));

    assertEquals(0, (new Timestamp(newTime)).compareTo(resultSetModerateNew.getTimestamp("postdate")));
    assertEquals(0, (new Timestamp(oldTime)).compareTo(resultSetModerateOld.getTimestamp("postdate")));
    assertEquals(0, (new Timestamp(newTime)).compareTo(resultSetNotModerateNew.getTimestamp("postdate")));
    assertEquals(0, (new Timestamp(oldTime)).compareTo(resultSetNotModerateOld.getTimestamp("postdate")));


    Topic messageModerateOld = new Topic(resultSetModerateOld);
    Topic messageNotModerateOld = new Topic(resultSetNotModerateOld);
    Topic messageModerateNew = new Topic(resultSetModerateNew);
    Topic messageNotModerateNew = new Topic(resultSetNotModerateNew);

    // проверка что данные в mock message верные
    assertTrue(messageModerateNew.isCommited());
    assertTrue(messageModerateOld.isCommited());
    assertFalse(messageNotModerateNew.isCommited());
    assertFalse(messageNotModerateOld.isCommited());

    assertEquals(0, (new Timestamp(newTime)).compareTo(messageModerateNew.getPostdate()));
    assertEquals(0, (new Timestamp(oldTime)).compareTo(messageModerateOld.getPostdate()));
    assertEquals(0, (new Timestamp(newTime)).compareTo(messageNotModerateNew.getPostdate()));
    assertEquals(0, (new Timestamp(oldTime)).compareTo(messageNotModerateOld.getPostdate()));
    
    // нельзя удалять старые подтвержденные топики в премодерируемом разделе
    assertFalse(permissionService.isDeletable(messageModerateOld, user));
    // можно удалять старые подтвержденные топики в непремодерируемом разделе
//    Assert.assertTrue(permissionService.isDeletableByModerator(messageModerateOld, user));
    // можно удалять старые не подтвержденные топики в премодерируемом разделе
    assertTrue(permissionService.isDeletable(messageNotModerateOld, user));
    // можно удалять старые не подтвержденные топики в непремодерируемом разделе
//    Assert.assertTrue(permissionService.isDeletableByModerator(messageNotModerateOld, user));

    // можно удалять новые подтвержденные топики в премодерируемом разделе
    assertTrue(permissionService.isDeletable(messageModerateNew, user));
    // можно удалять новые подтвержденные топики в непремодерируемом разделе
//    Assert.assertTrue(permissionService.isDeletableByModerator(messageModerateNew, user));
    // можно удалять новые не подтвержденные топики в премодерируемом разделе
    assertTrue(permissionService.isDeletable(messageNotModerateNew, user));
    // можно удалять новые не подтвержденные топики в непремодерируемом разделе
//    Assert.assertTrue(permissionService.isDeletableByModerator(messageNotModerateNew, user));
  }
}
