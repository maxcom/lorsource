package ru.org.linux.comment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;

import java.sql.Timestamp;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("integration-tests-context2.xml")
public class CommentDao2IntegrationTest {
    @Autowired
    CommentDao commentDao;

    @Autowired
    UserDao userDao;

    @Test
    public void deletedCommentsForUserNodashiTest() throws Exception {
      User nodashi = userDao.getUser("no-dashi");
      List<CommentDao.DeletedCommentForUser> nodashiDeleted= commentDao.getDeletedCommentsForUser(nodashi, 0, 0);

      assertEquals(1, nodashiDeleted.size());
      assertEquals(commentDao.getCountDeletedCommentsForUser(nodashi), commentDao.getDeletedCommentsForUser(nodashi, 0, 0).size());

      CommentDao.DeletedCommentForUser[] nodashiDeletedArray = nodashiDeleted.toArray(new CommentDao.DeletedCommentForUser[nodashiDeleted.size()]);
      assertEquals(1948636, nodashiDeletedArray[0].getId());
      assertEquals(0, nodashiDeletedArray[0].getBonus());
      assertEquals(1, nodashiDeletedArray[0].getModeratorId());
      assertEquals(Timestamp.valueOf("2010-06-11 15:15:40.947313"), nodashiDeletedArray[0].getDate());
    }

  @Test
  public void deletedCommentsForUserSunchTest() throws Exception {
    User user = userDao.getUser("Sun-ch");
    assertEquals(7, commentDao.getDeletedCommentsForUser(user, 0, 0).size());
    assertEquals(2, commentDao.getDeletedCommentsForUser(user, 0, 2).size());
    assertEquals(5, commentDao.getDeletedCommentsForUser(user, 2, 0).size());
    assertEquals(2, commentDao.getDeletedCommentsForUser(user, 2, 2).size());
    assertEquals(commentDao.getCountDeletedCommentsForUser(user), commentDao.getDeletedCommentsForUser(user, 0, 0).size());
  }

  @Test
  public void deletedCommentForUserWithZeroDeletedTest() throws Exception {
    User user = userDao.getUser("waker");
    assertEquals(0, commentDao.getCountDeletedCommentsForUser(user));
    assertEquals(0, commentDao.getDeletedCommentsForUser(user, 0, 0).size());
  }
}
