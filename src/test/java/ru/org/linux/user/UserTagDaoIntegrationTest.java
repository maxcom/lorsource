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

package ru.org.linux.user;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("integration-tests-context.xml")
public class UserTagDaoIntegrationTest {
  private static final String QUERY_COUNT_FAVORITE_BY_USER = "SELECT count(user_id) FROM user_tags WHERE is_favorite=true AND user_id=?";
  private static final String QUERY_COUNT_IGNORE_BY_USER = "SELECT count(user_id) FROM user_tags WHERE is_favorite=false AND user_id=?";
  @Autowired
  UserTagDao userTagDao;

  private JdbcTemplate jdbcTemplate;

  private int user1Id;
  private int user2Id;

  private int tag1Id;
  private int tag2Id;
  private int tag3Id;
  private int tag4Id;
  private int tag5Id;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  private int createUser(String userName) {
    int userid = jdbcTemplate.queryForInt("SELECT nextval('s_uid') AS userid");

    jdbcTemplate.update(
      "INSERT INTO users (id, name, nick) VALUES (?, ?, ?)",
      userid,
      userName,
      userName
    );
    return userid;
  }

  private int createTag(String tagName) {

    jdbcTemplate.update(
      "INSERT INTO tags_values (value) VALUES (?)",
      tagName
    );
    return jdbcTemplate.queryForInt("SELECT currval('tags_values_id_seq') AS tagid");
  }

  @Before
  public void prepareTestData() {
    cleanupTestData();

    user1Id = createUser("UserTagDaoIntegrationTest_user1");
    user2Id = createUser("UserTagDaoIntegrationTest_user2");

    tag1Id = createTag("UserTagDaoIntegrationTest_tag1");
    tag2Id = createTag("UserTagDaoIntegrationTest_tag2");
    tag3Id = createTag("UserTagDaoIntegrationTest_tag3");
    tag4Id = createTag("UserTagDaoIntegrationTest_tag4");
    tag5Id = createTag("UserTagDaoIntegrationTest_tag5");
  }

  @After
  public void cleanupTestData() {
    jdbcTemplate.update(
      "DELETE FROM user_TAGS WHERE user_id in (" + user1Id + ", " + user2Id + ')'
    );

    jdbcTemplate.update(
      "DELETE FROM users WHERE nick LIKE 'UserTagDaoIntegrationTest_user%'"
    );

    jdbcTemplate.update(
      "DELETE FROM tags_values WHERE value LIKE 'UserTagDaoIntegrationTest_tag%'"
    );

  }

  private void prepareUserTags() {
    userTagDao.addTag(user1Id, tag1Id, true);
    userTagDao.addTag(user2Id, tag1Id, true);
    userTagDao.addTag(user1Id, tag2Id, true);
    userTagDao.addTag(user1Id, tag2Id, false);
    userTagDao.addTag(user2Id, tag2Id, true);
    userTagDao.addTag(user2Id, tag3Id, true);
    userTagDao.addTag(user1Id, tag3Id, true);
    userTagDao.addTag(user2Id, tag4Id, true);
    userTagDao.addTag(user1Id, tag4Id, true);
    userTagDao.addTag(user1Id, tag5Id, false);
    userTagDao.addTag(user2Id, tag5Id, true);
    userTagDao.addTag(user1Id, tag5Id, true);
  }

  @Test
  public void addTest() {
    prepareUserTags();

    userTagDao.addTag(user1Id, tag1Id, false);

    int result;

    result = jdbcTemplate.queryForInt(
      QUERY_COUNT_FAVORITE_BY_USER,
      user1Id
    );
    Assert.assertEquals("Wrong count of user tags.", 5, result);

    result = jdbcTemplate.queryForInt(
      QUERY_COUNT_IGNORE_BY_USER,
      user1Id
    );
    Assert.assertEquals("Wrong count of user tags.", 3, result);

    result = jdbcTemplate.queryForInt(
      QUERY_COUNT_FAVORITE_BY_USER,
      user2Id
    );
    Assert.assertEquals("Wrong count of user tags.", 5, result);

    result = jdbcTemplate.queryForInt(
      QUERY_COUNT_IGNORE_BY_USER,
      user2Id
    );
    Assert.assertEquals("Wrong count of user tags.", 0, result);

    result = jdbcTemplate.queryForInt(
      "SELECT count(user_id) FROM user_tags WHERE tag_id=?",
      tag1Id
    );
    Assert.assertEquals("Wrong count of user tags.", 3, result);
  }

