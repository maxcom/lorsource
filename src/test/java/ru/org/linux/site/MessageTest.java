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

package ru.org.linux.site;

import org.junit.Test;
import junit.framework.Assert;
import ru.org.linux.dto.MessageDto;
import ru.org.linux.dto.SectionDto;
import ru.org.linux.dto.UserDto;

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
public class MessageTest {

  /**
   * Проверка что пользователь МОЖЕТ удалить топик автором которого он является
   * и прошло меньше часа с момента постинга.
   *
   * @throws Exception
   */
  @Test
  public void isDeletableByUserTest1() throws Exception {
    UserDto user;
    ResultSet resultSet;
    Calendar calendar = Calendar.getInstance();

    resultSet = mock(ResultSet.class);
    when(resultSet.getInt("postscore")).thenReturn(-9999);
    when(resultSet.wasNull()).thenReturn(false);
    calendar.setTime(new Date());
    calendar.add(Calendar.MINUTE, -10);
    // commitdate, lastmod
    when(resultSet.getTimestamp(anyString())).thenReturn(new Timestamp(calendar.getTimeInMillis()));
    // commitby, sectionid, stat1, ua_id,
    when(resultSet.getInt(anyString())).thenReturn(13);
    // gtitle, urlname, messageDto, postip,
    when(resultSet.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, havelink, bbcode, resolved, minor
    when(resultSet.getBoolean(anyString())).thenReturn(false);

    when(resultSet.getInt("section")).thenReturn(3); // Галлерея
    when(resultSet.getBoolean("moderate")).thenReturn(true);

    user = mock(UserDto.class);
    when(user.isModerator()).thenReturn(false);
    when(user.getId()).thenReturn(13);

    MessageDto messageDto = new MessageDto(resultSet);

    Assert.assertEquals(false, user.isModerator());
    Assert.assertTrue(user.getId() == resultSet.getInt("userid"));
    Assert.assertTrue(user.getId() == messageDto.getUid());

    Assert.assertTrue(messageDto.isDeletableByUser(user));
  }

  /**
   * Проверка что пользователь НЕМОЖЕТ удалить топик автором которого он является
   * и прошло больше часа с момента постинга.
   *
   * @throws Exception
   */
  @Test
  public void isDeletableByUserTest2() throws Exception {
    UserDto user;
    ResultSet resultSet;
    Calendar calendar = Calendar.getInstance();

    resultSet = mock(ResultSet.class);
    when(resultSet.getInt("postscore")).thenReturn(-9999);
    when(resultSet.wasNull()).thenReturn(false);
    calendar.setTime(new Date());
    calendar.add(Calendar.MINUTE, -70);
    // commitdate, lastmod
    when(resultSet.getTimestamp(anyString())).thenReturn(new Timestamp(calendar.getTimeInMillis()));
    // commitby, sectionid, stat1, ua_id,
    when(resultSet.getInt(anyString())).thenReturn(13);
    // gtitle, urlname, messageDto, postip,
    when(resultSet.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, havelink, bbcode, resolved, minor
    when(resultSet.getBoolean(anyString())).thenReturn(false);

    when(resultSet.getInt("section")).thenReturn(3); // Галлерея
    when(resultSet.getBoolean("moderate")).thenReturn(true);

    user = mock(UserDto.class);
    when(user.isModerator()).thenReturn(false);
    when(user.getId()).thenReturn(13);

    MessageDto messageDto = new MessageDto(resultSet);

    Assert.assertEquals(false, user.isModerator());
    Assert.assertTrue(user.getId() == resultSet.getInt("userid"));
    Assert.assertTrue(user.getId() == messageDto.getUid());

    Assert.assertFalse(messageDto.isDeletableByUser(user));
  }

  /**
   * Проверка что пользователь НЕМОЖЕТ удалить топик автором которого он неявляется
   * и прошло больше часа с момента постинга.
   *
   * @throws Exception
   */
  @Test
  public void isDeletableByUserTest3() throws Exception {
    UserDto user;
    ResultSet resultSet;
    Calendar calendar = Calendar.getInstance();

    resultSet = mock(ResultSet.class);
    when(resultSet.getInt("postscore")).thenReturn(-9999);
    when(resultSet.wasNull()).thenReturn(false);
    calendar.setTime(new Date());
    calendar.add(Calendar.MINUTE, -70);
    // commitdate, lastmod
    when(resultSet.getTimestamp(anyString())).thenReturn(new Timestamp(calendar.getTimeInMillis()));
    // commitby, sectionid, stat1, ua_id,
    when(resultSet.getInt(anyString())).thenReturn(13);
    // gtitle, urlname, messageDto, postip,
    when(resultSet.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, havelink, bbcode, resolved, minor
    when(resultSet.getBoolean(anyString())).thenReturn(false);

    when(resultSet.getInt("section")).thenReturn(3); // Галлерея
    when(resultSet.getBoolean("moderate")).thenReturn(true);

    user = mock(UserDto.class);
    when(user.isModerator()).thenReturn(false);
    when(user.getId()).thenReturn(14);

    MessageDto messageDto = new MessageDto(resultSet);

    Assert.assertEquals(false, user.isModerator());
    Assert.assertFalse(user.getId() == resultSet.getInt("userid"));
    Assert.assertFalse(user.getId() == messageDto.getUid());

    Assert.assertFalse(messageDto.isDeletableByUser(user));
  }

  /**
   * Проверка что пользователь НЕМОЖЕТ удалить топик автором которого он неявляется
   * и прошло больше часа с момента постинга.
   *
   * @throws Exception
   */
  @Test
  public void isDeletableByUserTest4() throws Exception {
    UserDto user;
    ResultSet resultSet;
    Calendar calendar = Calendar.getInstance();

    resultSet = mock(ResultSet.class);
    when(resultSet.getInt("postscore")).thenReturn(-9999);
    when(resultSet.wasNull()).thenReturn(false);
    calendar.setTime(new Date());
    calendar.add(Calendar.MINUTE, -5);
    // commitdate, lastmod
    when(resultSet.getTimestamp(anyString())).thenReturn(new Timestamp(calendar.getTimeInMillis()));
    // commitby, sectionid, stat1, ua_id,
    when(resultSet.getInt(anyString())).thenReturn(13);
    // gtitle, urlname, messageDto, postip,
    when(resultSet.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, havelink, bbcode, resolved, minor
    when(resultSet.getBoolean(anyString())).thenReturn(false);

    when(resultSet.getInt("section")).thenReturn(3); // Галлерея
    when(resultSet.getBoolean("moderate")).thenReturn(true);

    user = mock(UserDto.class);
    when(user.isModerator()).thenReturn(false);
    when(user.getId()).thenReturn(14);

    MessageDto messageDto = new MessageDto(resultSet);

    Assert.assertEquals(false, user.isModerator());
    Assert.assertFalse(user.getId() == resultSet.getInt("userid"));
    Assert.assertFalse(user.getId() == messageDto.getUid());

    Assert.assertFalse(messageDto.isDeletableByUser(user));
  }

  /**
   * Проверка для модератора
   *
   * @throws Exception
   */
  @Test
  public void isDeletableByModeratorTest() throws Exception {
    UserDto user;
    ResultSet resultSetModerateOld;
    ResultSet resultSetNotModerateOld;
    ResultSet resultSetModerateNew;
    ResultSet resultSetNotModerateNew;
    SectionDto sectionDtoModerate;
    SectionDto sectionDtoNotModerate;

    Calendar calendar = Calendar.getInstance();

    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -2);
    long oldTime = calendar.getTimeInMillis();

    calendar.setTime(new Date());
    calendar.add(Calendar.DAY_OF_MONTH, -2);
    long newTime = calendar.getTimeInMillis();


    resultSetModerateOld = mock(ResultSet.class);
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


    resultSetNotModerateOld = mock(ResultSet.class);
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


    resultSetModerateNew = mock(ResultSet.class);
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


    resultSetNotModerateNew = mock(ResultSet.class);
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


    user = mock(UserDto.class);
    when(user.isModerator()).thenReturn(true);
    when(user.getId()).thenReturn(13);

    // проверка что данные в mock user верные
    Assert.assertEquals(true, user.isModerator());


    sectionDtoModerate = mock(SectionDto.class);
    when(sectionDtoModerate.isPremoderated()).thenReturn(true);
    sectionDtoNotModerate = mock(SectionDto.class);
    when(sectionDtoNotModerate.isPremoderated()).thenReturn(false);

    // проверка что данные в mock resultSet верные
    Assert.assertEquals(true, resultSetModerateNew.getBoolean("moderate"));
    Assert.assertEquals(true, resultSetModerateOld.getBoolean("moderate"));
    Assert.assertEquals(false, resultSetNotModerateNew.getBoolean("moderate"));
    Assert.assertEquals(false, resultSetNotModerateOld.getBoolean("moderate"));

    Assert.assertTrue((new Timestamp(newTime)).compareTo(resultSetModerateNew.getTimestamp("postdate")) == 0);
    Assert.assertTrue((new Timestamp(oldTime)).compareTo(resultSetModerateOld.getTimestamp("postdate")) == 0);
    Assert.assertTrue((new Timestamp(newTime)).compareTo(resultSetNotModerateNew.getTimestamp("postdate")) == 0);
    Assert.assertTrue((new Timestamp(oldTime)).compareTo(resultSetNotModerateOld.getTimestamp("postdate")) == 0);


    MessageDto messageDtoModerateOld = new MessageDto(resultSetModerateOld);
    MessageDto messageDtoNotModerateOld = new MessageDto(resultSetNotModerateOld);
    MessageDto messageDtoModerateNew = new MessageDto(resultSetModerateNew);
    MessageDto messageDtoNotModerateNew = new MessageDto(resultSetNotModerateNew);

    // проверка что данные в mock message верные
    Assert.assertEquals(true, messageDtoModerateNew.isCommited());
    Assert.assertEquals(true, messageDtoModerateOld.isCommited());
    Assert.assertEquals(false, messageDtoNotModerateNew.isCommited());
    Assert.assertEquals(false, messageDtoNotModerateOld.isCommited());

    Assert.assertTrue((new Timestamp(newTime)).compareTo(messageDtoModerateNew.getPostdate()) == 0);
    Assert.assertTrue((new Timestamp(oldTime)).compareTo(messageDtoModerateOld.getPostdate()) == 0);
    Assert.assertTrue((new Timestamp(newTime)).compareTo(messageDtoNotModerateNew.getPostdate()) == 0);
    Assert.assertTrue((new Timestamp(oldTime)).compareTo(messageDtoNotModerateOld.getPostdate()) == 0);

    // нельзя удалять старые подтвержденные топики в премодерируемом разделе
    Assert.assertFalse(messageDtoModerateOld.isDeletableByModerator(user, sectionDtoModerate));
    // можно удалять старые подтвержденные топики в непремодерируемом разделе
    Assert.assertTrue(messageDtoModerateOld.isDeletableByModerator(user, sectionDtoNotModerate));
    // можно удалять старые не подтвержденные топики в премодерируемом разделе
    Assert.assertTrue(messageDtoNotModerateOld.isDeletableByModerator(user, sectionDtoModerate));
    // можно удалять старые не подтвержденные топики в непремодерируемом разделе
    Assert.assertTrue(messageDtoNotModerateOld.isDeletableByModerator(user, sectionDtoNotModerate));

    // можно удалять новые подтвержденные топики в премодерируемом разделе
    Assert.assertTrue(messageDtoModerateNew.isDeletableByModerator(user, sectionDtoModerate));
    // можно удалять новые подтвержденные топики в непремодерируемом разделе
    Assert.assertTrue(messageDtoModerateNew.isDeletableByModerator(user, sectionDtoNotModerate));
    // можно удалять новые не подтвержденные топики в премодерируемом разделе
    Assert.assertTrue(messageDtoNotModerateNew.isDeletableByModerator(user, sectionDtoModerate));
    // можно удалять новые не подтвержденные топики в непремодерируемом разделе
    Assert.assertTrue(messageDtoNotModerateNew.isDeletableByModerator(user, sectionDtoNotModerate));
  }

}
