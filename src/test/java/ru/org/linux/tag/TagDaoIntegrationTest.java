/*
 * Copyright 1998-2024 Linux.org.ru
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

package ru.org.linux.tag;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import scala.Option;
import scala.collection.Seq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
        @ContextConfiguration("classpath:database.xml"),
        @ContextConfiguration(classes = TagIntegrationTestConfiguration.class)
})
@Transactional
public class TagDaoIntegrationTest {
  @Autowired
  TagDao tagDao;

  @Test
  public void testTagNotFound() {
    Option<Object> fetch = tagDao.getTagId("fdsfsdfdsfsdfs", false);

    assertTrue(fetch.isEmpty());
  }

  @Test
  public void createAndGetTest() {
    int id = tagDao.createTag("test-tag");
    Option<Object> fetchId = tagDao.getTagId("test-tag", false);

    assertEquals(Option.apply((Object) id), fetchId);
  }

  @Test
  public void prefixSearchExactTest() {
    tagDao.createTag("zest");
    tagDao.createTag("zesd");

    scala.collection.Seq<TagInfo> tags = tagDao.getTagsByPrefix("zest", 0);

    assertEquals(1, tags.size());
  }

  @Test
  public void prefixTopSearchExactTest() {
    tagDao.createTag("zest");
    tagDao.createTag("zesd");

    Seq<String> tags = tagDao.getTopTagsByPrefix("zest", 0, 20);

    assertEquals(1, tags.size());
  }

  @Test
  public void prefixSearchSimpleTest() {
    int zest = tagDao.createTag("zest");
    int zesd = tagDao.createTag("zesd");

    scala.collection.Seq<TagInfo> tags = tagDao.getTagsByPrefix("ze", 0);

    assertEquals(2, tags.size());
    assertEquals(zesd, tags.apply(0).id());
    assertEquals(zest, tags.apply(1).id());
  }

  @Test
  public void prefixSearchEscapeTest() {
    tagDao.createTag("zestxtest");

    assertEquals(0, tagDao.getTagsByPrefix("zest_", 0).size());
    assertEquals(0, tagDao.getTagsByPrefix("zest%", 0).size());
  }
}
