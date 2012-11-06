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

import org.mockito.ArgumentCaptor;
import org.springframework.validation.Errors;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TagService_createTest extends TagServiceTestBase {

  private final String newTagName = "new tag";
  private Errors errors;

  @BeforeMethod
  public void setUp() {
    errors = mock(Errors.class);
  }

  @Test
  public void createNewTagWithoutChecksForExists() {
    // given

    // when
    tagService.create(newTagName);

    // then
    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(tagDao).createTag(argumentCaptor.capture());
    Assert.assertEquals(argumentCaptor.getValue(), newTagName);

    verifyNoMoreInteractions(tagDao);
  }

  @Test
  public void tagAlreadyExists()
  throws TagNotFoundException {
    // given
    when(tagDao.getTagId(newTagName))
    .thenReturn(1);

    // when
    tagService.create(newTagName, errors);

    // then
    verify(errors).rejectValue("tagName", "", "Тег с таким именем уже существует!");
    verify(tagDao).getTagId(newTagName);
    verifyNoMoreInteractions(
      tagDao,
      errors
    );
  }

  @Test
  public void createTagWithoutIssues()
    throws TagNotFoundException {
    // given
    when(tagDao.getTagId(newTagName))
      .thenThrow(new TagNotFoundException());

    // when
    tagService.create(newTagName, errors);

    // then
    verify(tagDao).getTagId(newTagName);
    verify(tagDao).createTag(newTagName);
    verifyNoMoreInteractions(
      tagDao,
      errors
    );
  }

}
