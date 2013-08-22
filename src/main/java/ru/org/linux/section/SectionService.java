/*
 * Copyright 1998-2013 Linux.org.ru
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

package ru.org.linux.section;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class SectionService {
  private ImmutableList<Section> sectionList;

  @Autowired
  private SectionDao sectionDao;

  /**
   * Инициализация списка секций из БД.
   * Метод вызывается автоматически сразу после создания бина.
   */
  @PostConstruct
  private void initializeSectionList() {
    Builder<Section> sectionListBuilder = ImmutableList.builder();
    List<Section> sections = sectionDao.getAllSections();
    sectionListBuilder.addAll(sections);
    sectionList = sectionListBuilder.build();
  }

  /**
   * Получить идентификатор секции по url-имени.
   *
   * @param sectionName название секции
   * @return идентификатор секции
   * @throws SectionNotFoundException если секция не найдена
   */
  public Section getSectionByName(String sectionName) throws SectionNotFoundException {
    for (Section section : sectionList) {
      if (section.getUrlName().equals(sectionName)) {
        return section;
      }
    }

    throw new SectionNotFoundException();
  }

  /**
   * Получить объект секции по идентификатору секции.
   *
   * @param sectionId идентификатор секции
   * @return объект секции
   * @throws SectionNotFoundException если секция не найдена
   */
  @Nonnull
  public Section getSection(int sectionId) throws SectionNotFoundException {
    for (Section section : sectionList) {
      if (section.getId() == sectionId) {
        return section;
      }
    }
    throw new SectionNotFoundException(sectionId);
  }

  /**
   * получить список секций.
   *
   * @return список секций
   */
  public ImmutableList<Section> getSectionList() {
    return sectionList;
  }

  /**
   * Получить расширенную информацию о секции по идентификатору секции.
   *
   * @param id идентификатор секции
   * @return расширеннуя информация о секции
   */
  public String getAddInfo(int id) {
    return sectionDao.getAddInfo(id);
  }

  /**
   * Получить тип "листания" между страницами.
   *
   * @param sectionId  идентификатор секции
   * @return тип "листания" между страницами
   * @throws SectionNotFoundException
   */
  public SectionScrollModeEnum getScrollMode(int sectionId)
    throws SectionNotFoundException {
    Section section = getSection(sectionId);
    return section.getScrollMode();
  }
}
