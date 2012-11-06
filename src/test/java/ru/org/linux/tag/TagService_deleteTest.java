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

/**
 * Created with IntelliJ IDEA.
 * User: slavaz
 * Date: 04.11.12
 * Time: 1:22
 */
public class TagService_deleteTest extends TagServiceTestBase {
  private Errors errors;
  private final String deletedTagName = "deleted tag";
  private final String tagNameForReplace = "replaced tag name";

  @BeforeMethod
  public void setUp() {
    errors = mock(Errors.class);
  }

  @Test
  public void deletedTagNotFound()
    throws TagNotFoundException {
    // given

    when(tagDao.getTagId(deletedTagName))
      .thenThrow(new TagNotFoundException());

    // when
    tagService.delete(deletedTagName, tagNameForReplace, errors);

    // then
    verify(errors).rejectValue("tagName", "", "Тега с таким именем не существует!");
    verify(tagDao).getTagId(deletedTagName);
    verifyNoMoreInteractions(
      tagDao,
      errors
    );
  }

  @Test
  public void deletedTagNameAndReplacedTagNameAreEquals()
    throws TagNotFoundException {
    // given

    // when
    tagService.delete(deletedTagName, deletedTagName, errors);

    // then
    verify(errors).rejectValue("tagName", "", "Заменяемый тег не должен быть равен удаляемому!");
    verify(tagDao).getTagId(deletedTagName);
    verifyNoMoreInteractions(
      tagDao,
      errors
    );
  }

  @Test
  public void tagNameForReplaceIsEmpty()
    throws TagNotFoundException {
    // given
    ITagActionHandler actionHandler = mock(ITagActionHandler.class);
    tagService.getActionHandlers().add(actionHandler);

    when(tagDao.getTagId(deletedTagName))
      .thenReturn(123);

    // when
    tagService.delete(deletedTagName, null, errors);

    // then
    verify(tagDao).getTagId(deletedTagName);
    verify(tagDao).deleteTag(123);
    verify(actionHandler).deleteTag(123, deletedTagName);
    verifyNoMoreInteractions(
      tagDao,
      errors,
      actionHandler
    );
  }

  @Test
  public void deleteTagWithReplacement()
   throws TagNotFoundException {
    // given
    when(tagDao.getTagId(deletedTagName))
      .thenReturn(123);
    when(tagDao.getTagId(tagNameForReplace))
      .thenReturn(456);

    ITagActionHandler actionHandler = mock(ITagActionHandler.class);
    tagService.getActionHandlers().add(actionHandler);

    // when
    tagService.delete(deletedTagName, tagNameForReplace, errors);

    // then
    verify(tagDao).getTagId(deletedTagName);
    verify(tagDao).getTagId(tagNameForReplace);
    verify(tagDao).deleteTag(123);
    verify(actionHandler).deleteTag(123, deletedTagName);
    verify(actionHandler).replaceTag(123, deletedTagName, 456, tagNameForReplace);
    verifyNoMoreInteractions(
      tagDao,
      errors,
      actionHandler
    );
  }
}
