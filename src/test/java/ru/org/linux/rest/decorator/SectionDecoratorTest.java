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

package ru.org.linux.rest.decorator;

import junit.framework.Assert;
import org.junit.Test;

import ru.org.linux.section.Section;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для SectionDecorator
 */
public class SectionDecoratorTest {

  @Test
  public void sectionIsNull() {
    // given

    // when
    SectionDecorator sectionDecorator = new SectionDecorator(null);

    // then
    Assert.assertEquals(null, sectionDecorator.getId());
    Assert.assertEquals(null, sectionDecorator.getAlias());
    Assert.assertEquals(null, sectionDecorator.getTitle());
  }

  @Test
  public void checkSectionDecor() {
    // given
    Section section = mock(Section.class);
    when(section.getId()).thenReturn(12345);
    when(section.getName()).thenReturn("alias");
    when(section.getTitle()).thenReturn("some long title");

    // when
    SectionDecorator sectionDecorator = new SectionDecorator(section);

    // then
    Assert.assertEquals(new Integer(12345), sectionDecorator.getId());
    Assert.assertEquals("alias", sectionDecorator.getAlias());
    Assert.assertEquals("some long title", sectionDecorator.getTitle());

    //verify

    verify(section).getId();
    verify(section).getName();
    verify(section).getTitle();
    verifyNoMoreInteractions(section);
  }


  @Test
  public void checkLink() {
    // given
    Section section = mock(Section.class);
    when(section.getId()).thenReturn(12345);

    // when
    SectionDecorator sectionDecorator = new SectionDecorator(section);

    // then
    Assert.assertEquals("/api/sections/12345", sectionDecorator.getLink());

    //verify
    verify(section).getId();
    verifyNoMoreInteractions(section);
  }

  @Test
  public void checkArrayConverter() {
    // given
    List<Section> inputList = new ArrayList<Section>();

    Section section1 = mock(Section.class);
    when(section1.getId()).thenReturn(12345);
    inputList.add(section1);

    Section section2 = mock(Section.class);
    when(section2.getId()).thenReturn(67890);
    inputList.add(section2);

    // when
    List<SectionDecorator> actualList = SectionDecorator.getSections(inputList);

    // then
    Assert.assertEquals(2, actualList.size());

    List<SectionDecorator> expectedList = new ArrayList<SectionDecorator>();

    SectionDecorator decorator1 = mock(SectionDecorator.class);
    when(decorator1.getId()).thenReturn(12345);
    expectedList.add(decorator1);

    SectionDecorator decorator2 = mock(SectionDecorator.class);
    when(decorator2.getId()).thenReturn(67890);
    expectedList.add(decorator2);

    Assert.assertEquals(expectedList.get(0).getId(), actualList.get(0).getId());
    Assert.assertEquals(expectedList.get(1).getId(), actualList.get(1).getId());
  }

}
