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

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ru.org.linux.user.UserErrorException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TagServiceTest extends TagServiceTestBase {

  @Test
  public void getAllTags() {
    // given
    Map<String, Integer> expectedTagMap = mock(Map.class);

    when(tagDao.getAllTags())
      .thenReturn(expectedTagMap);

    // when
    Map<String, Integer> actualTagMap = tagService.getAllTags();

    // then
    Assert.assertSame(actualTagMap, expectedTagMap);

    verify(tagDao).getAllTags();
    verifyNoMoreInteractions(tagDao);
  }

  @Test(expectedExceptions = {TagNotFoundException.class})
  public void getTagIdByWrongName() throws TagNotFoundException, UserErrorException {
    // given
    when(tagDao.getTagId(anyString(), anyBoolean()))
      .thenThrow(new TagNotFoundException());

    // when
    tagService.getTagId("blabla");

    // then
  }

  @Test
  public void getTagIdByName() throws TagNotFoundException, UserErrorException {
    // given
    final int expectedTagId = 123;
    final String tagName = "blabla";
    when(tagDao.getTagId(eq(tagName), anyBoolean()))
      .thenReturn(expectedTagId);

    // when
    int actualTagId = tagService.getTagId(tagName);

    // then
    Assert.assertEquals(actualTagId, expectedTagId);

    verify(tagDao).getTagId(eq(tagName), anyBoolean());
    verifyNoMoreInteractions(tagDao);
  }


  @Test(
    dataProvider = "isGoodTagDataSource"
  )
  public void isGoodTag(
    String inputTagName,
    boolean expectedResult
  ) {
    // given

    // when
    boolean actualResult = TagService.isGoodTag(inputTagName);

    // then
    Assert.assertEquals(actualResult, expectedResult);
  }

  @DataProvider(name = "isGoodTagDataSource")
  public Object[][] isGoodTagDataSource() {
    return new Object[][]{
      // невалидные варианты
      new Object[]{null, false},
      new Object[]{"", false},
      new Object[]{"1", false},
      new Object[]{"##%#", false},
      new Object[]{"some&tar$name", false},
      new Object[]{"123456789012345678901234567890", false},
      new Object[]{"!@#$%^&*()`\"|\\][{}", false},
      new Object[]{":;'/,<>?`~*'", false},

      // валидные варианты
      new Object[]{"tag", true},
      new Object[]{"Русский Тег", true},
      new Object[]{"a+s-d.f 12344567890", true}
    };
  }

  @Test(dataProvider = "parseSanitizeTagsDataSource")
  public void parseSanitizeTags(
    String inputTagString,
    List<String> expectedTagList
  ) {
    // given

    // when
    List<String> actualTagList = tagService.parseSanitizeTags(inputTagString);

    // then
    Assert.assertEqualsNoOrder(
      actualTagList.toArray(),
      expectedTagList.toArray()
    );
  }

  @DataProvider(name = "parseSanitizeTagsDataSource")
  public Object[][] parseSanitizeTagsDataSource() {

    List<String> etalonTagList = new ArrayList<String>();
    etalonTagList.add("tag1");
    etalonTagList.add("tag2");
    etalonTagList.add("tag3");

    return new Object[][] {
      // при null возврат пустого списка
      new Object[] {null, new ArrayList<String>()},
      // при пустой строке возврат пустого списка
      new Object[] {"", new ArrayList<String>()},
      // правильная строка тегов
      new Object[] {"tag1, tag2,tag3", etalonTagList},
      // разделение пайпой равнозначно разделению запятой
      new Object[] {"tag1| tag2|tag3", etalonTagList},
      // выбрасываем пустые теги
      new Object[] {",,,tag1,,,tag2,,,,tag3,,,", etalonTagList},
      // выбрасываем плохие теги
      new Object[] {"1,@#,%$,tag1,*^,:>;,tag2,%**,**,||%$,tag3", etalonTagList},
    };
  }

  @Test
  public void getTopTags() {
    // given
    SortedSet<String> expectedTagMap = mock(SortedSet.class);

    when(tagDao.getTopTags())
      .thenReturn(expectedTagMap);

    // when
    SortedSet<String> actualTagMap = tagService.getTopTags();

    // then
    Assert.assertSame(actualTagMap, expectedTagMap);

    verify(tagDao).getTopTags();
    verifyNoMoreInteractions(tagDao);
  }

  @Test
  public void getFirstLetters() {
    // given
    SortedSet<String> expectedTagMap = mock(SortedSet.class);

    when(tagDao.getFirstLetters())
      .thenReturn(expectedTagMap);

    // when
    SortedSet<String> actualTagMap = tagService.getFirstLetters();

    // then
    Assert.assertSame(actualTagMap, expectedTagMap);

    verify(tagDao).getFirstLetters();
    verifyNoMoreInteractions(tagDao);
  }


  @Test
  public void getTagsByFirstLetter() {
    // given
    Map<String, Integer> expectedTagMap = mock(Map.class);

    final String tagName = "tag1";
    when(tagDao.getTagsByFirstLetter(tagName))
      .thenReturn(expectedTagMap);

    // when
    Map<String, Integer> actualTagMap = tagService.getTagsByFirstLetter(tagName);

    // then
    Assert.assertSame(actualTagMap, expectedTagMap);

    verify(tagDao).getTagsByFirstLetter(tagName);
    verifyNoMoreInteractions(tagDao);
  }

  @Test(dataProvider = "toStringTagListDataSource")
  public void toStringTagList(
    Collection<String> inputTagList,
    String expectedTagString
  ) {
    // given

    // when
    String actualTagString = TagService.toString(inputTagList);

    // then
    Assert.assertEquals(actualTagString, expectedTagString);
  }
  @DataProvider(name = "toStringTagListDataSource")
  public Object[][] toStringTagListDataSource() {
    List<String> expectedTagList = new ArrayList<String>();
    expectedTagList.add("tag1");
    expectedTagList.add("tag2");
    expectedTagList.add("tag3");

    return new Object[][] {
      new Object[] {new ArrayList<String>(), ""},
      new Object[] {expectedTagList, "tag1,tag2,tag3"},
    };
  }

  @Test
  public void reCalculateAllCounters() {
    // given
    ITagActionHandler actionHandler = mock(ITagActionHandler.class);
    tagService.getActionHandlers().add(actionHandler);

    // when
    tagService.reCalculateAllCounters();
    // then

    verify(actionHandler).reCalculateAllCounters();
    verifyNoMoreInteractions(
      tagDao,
      actionHandler
    );

  }

  @Test
  public void getCounter()
    throws TagNotFoundException {
    // given
    final String tagName = "tag name";
    final int tagId = 123;
    when(tagDao.getTagId(tagName))
      .thenReturn(tagId);
    when(tagDao.getCounter(tagId))
      .thenReturn(456);

    // when
    int actualCounter = tagService.getCounter(tagName);

    // then

    Assert.assertEquals(actualCounter, 456);
    verify(tagDao).getTagId(tagName);
    verify(tagDao).getCounter(tagId);
    verifyNoMoreInteractions(tagDao);
  }
}