  @Test
  public void deleteOneTest() {
    prepareUserTags();

    userTagDao.deleteTag(user1Id, tag1Id, true);
    userTagDao.deleteTag(user1Id, tag2Id, true);

    int result;

    result = jdbcTemplate.queryForInt(
      QUERY_COUNT_FAVORITE_BY_USER,
      user1Id
    );
    Assert.assertEquals("Wrong count of user tags.", 3, result);

    userTagDao.deleteTag(user1Id, tag2Id, false);

    result = jdbcTemplate.queryForInt(
      QUERY_COUNT_FAVORITE_BY_USER,
      user1Id
    );
    Assert.assertEquals("Wrong count of user tags.", 3, result);

    result = jdbcTemplate.queryForInt(
      QUERY_COUNT_IGNORE_BY_USER,
      user1Id
    );
    Assert.assertEquals("Wrong count of user tags.", 1, result);
  }

  @Test
  public void deleteAllTest() {
    prepareUserTags();

    userTagDao.deleteTags(tag2Id);

    int result;

    result = jdbcTemplate.queryForInt(
      QUERY_COUNT_FAVORITE_BY_USER,
      user1Id
    );
    Assert.assertEquals("Wrong count of user tags.", 4, result);

    result = jdbcTemplate.queryForInt(
      QUERY_COUNT_IGNORE_BY_USER,
      user1Id
    );
    Assert.assertEquals("Wrong count of user tags.", 1, result);

    result = jdbcTemplate.queryForInt(
      QUERY_COUNT_FAVORITE_BY_USER,
      user2Id
    );
    Assert.assertEquals("Wrong count of user tags.", 4, result);
  }

  @Test
  public void getTest() {
    prepareUserTags();

    ImmutableList<String> tags;

    tags = userTagDao.getTags(user1Id, true);
    Assert.assertEquals("Wrong count of user tags.", 5, tags.size());

    tags = userTagDao.getTags(user1Id, false);
    Assert.assertEquals("Wrong count of user tags.", 2, tags.size());
  }

  @Test
  public void getUserIdListByTagsTest() {
    prepareUserTags();
    List<Integer> userIdList;
    List<String> tags = new ArrayList<>();
    tags.add("UserTagDaoIntegrationTest_tag1");
    userIdList = userTagDao.getUserIdListByTags(user1Id, tags);
    Assert.assertEquals("Wrong count of user ID's.", 1, userIdList.size());

    tags.add("UserTagDaoIntegrationTest_tag2");
    userIdList = userTagDao.getUserIdListByTags(user1Id, tags);
    Assert.assertEquals("Wrong count of user ID's.", 1, userIdList.size());

    tags.clear();
    userTagDao.deleteTag(user1Id, tag5Id, true);
    tags.add("UserTagDaoIntegrationTest_tag5");
    userIdList = userTagDao.getUserIdListByTags(user1Id, tags);
    Assert.assertEquals("Wrong count of user ID's.", 1, userIdList.size());
  }

  @Test
  public void replaceTagTest() {
    int result;
    prepareUserTags();

    userTagDao.replaceTag(tag2Id, tag1Id);
    result = jdbcTemplate.queryForInt(
      "SELECT count(user_id) FROM user_tags WHERE tag_id=?",
      tag1Id
    );
    Assert.assertEquals("Wrong count of user tags.", 2, result);

    userTagDao.deleteTags(tag1Id);
    userTagDao.replaceTag(tag2Id, tag1Id);
    result = jdbcTemplate.queryForInt(
      "SELECT count(user_id) FROM user_tags WHERE tag_id=?",
      tag1Id
    );
    Assert.assertEquals("Wrong count of user tags.", 3, result);
  }
}
