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
import org.testng.annotations.Test;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TagService_getOrCreateTagTest extends TagServiceTestBase {

  private final String tagName = "tag name";

  @Test
  public void tagAlreadyExists()
    throws TagNotFoundException {
    // given
    final int expectedTagId = 123;
    when(tagDao.getTagId(tagName))
      .thenReturn(expectedTagId);

    // when
    int actualTagId = tagService.getOrCreateTag(tagName);

    // then
    Assert.assertEquals(actualTagId, expectedTagId);

    verify(tagDao).getTagId(tagName);
    verifyNoMoreInteractions(tagDao);
  }

  @Test
  public void tagNotFoundAndCanNotCreated()
    throws TagNotFoundException {
    // given
    when(tagDao.getTagId(tagName))
      .thenThrow(new TagNotFoundException())
      .thenThrow(new TagNotFoundException());

    // when
    int actualTagId = tagService.getOrCreateTag(tagName);

    // then
    Assert.assertEquals(actualTagId, 0);

    verify(tagDao, times(2)).getTagId(tagName);
    verify(tagDao).createTag(tagName);
    verifyNoMoreInteractions(tagDao);
  }

  @Test
  public void tagShouldBeCreated()
    throws TagNotFoundException {
    // given
    final int expectedTagId = 123;
    when(tagDao.getTagId(tagName))
      .thenThrow(new TagNotFoundException())
      .thenReturn(expectedTagId);

    // when
    int actualTagId = tagService.getOrCreateTag(tagName);

    // then
    Assert.assertEquals(actualTagId, expectedTagId);

    verify(tagDao, times(2)).getTagId(tagName);
    verify(tagDao).createTag(tagName);
    verifyNoMoreInteractions(tagDao);
  }
}
