/*
 * Copyright 1998-2016 Linux.org.ru
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
        @ContextConfiguration("classpath:database.xml"),
        @ContextConfiguration(classes = CommentDaoIntegrationTestConfiguration.class)
})
@Transactional
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
    topicId = jdbcTemplate.queryForObject("select min (id) from topics", Integer.class);
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
      "INSERT INTO msgbase (id, message) VALUES (?, ?)",
      commentId,
      body
    );

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
    int commentId = jdbcTemplate.queryForObject("select nextval('s_msgid')", Integer.class);

    addComment(
            commentId,
            null,
            "CommentDaoIntegrationTest.editCommentTest()",
            "CommentDaoIntegrationTest.editCommentTest(): comment body"
    );


    Comment oldComment = mock(Comment.class);
    when(oldComment.getId()).thenReturn(commentId);

    commentDao.changeTitle(oldComment, "CommentDaoIntegrationTest.editCommentTest(): new title");

    List<Map<String, Object>> rows = getComment(commentId);
    assertFalse("No any records", rows.isEmpty());
    Map<String, Object> row = rows.get(0);
    assertEquals("CommentDaoIntegrationTest.editCommentTest(): new title", row.get("title"));
  }

  @Test
  public void updateLatestEditorInfoTest() {
    int commentId = jdbcTemplate.queryForObject("select nextval('s_msgid')", Integer.class);

    addComment(
            commentId,
            null,
            "CommentDaoIntegrationTest.updateLatestEditorInfoTest()",
            "comment body"
    );

    List<Map<String, Object>> rows = getComment(commentId);
    assertFalse("No any records", rows.isEmpty());
    Map<String, Object> row = rows.get(0);

    assertNull(row.get("edit_nick"));
    assertNull(row.get("edit_date"));
    assertEquals(0, row.get("edit_count"));

    Date commentEditDate = new Date();
    commentDao.updateLatestEditorInfo(commentId, 1, commentEditDate, 1234);

    rows = getComment(commentId);
    assertFalse("No any records", rows.isEmpty());
    row = rows.get(0);
    Timestamp rowTimestamp = (Timestamp) row.get("edit_date");
    assertEquals("maxcom", row.get("edit_nick"));
    assertEquals(rowTimestamp.getTime(), commentEditDate.getTime());
    assertEquals(1234, row.get("edit_count"));
  }

  @Test
  public void isHaveAnswersTest() {
    int commentId1 = jdbcTemplate.queryForObject("select nextval('s_msgid')", Integer.class);
    int commentId2 = jdbcTemplate.queryForObject("select nextval('s_msgid')", Integer.class);
    addComment(
            commentId1,
            null,
            "CommentDaoIntegrationTest.isHaveAnswersTest() - 1",
            "comment body"
    );

    assertEquals(0, commentDao.getRepliesCount(commentId1));

    addComment(
            commentId2,
            commentId1,
            "CommentDaoIntegrationTest.isHaveAnswersTest() - 2",
            "comment body"
    );

    assertTrue(commentDao.getRepliesCount(commentId1)>0);
  }
}
