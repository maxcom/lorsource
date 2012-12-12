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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import ru.org.linux.rest.Constants;
import ru.org.linux.section.Section;

import java.util.ArrayList;
import java.util.List;

/**
 * REST-API декоратор для вывода информации по одной секции.
 */
public class SectionDecorator {
  private final Section section;

  /**
   * конструктор с объектом секции в качестве параметра.
   *
   * @param section объект секции
   */
  public SectionDecorator(Section section) {
    this.section = section;
  }

  /**
   * Уникальный идентификатор секции.
   *
   * @return идентификатор
   */
  public Integer getId() {
    return (section != null) ? section.getId() : null;
  }

  /**
   * Краткое обозначение раздела.
   * (напр. general)
   *
   * @return алиас
   */
  public String getAlias() {
    return (section != null) ? section.getName() : null;
  }

  /**
   * Название раздела.
   * (напр. общий форум для технических вопросов, не подходящих в другие группы)
   *
   * @return название раздела
   */
  public String getTitle() {
    return (section != null) ? section.getTitle() : null;
  }

  /**
   * Линк на подробности по секции.
   *
   * @return URL
   */
  public String getLink() {
    return Constants.URL_SECTION + "/" + String.valueOf(section.getId());
  }

  /**
   * Преобразование списка секций в декорируемый список
   *
   * @param sectionList
   * @return
   */
  public static List<SectionDecorator> getSections(List<Section> sectionList) {
    List<SectionDecorator> decoratorList = new ArrayList<SectionDecorator>();
    for (Section section : sectionList) {
      decoratorList.add(new SectionDecorator(section));
    }
    return decoratorList;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SectionDecorator that = (SectionDecorator) o;

    return new EqualsBuilder()
            .append(getId(), that.getId())
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
            .append(getId())
            .toHashCode();
  }
}
