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

package ru.org.linux.comment;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("integration-tests-context.xml")
public class CommentDaoIntegrationTest {
  private int topicId;

  @Autowired
  private CommentDao commentDao;

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @Before
  public void setUp() {
    topicId = jdbcTemplate.queryForInt("select min (id) from topics");
  }

  private void addComment(int commentId, Integer replyToId, String title, String body) {
    jdbcTemplate.update(
      "INSERT INTO comments (id, userid, title, postdate, replyto, deleted, topic, postip, ua_id) " +
        "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, 'f', ?, ?::inet, create_user_agent(?))",
      commentId,
      1,
      title,
      replyToId,
      topicId,
      "127.0.0.1",
      "Integration test User Agent"
    );
    jdbcTemplate.update(
      "INSERT INTO msgbase (id, message, bbcode) VALUES (?, ?, true)",
      commentId,
      body
    );

  }

  private void delComment(int commentId) {
    jdbcTemplate.update("DELETE FROM msgbase WHERE id=?", commentId);
    jdbcTemplate.update("DELETE FROM user_events WHERE comment_id=?", commentId);
    jdbcTemplate.update("DELETE FROM comments WHERE id=?", commentId);
  }

  private List<Map<String, Object>> getComment(int commentId) {
    return jdbcTemplate.queryForList(
      "SELECT *, editors.nick as edit_nick FROM comments " +
        " LEFT JOIN users as editors ON comments.editor_id=editors.id " +
        " WHERE comments.id=? ", commentId
    );
  }

  @Test
  public void editCommentTest() {
    int commentId = jdbcTemplate.queryForInt("select nextval('s_msgid')");
    try {
      addComment(
        commentId,
        null,
        "CommentDaoIntegrationTest.editCommentTest()",
        "CommentDaoIntegrationTest.editCommentTest(): comment body"
      );


      Comment oldComment = mock(Comment.class);
      when(oldComment.getId()).thenReturn(commentId);

      Comment newComment = mock(Comment.class);
      when(newComment.getTitle()).thenReturn("CommentDaoIntegrationTest.editCommentTest(): new title");

      commentDao.edit(oldComment, newComment, "test body");

      List<Map<String, Object>> rows = getComment(commentId);
      Assert.assertFalse("No any records", rows.size() == 0);
      Map<String, Object> row = rows.get(0);
      Assert.assertEquals("CommentDaoIntegrationTest.editCommentTest(): new title", row.get("title"));

      rows = jdbcTemplate.queryForList(
        "SELECT * FROM msgbase WHERE id=?", commentId
      );

      Assert.assertFalse("No any records", rows.size() == 0);
      row = rows.get(0);
      Assert.assertEquals("test body", row.get("message"));
    } finally {
      delComment(commentId);
    }
  }

  @Test
  public void updateLatestEditorInfoTest() {
    int commentId = jdbcTemplate.queryForInt("select nextval('s_msgid')");
    try {
      addComment(
        commentId,
        null,
        "CommentDaoIntegrationTest.updateLatestEditorInfoTest()",
        "comment body"
      );

      List<Map<String, Object>> rows = getComment(commentId);
      Assert.assertFalse("No any records", rows.size() == 0);
      Map<String, Object> row = rows.get(0);

      Assert.assertTrue(row.get("edit_nick") == null);
      Assert.assertTrue(row.get("edit_date") == null);
      Assert.assertEquals(row.get("edit_count"), 0);

      Date commentEditDate = new Date();
      commentDao.updateLatestEditorInfo(commentId, 1, commentEditDate, 1234);

      rows = getComment(commentId);
      Assert.assertFalse("No any records", rows.size() == 0);
      row = rows.get(0);
      Timestamp rowTimestamp = (Timestamp) row.get("edit_date");
      Assert.assertEquals("maxcom", row.get("edit_nick"));
      Assert.assertEquals(rowTimestamp.getTime(), commentEditDate.getTime());
      Assert.assertEquals(row.get("edit_count"), 1234);
    } finally {
      delComment(commentId);
    }
  }

  @Test
  public void isHaveAnswersTest() {
    int commentId1 = jdbcTemplate.queryForInt("select nextval('s_msgid')");
    int commentId2 = jdbcTemplate.queryForInt("select nextval('s_msgid')");
    try {
      addComment(
        commentId1,
        null,
        "CommentDaoIntegrationTest.isHaveAnswersTest() - 1",
        "comment body"
      );

      Assert.assertFalse(commentDao.isHaveAnswers(commentId1));

      addComment(
        commentId2,
        commentId1,
        "CommentDaoIntegrationTest.isHaveAnswersTest() - 2",
        "comment body"
      );
      Assert.assertTrue(commentDao.isHaveAnswers(commentId1));

    } finally {
      delComment(commentId2);
      delComment(commentId1);
    }
  }
}
