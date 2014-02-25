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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.WebDataBinder;
import scala.Option;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("unit-tests-context.xml")
public class TagModificationServiceTest {
  @Autowired
  private TagModificationService tagModificationService;

 @Autowired
  private TagDao tagDao;

  WebDataBinder binder;

  private static final Option<Object> noneInt = scala.Option.apply(null);

  @Before
  public void resetTagDaoMock() {
    reset(tagDao);
    when(tagDao.getTagId(anyString())).thenReturn(noneInt);
    when(tagDao.getTagId(anyString())).thenReturn(noneInt);
  }

  private void prepareChangeDataBinder() {
    TagRequest.Change tagRequestChange = new TagRequest.Change();
    binder = new WebDataBinder(tagRequestChange);
  }

  private void prepareDeleteDataBinder() {
    TagRequest.Delete tagRequestDelete = new TagRequest.Delete();
    binder = new WebDataBinder(tagRequestDelete);
  }

  @Test
  public void changeTest()
    throws Exception {
    when(tagDao.getTagId("testTag")).thenReturn(Option.apply((Object) 123));
    when(tagDao.getTagId("testNewTag")).thenReturn(Option.apply((Object) 456));

    prepareChangeDataBinder();
    tagModificationService.change("InvalidTestTag", "testNewTag", binder.getBindingResult());
    assertTrue(binder.getBindingResult().hasErrors());

    prepareChangeDataBinder();
    tagModificationService.change("testTag", "#$%@@#%$", binder.getBindingResult());
    assertTrue(binder.getBindingResult().hasErrors());

    prepareChangeDataBinder();
    tagModificationService.change("#$%@@#%$", "testNewTag", binder.getBindingResult());
    assertTrue(binder.getBindingResult().hasErrors());

    prepareChangeDataBinder();
    tagModificationService.change("testTag", "testNewTag", binder.getBindingResult());
    assertTrue(binder.getBindingResult().hasErrors());

    resetTagDaoMock();
    when(tagDao.getTagId("testTag")).thenReturn(Option.apply((Object) 123));
    when(tagDao.getTagId("testNewTag")).thenReturn(noneInt);
    prepareChangeDataBinder();
    tagModificationService.change("testTag", "testNewTag", binder.getBindingResult());
    assertFalse(binder.getBindingResult().hasErrors());
  }

  @Test
  public void deleteTest()
    throws Exception {

    when(tagDao.getTagId("testTag")).thenReturn(Option.apply((Object) 123));
    when(tagDao.getTagId("InvalidTestTag")).thenReturn(noneInt);

    prepareDeleteDataBinder();
    tagModificationService.delete("InvalidTestTag", "testNewTag", binder.getBindingResult());
    assertTrue(binder.getBindingResult().hasErrors());

    prepareDeleteDataBinder();
    tagModificationService.delete("testTag", "#$%@@#%$", binder.getBindingResult());
    assertTrue(binder.getBindingResult().hasErrors());

    prepareDeleteDataBinder();
    tagModificationService.delete("testTag", "testTag", binder.getBindingResult());
    assertTrue(binder.getBindingResult().hasErrors());

    prepareDeleteDataBinder();
    tagModificationService.delete("testTag", "testNewTag", binder.getBindingResult());
    assertFalse(binder.getBindingResult().hasErrors());
  }
}
