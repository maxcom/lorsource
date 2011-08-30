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

  @Test
  public void isDeletableByUserTest() throws Exception {
    User user;
    Section section;
    ResultSet resultSet;
    Calendar calendar = Calendar.getInstance();

    resultSet = mock(ResultSet.class);
    when(resultSet.getInt("msgid")).thenReturn(14);
    when(resultSet.getInt("postscore")).thenReturn(-9999);
    when(resultSet.wasNull()).thenReturn(false);
    when(resultSet.getInt("userid")).thenReturn(14);
    calendar.setTime(new Date());
    calendar.add(Calendar.DAY_OF_MONTH, -5);
    when(resultSet.getTimestamp("postdate")).thenReturn(new Timestamp(calendar.getTimeInMillis()));
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
    when(user.canModerate()).thenReturn(true);

    section = mock(Section.class);
    when(section.isPremoderated()).thenReturn(true);

    Message message = new Message(resultSet);
    Assert.assertTrue(message.isDeletableByModerator(user, section));
  }
}
