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

import junit.framework.Assert;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TagService_updateCountersTest extends TagServiceTestBase {

  private static interface TagDaoVerifyHelper {
    void doVerify(ArgumentCaptor<Integer> argumentCaptor, Integer expectedCallCounter)
      throws TagNotFoundException;
  }

  @BeforeMethod
  public void initTagDaoGetTagId() throws TagNotFoundException {
    when(tagDao.getTagId("tag1"))
      .thenReturn(1)
      .thenReturn(1);

    when(tagDao.getTagId("tag2"))
      .thenReturn(2)
      .thenReturn(2);

    when(tagDao.getTagId("tag3"))
      .thenReturn(3)
      .thenReturn(3);

    when(tagDao.getTagId("tag4"))
      .thenReturn(4)
      .thenReturn(4);
  }

  @Test(dataProvider = "updateCountersDataSource")
  public void updateCounters(
    List<String> inputOldTags,
    List<String> inputNewTags,
    Integer expectedCallCounter,
    List<Integer> expectedTagIdList,
    TagDaoVerifyHelper tagDaoVerifyHelper
  ) throws TagNotFoundException {
    // given

    // when
    tagService.updateCounters(inputOldTags, inputNewTags);

    // then

    // перехватываем вызова по увеличению счётчика
    ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.class);
    tagDaoVerifyHelper.doVerify(argumentCaptor, expectedCallCounter);

    // получаем список tagId, которые были предоставлены счётчику
    List<Integer> tagIdList = argumentCaptor.getAllValues();

    // в списке должны быть только ожидаемые ID тегов
    Assert.assertEquals(tagIdList, expectedTagIdList);

    // больше вызовов tagDao не должно быть
    verify(tagDao, times(expectedCallCounter)).getTagId(anyString());
    verifyNoMoreInteractions(tagDao);
  }

  @DataProvider(name = "updateCountersDataSource")
  public Object[][] updateCountersDataSource() {

    // ---------------------------------------------------------------
    // Первый тестовый случай. В списке новых тегов на два тега больше.
    // в списке старых тегов есть tag1 и tag2
    List<String> oldTags1 = new LinkedList<String>();
    oldTags1.add("tag1");
    oldTags1.add("tag2");

    //в списке новых тегов есть  tag1, tag2, tag3 и tag4
    List<String> newTags1 = new LinkedList<String>();
    newTags1.add("tag1");
    newTags1.add("tag2");
    newTags1.add("tag3");
    newTags1.add("tag4");

    // ожидаемое количество вызовов по приращению счётчика
    Integer expectedCallCounter1 = 2;

    // ожидаемый список перехваченных ID тегов
    // // в списке должны быть только  3 и 4
    List expectedTagIdList1 = new LinkedList();
    expectedTagIdList1.add(3);
    expectedTagIdList1.add(4);

    // помощник проверки вызовов tagDao для данного конкретного тестового случая
    TagDaoVerifyHelper tagDaoVerifyHelper1 = new TagDaoVerifyHelper() {
      @Override
      public void doVerify(ArgumentCaptor<Integer> argumentCaptor, Integer expectedCallCounter)
        throws TagNotFoundException {
        verify(tagDao, times(expectedCallCounter)).increaseCounterById(argumentCaptor.capture(), anyInt());
      }
    };

    // ---------------------------------------------------------------
    // Второй тестовый случай. В списке старых тегов на два тега больше.
    // в списке старых тегов есть tag1, tag2, tag3 и tag4
    List<String> oldTags2 = new LinkedList<String>();
    oldTags2.add("tag1");
    oldTags2.add("tag2");
    oldTags2.add("tag3");
    oldTags2.add("tag4");

    //в списке новых тегов есть  tag1 и tag3
    List<String> newTags2 = new LinkedList<String>();
    newTags2.add("tag1");
    newTags2.add("tag3");

    // ожидаемое количество вызовов по приращению счётчика
    Integer expectedCallCounter2 = 2;

    // ожидаемый список перехваченных ID тегов
    // // в списке должны быть только  3 и 4
    List expectedTagIdList2 = new LinkedList();
    expectedTagIdList2.add(2);
    expectedTagIdList2.add(4);

    // помощник проверки вызовов tagDao для данного конкретного тестового случая
    TagDaoVerifyHelper tagDaoVerifyHelper2 = new TagDaoVerifyHelper() {
      @Override
      public void doVerify(ArgumentCaptor<Integer> argumentCaptor, Integer expectedCallCounter)
        throws TagNotFoundException {
        verify(tagDao, times(expectedCallCounter)).decreaseCounterById(argumentCaptor.capture(), anyInt());
      }
    };

    // ---------------------------------------------------------------
    // Третий тестовый случай. Добавился тег test4 и убрался тег test1
    // в списке старых тегов есть tag1 и tag2
    List<String> oldTags3 = new LinkedList<String>();
    oldTags3.add("tag1");
    oldTags3.add("tag2");
    oldTags3.add("tag3");

    //в списке новых тегов есть  tag1, tag2, tag3 и tag4
    List<String> newTags3 = new LinkedList<String>();
    newTags3.add("tag2");
    newTags3.add("tag3");
    newTags3.add("tag4");

    // ожидаемое количество вызовов по приращению счётчика
    Integer expectedCallCounter3 = 2;

    // ожидаемый список перехваченных ID тегов
    // // в списке должны быть только  1 и 4
    List expectedTagIdList3 = new LinkedList();
    expectedTagIdList3.add(4);
    expectedTagIdList3.add(1);

    // помощник проверки вызовов tagDao для данного конкретного тестового случая
    TagDaoVerifyHelper tagDaoVerifyHelper3 = new TagDaoVerifyHelper() {
      @Override
      public void doVerify(ArgumentCaptor<Integer> argumentCaptor, Integer expectedCallCounter)
        throws TagNotFoundException {
        verify(tagDao).increaseCounterById(argumentCaptor.capture(), anyInt());
        verify(tagDao).decreaseCounterById(argumentCaptor.capture(), anyInt());
      }
    };
    // ---------------------------------------------------------------

    return new Object[][]{
      new Object[]{oldTags1, newTags1, expectedCallCounter1, expectedTagIdList1, tagDaoVerifyHelper1},
      new Object[]{oldTags2, newTags2, expectedCallCounter2, expectedTagIdList2, tagDaoVerifyHelper2},
      new Object[]{oldTags3, newTags3, expectedCallCounter3, expectedTagIdList3, tagDaoVerifyHelper3},
    };
  }

}
