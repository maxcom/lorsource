/*
 * Copyright 1998-2016 Linux.org.ru
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
 *//*
 * Copyright 1998-2016 Linux.org.ru
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
package ru.org.linux.user

import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.validation.{Errors, MapBindingResult}
import ru.org.linux.tag.{TagName, TagNotFoundException, TagService}

import scala.collection.JavaConverters._

@Service
class UserTagService(userTagDao: UserTagDao, tagService: TagService) {
  /**
    * Добавление тега к пользователю.
    *
    * @param user    объект пользователя
    * @param tagName название тега
    * @return идентификатор избранного тега
    */
  @throws[TagNotFoundException]
  def favoriteAdd(user: User, tagName: String): Int = {
    val tagId = tagService.getTagId(tagName)

    userTagDao.addTag(user.getId, tagId, true)

    tagId
  }

  /**
    * Удаление тега у пользователя.
    *
    * @param user    объект пользователя
    * @param tagName название тега
    * @return идентификатор тега
    */
  @throws[TagNotFoundException]
  def favoriteDel(user: User, tagName: String): Int = {
    val tagId = tagService.getTagId(tagName)

    userTagDao.deleteTag(user.getId, tagId, true)

    tagId
  }

  /**
    * Добавление игнорированного тега к пользователю.
    *
    * @param user    объект пользователя
    * @param tagName название тега
    */
  @throws[TagNotFoundException]
  def ignoreAdd(user: User, tagName: String): Int = {
    val tagId = tagService.getTagId(tagName)

    userTagDao.addTag(user.getId, tagService.getTagId(tagName), false)

    tagId
  }

  /**
    * Удаление игнорированного тега у пользователя.
    *
    * @param user    объект пользователя
    * @param tagName название тега
    */
  @throws[TagNotFoundException]
  def ignoreDel(user: User, tagName: String): Int = {
    val tagId = tagService.getTagId(tagName)

    userTagDao.deleteTag(user.getId, tagService.getTagId(tagName), false)

    tagId
  }

  /**
    * Получение списка тегов пользователя.
    *
    * @param user объект пользователя
    * @return список тегов пользователя
    */
  def favoritesGet(user: User): java.util.List[String] = {
    userTagDao.getTags(user.getId, true)
  }

  /**
    * Получение списка игнорированных тегов пользователя.
    *
    * @param user объект пользователя
    * @return список игнорированных тегов пользователя
    */
  def ignoresGet(user: User): java.util.List[String] = {
    userTagDao.getTags(user.getId, false)
  }

  /**
    * Получить список ID пользователей, у которых в профиле есть перечисленные фаворитные теги.
    *
    * @param userid id пользователя, которому не нужно слать оповещение
    * @param tags   список фаворитных тегов
    * @return список ID пользователей
    */
  def getUserIdListByTags(userid: Int, tags: java.util.List[String]): java.util.List[Integer] = {
    userTagDao.getUserIdListByTags(userid, tags)
  }

  /**
    * Проверяет, есть ли указанный фаворитный тег у пользователя.
    *
    * @param user    объект пользователя
    * @param tagName название тега
    * @return true если у пользователя есть тег
    */
  def hasFavoriteTag(user: User, tagName: String): Boolean = {
    favoritesGet(user).contains(tagName)
  }

  /**
    * Проверяет, есть ли указанный игнорируемый тег у пользователя.
    *
    * @param user    объект пользователя
    * @param tagName название тега
    * @return true если у пользователя есть тег
    */
  def hasIgnoreTag(user: User, tagName: String): Boolean = {
    ignoresGet(user).contains(tagName)
  }

  /**
    * Добавление нескольких тегов из строки. разделённых запятой.
    *
    * @param user       объект пользователя
    * @param tagsStr    строка, содержащая разделённые запятой теги
    * @param isFavorite добавлять фаворитные теги (true) или игнорируемые (false)
    * @return null если не было ошибок; строка если были ошибки при добавлении.
    */
  def addMultiplyTags(user: User, tagsStr: String, isFavorite: Boolean): java.util.List[String] = {
    val errors = new MapBindingResult(Map.empty.asJava, "")
    val tagList = TagName.parseAndValidateTags(tagsStr, errors, Integer.MAX_VALUE)

    for (tag <- tagList) {
      try {
        if (isFavorite) {
          favoriteAdd(user, tag)
        } else {
          ignoreAdd(user, tag)
        }
      } catch {
        case e: TagNotFoundException ⇒
          errors.reject(s"${e.getMessage}: '$tag'")
        case _: DuplicateKeyException ⇒
          errors.reject(s"Тег уже добавлен: '$tag")
      }
    }

    errorsToStringList(errors).asJava
  }

  def countFavs(id: Int): Int = userTagDao.countFavs(id)

  def countIgnore(id: Int): Int = userTagDao.countIgnore(id)

  /**
    * преобразование ошибок в массив строк.
    *
    * @param errors объект ошибок
    * @return массив строк, содержащий описания ошибок
    */
  private def errorsToStringList(errors: Errors): Seq[String] = {
    if (errors.hasErrors) {
      errors.getAllErrors.asScala.map(_.getCode)
    } else {
      Seq.empty
    }
  }
}