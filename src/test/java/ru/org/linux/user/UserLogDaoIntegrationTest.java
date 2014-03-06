package ru.org.linux.user;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = UserLogDaoIntegrationTestConfiguration.class)
@Transactional
public class UserLogDaoIntegrationTest {
  @Autowired
  private UserLogDao userLogDao;

  @Test
  public void testLogAcceptEmail() {
    User user = mock(User.class);
    when(user.getId()).thenReturn(UserDaoIntegrationTest.TEST_ID);
    when(user.getEmail()).thenReturn("old@email");

    userLogDao.logAcceptNewEmail(user, "test@email");

    List<UserLogItem> logItems = userLogDao.getLogItems(user, true);

    Assert.assertEquals(1, logItems.size());

    UserLogItem item = logItems.get(0);

    assertNotNull(item);
    assertEquals(UserLogAction.ACCEPT_NEW_EMAIL, item.getAction());
  }

  @Test
  public void testLogScore50() {
    User user = mock(User.class);
    when(user.getId()).thenReturn(UserDaoIntegrationTest.TEST_ID);

    userLogDao.logScore50(user, user);

    List<UserLogItem> logItems = userLogDao.getLogItems(user, true);

    Assert.assertEquals(1, logItems.size());

    UserLogItem item = logItems.get(0);

    assertNotNull(item);
    assertEquals(UserLogAction.SCORE50, item.getAction());
  }
}
