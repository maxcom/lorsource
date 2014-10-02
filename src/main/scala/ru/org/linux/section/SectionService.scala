/*
 * Copyright 1998-2014 Linux.org.ru
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
package ru.org.linux.section

import javax.annotation.PostConstruct

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import scala.collection.JavaConversions._

@Service
class SectionService {
  @Autowired
  private var sectionDao: SectionDao = null

  private var sectionList: Seq[Section] = null

  /**
   * Инициализация списка секций из БД.
   * Метод вызывается автоматически сразу после создания бина.
   */
  @PostConstruct
  private def initializeSectionList:Unit = {
    sectionList = sectionDao.getAllSections.toVector
  }

  /**
   * Получить идентификатор секции по url-имени.
   *
   * @param sectionName название секции
   * @return идентификатор секции
   * @throws SectionNotFoundException если секция не найдена
   */
  def getSectionByName(sectionName: String): Section =
    sectionList.find(_.getUrlName == sectionName).getOrElse(throw new SectionNotFoundException)

  /**
   * Получить объект секции по идентификатору секции.
   *
   * @param sectionId идентификатор секции
   * @return объект секции
   * @throws SectionNotFoundException если секция не найдена
   */
  def getSection(sectionId: Int): Section =
    sectionList.find(_.getId == sectionId).getOrElse(throw new SectionNotFoundException)

  /**
   * получить список секций.
   *
   * @return список секций
   */
  def getSectionList: java.util.List[Section] = sectionList

  /**
   * Получить расширенную информацию о секции по идентификатору секции.
   *
   * @param id идентификатор секции
   * @return расширеннуя информация о секции
   */
  def getAddInfo(id: Int): String = sectionDao.getAddInfo(id)

  /**
   * Получить тип "листания" между страницами.
   *
   * @param sectionId  идентификатор секции
   * @return тип "листания" между страницами
   * @throws SectionNotFoundException
   */
  def getScrollMode(sectionId: Int): SectionScrollModeEnum = getSection(sectionId).getScrollMode
}