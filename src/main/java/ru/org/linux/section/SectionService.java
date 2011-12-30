/*
 * Copyright 1998-2011 Linux.org.ru
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class SectionService {
  private ImmutableList<Section> sectionList;

  @Autowired
  SectionDao sectionDao;

  /**
   * Инициализация списка секций из БД.
   * Метод вызывается автоматически сразу после создания бина.
   */
  @PostConstruct
  private void initializeSectionList() {
    ImmutableList.Builder<Section> sectionListBuilder = ImmutableList.builder();
    List<SectionDto> sectionDtoList = sectionDao.getAllSections();
    for (SectionDto sectionDto: sectionDtoList) {
      Section section = new Section(sectionDto);
      sectionListBuilder.add(section);
    }
    sectionList = sectionListBuilder.build();
  }

  /**
   * Получить идентификатор секции по названию.
   *
   * @param SectionName название секции
   * @return идентификатор секции
   * @throws SectionNotFoundException если секция не найдена
   */
  public int getSectionIdByName(String SectionName) throws SectionNotFoundException {
    if (sectionList == null) {
      initializeSectionList();
    }
    for (Section section : sectionList) {
      if (section.getTitle().equals(SectionName)) {
        return section.getId();
      }
    }
    throw new SectionNotFoundException();
  }

  /**
   * Получить объект секции по идентификатору секции.
   *
   * @param SectionId идентификатор секции
   * @return объект секции
   * @throws SectionNotFoundException если секция не найдена
   */
  public Section getSection(int SectionId) throws SectionNotFoundException {
    for (Section section : sectionList) {
      if (section.getId() == SectionId) {
        return section;
      }
    }
    throw new SectionNotFoundException(SectionId);
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
