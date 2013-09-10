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

package ru.org.linux.tag;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TagIntegrationTestConfiguration.class)
@Transactional
public class TagDaoIntegrationTest {
  @Autowired
  TagDao tagDao;

  @Test
  public void prefixSearchExactTest() {
    tagDao.createTag("zest");
    tagDao.createTag("zesd");

    Map<String,Integer> tags = tagDao.getTagsByPrefix("zest", 0);

    assertEquals(1, tags.size());
  }

  @Test
  public void prefixSearchSimpleTest() {
    tagDao.createTag("zest");
    tagDao.createTag("zesd");

    Map<String,Integer> tags = tagDao.getTagsByPrefix("ze", 0);

    assertEquals(2, tags.size());
  }

  @Test
  public void prefixSearchEscapeTest() {
    tagDao.createTag("zest_test");
    tagDao.createTag("zestxtest");

    assertEquals(1, tagDao.getTagsByPrefix("zest_", 0).size());
    assertEquals(0, tagDao.getTagsByPrefix("zest%", 0).size());
  }
  @Test
  public void prefixSearchEscapeTest2() {
    tagDao.createTag("zest__test");
    tagDao.createTag("zest_xtest");

    assertEquals(2, tagDao.getTagsByPrefix("zest_", 0).size());
    assertEquals(1, tagDao.getTagsByPrefix("zest__", 0).size());
    assertEquals(1, tagDao.getTagsByPrefix("zest__t", 0).size());
  }
}
