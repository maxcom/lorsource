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

package ru.org.linux.user;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.org.linux.tag.TagDao;
import ru.org.linux.tag.TagNotFoundException;
import ru.org.linux.tag.TagService;
import scala.Option;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("unit-tests-context.xml")
public class UserTagServiceTest {
  @Autowired
  private TagDao tagDao;

  @Autowired
  private TagService tagService;

  @Autowired
  private UserTagDao userTagDao;

  @Autowired
  private UserTagService userTagService;

  private User user;

  @Before
  public void resetMockObjects() throws Exception {
    reset(userTagDao);
    reset(tagService);
    when(tagService.getTagId(eq("tag1"), anyBoolean())).thenReturn(2);
    when(tagService.getTagIdOptWithSynonym(eq("tag1"))).thenReturn(Option.apply(2));
    user = getUser(1);
  }

  private User getUser(int id) {
    ResultSet rs = mock(ResultSet.class);
    try {
      when(rs.getInt("id")).thenReturn(id);
      return new User(rs);
    } catch (SQLException ignored) {
      return null;
    }
  }

  @Test
  public void favoriteAddTest() throws TagNotFoundException {
    when(tagDao.getTagId("tag1", false)).thenReturn(Option.apply(2));
    userTagService.favoriteAdd(user, "tag1");
    verify(userTagDao).addTag(eq(1), eq(2), eq(true));
  }

  @Test
  public void favoriteDelTest() throws TagNotFoundException {
    userTagService.favoriteDel(user, "tag1");
    verify(userTagDao).deleteTag(eq(1), eq(2), eq(true));
  }

  @Test
  public void ignoreAddTest() throws TagNotFoundException {
    userTagService.ignoreAdd(user, "tag1");
    verify(userTagDao).addTag(eq(1), eq(2), eq(false));
  }

  @Test
  public void ignoreDelTest() throws TagNotFoundException {
    userTagService.ignoreDel(user, "tag1");
    verify(userTagDao).deleteTag(eq(1), eq(2), eq(false));
  }

  @Test
  public void favoritesGetTest() {
    ImmutableList.Builder<String> etalonBuild = ImmutableList.builder();
    etalonBuild.add("tag1");
    ImmutableList<String> etalon = etalonBuild.build();
    when(userTagDao.getTags(1, true)).thenReturn(etalon);

    List<String> actual = userTagService.favoritesGet(user);
    Assert.assertEquals(etalon.size(), actual.size());
    Assert.assertEquals(etalon.getFirst(), actual.getFirst());
  }

  @Test
  public void ignoresGetTest() {
    ImmutableList.Builder<String> etalonBuild = ImmutableList.builder();
    etalonBuild.add("tag1");
    ImmutableList<String> etalon = etalonBuild.build();
    when(userTagDao.getTags(1, false)).thenReturn(etalon);

    List<String> actual = userTagService.ignoresGet(user);
    Assert.assertEquals(etalon.size(), actual.size());
    Assert.assertEquals(etalon.getFirst(), actual.getFirst());
  }

  @Test
  public void getUserIdListByTagsTest() {
    List<Integer> etalon = new ArrayList<>();
    List<String> tags = new ArrayList<>();
    etalon.add(123);
    tags.add("tag1");
    when(userTagDao.getUserIdListByTags(1, List.of(2))).thenReturn(etalon);

    List<Integer> actual = userTagService.getUserIdListByTagsJava(user.getId(), tags);
    Assert.assertEquals(etalon.size(), actual.size());
    Assert.assertEquals(etalon.getFirst(), actual.getFirst());
  }

  @Test
  public void addMultiplyTagsTest() {
    UserTagService mockUserTagService = mock(UserTagService.class);
    when(mockUserTagService.addMultiplyTags(any(User.class), anyString(), anyBoolean())).thenCallRealMethod();
    try{
      doThrow(new TagNotFoundException()).when(mockUserTagService).favoriteAdd(eq(user), eq("uytutut"));
      doThrow(new DuplicateKeyException("duplicate")).when(mockUserTagService).favoriteAdd(eq(user), eq("tag3"));
    } catch (Exception e) {}

    List<String> strErrors = mockUserTagService.addMultiplyTags(user, "tag1, tag2, tag3, uytutut, @#$%$#", true);
    try{
      verify(mockUserTagService).favoriteAdd(eq(user), eq("tag1"));
      verify(mockUserTagService).favoriteAdd(eq(user), eq("tag2"));
      verify(mockUserTagService).favoriteAdd(eq(user), eq("uytutut"));
      verify(mockUserTagService, never()).favoriteAdd(eq(user), eq("@#$%$#"));
      verify(mockUserTagService, never()).ignoreAdd(any(User.class), anyString());
    } catch (Exception e) {}
    Assert.assertEquals(3, strErrors.size());

    reset(mockUserTagService);
    when(mockUserTagService.addMultiplyTags(any(User.class), anyString(), anyBoolean())).thenCallRealMethod();
    try{
      doThrow(new TagNotFoundException()).when(mockUserTagService).ignoreAdd(eq(user), eq("uytutut"));
      doThrow(new DuplicateKeyException("duplicate")).when(mockUserTagService).ignoreAdd(eq(user), eq("tag3"));
    } catch (Exception e) {}

    strErrors = mockUserTagService.addMultiplyTags(user, "tag1, tag2, tag3, uytutut, @#$%$#", false);
    try{
      verify(mockUserTagService).ignoreAdd(eq(user), eq("tag1"));
      verify(mockUserTagService).ignoreAdd(eq(user), eq("tag2"));
      verify(mockUserTagService).ignoreAdd(eq(user), eq("uytutut"));
      verify(mockUserTagService, never()).ignoreAdd(eq(user), eq("@#$%$#"));
      verify(mockUserTagService, never()).favoriteAdd(any(User.class), anyString());
    } catch (Exception e) {}
    Assert.assertEquals(3, strErrors.size());
  }

}
