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

import org.springframework.validation.Errors;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TagService_changeTest extends TagServiceTestBase {
  private Errors errors;
  private final String newTagName = "new tag";
  private final String oldTagName = "old tag";

  @BeforeMethod
  public void setUp() {
    errors = mock(Errors.class);
  }

  @Test
  public void oldTagNotFound()
    throws TagNotFoundException {
    // given
    when(tagDao.getTagId(oldTagName))
      .thenThrow(new TagNotFoundException());

    // when
    tagService.change(oldTagName, newTagName, errors);

    // then
    verify(errors).rejectValue("tagName", "", "Тега с таким именем не существует!");
    verify(tagDao).getTagId(oldTagName);
    verifyNoMoreInteractions(
      tagDao,
      errors
    );
  }

  @Test
  public void newTagAlreadyExists()
    throws TagNotFoundException {
    // given
    when(tagDao.getTagId(oldTagName))
      .thenReturn(1);
    when(tagDao.getTagId(newTagName))
      .thenReturn(2);

    // when
    tagService.change(oldTagName, newTagName, errors);

    // then
    verify(errors).rejectValue("tagName", "", "Тег с таким именем уже существует!");
    verify(tagDao).getTagId(newTagName);
    verify(tagDao).getTagId(oldTagName);
    verifyNoMoreInteractions(
      tagDao,
      errors
    );
  }

  @Test
  public void changeTagWithoutIssues()
    throws TagNotFoundException {
    // given

    when(tagDao.getTagId(oldTagName))
      .thenReturn(1);

    when(tagDao.getTagId(newTagName))
      .thenThrow(new TagNotFoundException());

    // when
    tagService.change(oldTagName, newTagName, errors);

    // then
    verify(tagDao).getTagId(newTagName);
    verify(tagDao).getTagId(oldTagName);
    verify(tagDao).changeTag(1, newTagName);
    verifyNoMoreInteractions(
      tagDao,
      errors
    );
  }

}
