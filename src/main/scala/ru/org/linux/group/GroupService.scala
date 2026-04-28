/*
 * Copyright 1998-2026 Linux.org.ru
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
package ru.org.linux.group

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import org.springframework.stereotype.Service
import ru.org.linux.section.Section

import java.util.concurrent.TimeUnit
import scala.jdk.OptionConverters.*

@Service
class GroupService(groupDao: GroupDao):
  private val groupCache: Cache[Int, Group] =
    Caffeine.newBuilder().maximumSize(500).expireAfterWrite(5, TimeUnit.MINUTES).build()

  /** Получить объект группы по идентификатору.
    *
    * @param id
    *   идентификатор группы
    * @return
    *   объект группы
    * @throws GroupNotFoundException
    *   если группа не существует
    */
  def getGroup(id: Int): Group =
    val cached = groupCache.getIfPresent(id)
    if cached != null then
      cached
    else
      val group = groupDao.getGroup(id)
      groupCache.put(id, group)
      group

  /** Получить список групп в указанной секции.
    *
    * @param section
    *   объект секции.
    * @return
    *   список групп
    */
  def getGroups(section: Section): java.util.List[Group] = groupDao.getGroups(section)

  /** Получить объект группы в указанной секции по имени группы.
    *
    * @param section
    *   объект секции.
    * @param name
    *   имя группы
    * @return
    *   объект группы
    * @throws GroupNotFoundException
    *   если группа не существует
    */
  def getGroup(section: Section, name: String): Group =
    val group = groupDao.getGroup(section, name)
    groupCache.put(group.id, group)
    group

  /** Получить объект группы в указанной секции по имени группы.
    *
    * @param section
    *   объект секции.
    * @param name
    *   имя группы
    * @param allowNumber
    *   разрешить поиск по числовому id
    * @return
    *   объект группы (optional)
    */
  def getGroupOpt(section: Section, name: String, allowNumber: Boolean): Option[Group] =
    val group = groupDao.getGroupOpt(section, name, allowNumber).toScala
    group.foreach(g => groupCache.put(g.id, g))
    group

  /** Изменить настройки группы.
    *
    * @param group
    *   объект группы
    * @param title
    *   Заголовок группы
    * @param info
    *   дополнительная информация
    * @param longInfo
    *   расширенная дополнительная информация
    * @param resolvable
    *   можно ли ставить темам признак "тема решена"
    * @param urlName
    *   имя группы в URL
    */
  def setParams(
      group: Group,
      title: String,
      info: String,
      longInfo: String,
      resolvable: Boolean,
      urlName: String): Unit =
    groupDao.setParams(group, title, info, longInfo, resolvable, urlName)
    groupCache.invalidate(group.id)
