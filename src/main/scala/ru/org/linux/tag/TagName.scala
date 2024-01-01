/*
 * Copyright 1998-2024 Linux.org.ru
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

package ru.org.linux.tag

import org.springframework.validation.Errors
import ru.org.linux.user.UserErrorException

import scala.jdk.CollectionConverters.*

object TagName {
  val MaxTagsPerTopic = 5
  private val MinTagLength = 1
  private val MaxTagLength = 32

  private val tagRE = """(?i)([\p{L}\d-](?:[.\p{L}\d \+-]*[\p{L}\d\+-])?)""".r.pattern

  def isGoodTag(tag: String): Boolean = {
    tagRE.matcher(tag).matches() && tag.length() >= MinTagLength && tag.length() <= MaxTagLength
  }

  @throws[UserErrorException]
  def checkTag(tag:String): Unit = {
    // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
    if (!isGoodTag(tag)) {
      throw new UserErrorException(s"Некорректный тег: '$tag'")
    }
  }

  def parseTags(tags: String): Set[String] = {
    if (tags == null) {
      Set.empty[String]
    } else {
      // Теги разделяютчя пайпом или запятой
      val tagsArr = tags.replaceAll("\\|", ",").split(",")

      val tagSet: Set[String] = tagsArr.filterNot(_.trim.isEmpty).map(_.toLowerCase.trim).toSet

      tagSet
    }
  }

  /**
   * Разбор строки тегов. Error при ошибках
   *
   * @param tags   список тегов через запятую
   * @param errors класс для ошибок валидации (параметр 'tags')
   * @return список тегов
   */
  def parseAndValidateTags(tags: String, errors:Errors, maxTags: Int): Seq[String] = {
    val (goodTags, badTags) = parseTags(tags).partition(isGoodTag)

    for (tag <- badTags) {
      // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
      if (tag.length() > MaxTagLength) {
        errors.rejectValue("tags", null, "Слишком длинный тег: '" + tag + "\' (максимум " + MaxTagLength + " символов)")
      } else if (!isGoodTag(tag)) {
        errors.rejectValue("tags", null, "Некорректный тег: '" + tag + '\'')
      }
    }

    if (goodTags.size > maxTags) {
      errors.rejectValue("tags", null, "Слишком много тегов (максимум " + maxTags + ')')
    }

    goodTags.toVector
  }

  /**
   * Разбор строки тегов. Игнорируем некорректные теги
   *
   * @param tags список тегов через запятую
   * @return список тегов
   */
  def parseAndSanitizeTagsJava(tags:String): java.util.List[String] =
    parseAndSanitizeTags(tags).asJava

  def parseAndSanitizeTags(tags: String): Seq[String] =
    parseTags(tags).filter(isGoodTag).toVector
}
