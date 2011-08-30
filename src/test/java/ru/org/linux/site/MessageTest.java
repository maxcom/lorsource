package ru.org.linux.site;

import org.junit.Test;
import junit.framework.Assert;

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
   * и прошло меньше часа с момента почтинга
   * @throws Exception
   */
  @Test
  public void isDeletableByUserTest1() throws Exception {
    User user;
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
    // gtitle, urlname, message, postip,
    when(resultSet.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, havelink, bbcode, resolved, minor
    when(resultSet.getBoolean(anyString())).thenReturn(false);

    when(resultSet.getInt("section")).thenReturn(3); // Галлерея
    when(resultSet.getBoolean("moderate")).thenReturn(true);

    user = mock(User.class);
    when(user.canModerate()).thenReturn(false);
    when(user.getId()).thenReturn(13);

    Message message = new Message(resultSet);

    Assert.assertTrue(message.isDeletableByUser(user));
  }
  /**
   * Проверка что пользователь НЕМОЖЕТ удалить топик автором которого он является
   * и прошло больше часа с момента почтинга
   * @throws Exception
   */
  @Test
  public void isDeletableByUserTest2() throws Exception {
    User user;
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
    // gtitle, urlname, message, postip,
    when(resultSet.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, havelink, bbcode, resolved, minor
    when(resultSet.getBoolean(anyString())).thenReturn(false);

    when(resultSet.getInt("section")).thenReturn(3); // Галлерея
    when(resultSet.getBoolean("moderate")).thenReturn(true);

    user = mock(User.class);
    when(user.canModerate()).thenReturn(false);
    when(user.getId()).thenReturn(13);

    Message message = new Message(resultSet);

    Assert.assertFalse(message.isDeletableByUser(user));
  }

  /**
   * Проверка что пользователь НЕМОЖЕТ удалить топик автором которого он является
   * и прошло больше часа с момента постинга
   * @throws Exception
   */
  @Test
  public void isDeletableByUserTest3() throws Exception {
    User user;
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
    // gtitle, urlname, message, postip,
    when(resultSet.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, havelink, bbcode, resolved, minor
    when(resultSet.getBoolean(anyString())).thenReturn(false);

    when(resultSet.getInt("section")).thenReturn(3); // Галлерея
    when(resultSet.getBoolean("moderate")).thenReturn(true);

    user = mock(User.class);
    when(user.canModerate()).thenReturn(false);
    when(user.getId()).thenReturn(14);

    Message message = new Message(resultSet);

    Assert.assertFalse(message.isDeletableByUser(user));
  }

  /**
   * Проверка что пользователь НЕМОЖЕТ удалить топик автором которого он является
   * и прошло больше часа с момента постинга
   * @throws Exception
   */
  @Test
  public void isDeletableByUserTest4() throws Exception {
    User user;
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
    // gtitle, urlname, message, postip,
    when(resultSet.getString(anyString())).thenReturn("any");
    // vote, sticky, expired, havelink, bbcode, resolved, minor
    when(resultSet.getBoolean(anyString())).thenReturn(false);

    when(resultSet.getInt("section")).thenReturn(3); // Галлерея
    when(resultSet.getBoolean("moderate")).thenReturn(true);

    user = mock(User.class);
    when(user.canModerate()).thenReturn(false);
    when(user.getId()).thenReturn(14);

    Message message = new Message(resultSet);

    Assert.assertFalse(message.isDeletableByUser(user));
  }

  /**
   * Проверка для модератора
   * @throws Exception
   */
  @Test
  public void isDeletableByModeratorTest() throws Exception {
    User user;
    ResultSet resultSetModerateOld;
    ResultSet resultSetNotModerateOld;
    ResultSet resultSetModerateNew;
    ResultSet resultSetNotModerateNew;
    Section sectionModerate;
    Section sectionNotModerate;

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


    user = mock(User.class);
    when(user.canModerate()).thenReturn(true);
    when(user.getId()).thenReturn(13);

    sectionModerate = mock(Section.class);
    when(sectionModerate.isPremoderated()).thenReturn(true);
    sectionNotModerate = mock(Section.class);
    when(sectionNotModerate.isPremoderated()).thenReturn(false);

    Message messageModerateOld = new Message(resultSetModerateOld);
    Message messageNotModerateOld = new Message(resultSetNotModerateOld);
    Message messageModerateNew = new Message(resultSetModerateNew);
    Message messageNotModerateNew = new Message(resultSetNotModerateNew);


    // нельзя удалять старые подтвержденные топики в премодерируемом разделе
    Assert.assertFalse(messageModerateOld.isDeletableByModerator(user, sectionModerate));
    // можно удалять старые подтвержденные топики в непремодерируемом разделе
    Assert.assertTrue(messageModerateOld.isDeletableByModerator(user, sectionNotModerate));
    // можно удалять старые не подтвержденные топики в премодерируемом разделе
    Assert.assertTrue(messageNotModerateOld.isDeletableByModerator(user, sectionModerate));
    // можно удалять старые не подтвержденные топики в непремодерируемом разделе
    Assert.assertTrue(messageNotModerateOld.isDeletableByModerator(user, sectionNotModerate));

    // можно удалять новые подтвержденные топики в премодерируемом разделе
    Assert.assertTrue(messageModerateNew.isDeletableByModerator(user, sectionModerate));
    // можно удалять новые подтвержденные топики в непремодерируемом разделе
    Assert.assertTrue(messageModerateNew.isDeletableByModerator(user, sectionNotModerate));
    // можно удалять новые не подтвержденные топики в премодерируемом разделе
    Assert.assertTrue(messageNotModerateNew.isDeletableByModerator(user, sectionModerate));
    // можно удалять новые не подтвержденные топики в непремодерируемом разделе
    Assert.assertTrue(messageNotModerateNew.isDeletableByModerator(user, sectionNotModerate));
  }

}
