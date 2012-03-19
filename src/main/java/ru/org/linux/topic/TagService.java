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

package ru.org.linux.topic;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import ru.org.linux.user.UserErrorException;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class TagService {
  private static final Pattern tagRE = Pattern.compile("([\\p{L}\\d \\+-]+)", Pattern.CASE_INSENSITIVE);

  @Autowired
  private TagDao tagDao;

  /**
   * Получить все тэги со счетчиком
   *
   * @return список всех тегов
   */
  public Map<String, Integer> getAllTags() {
    return tagDao.getAllTags();
  }

  /**
   * Получение идентификационного номера тега по названию. Тег должен использоваться.
   *
   * @param tag название тега
   * @return идентификационный номер
   * @throws UserErrorException
   * @throws TagNotFoundException
   */
  public int getTagId(String tag)
    throws UserErrorException, TagNotFoundException {
    checkTag(tag);
    return tagDao.getTagId(tag);
  }

  public void checkTag(String tag) throws UserErrorException {
    // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
    if (!isGoodTag(tag)) {
      throw new UserErrorException("Некорректный тег: '" + tag + '\'');
    }
  }

  public ImmutableList<String> parseTags(String tags) throws UserErrorException {
    Set<String> tagSet = new HashSet<String>();

    // Теги разделяютчя пайпом или запятой
    tags = tags.replaceAll("\\|", ",");
    String[] tagsArr = tags.split(",");

    if (tagsArr.length == 0) {
      return ImmutableList.of();
    }

    for (String aTagsArr : tagsArr) {
      String tag = StringUtils.stripToNull(aTagsArr.toLowerCase());
      // плохой тег - выбрасываем
      if (tag == null) {
        continue;
      }

      // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
      checkTag(tag);

      tagSet.add(tag);
    }

    return ImmutableList.copyOf(tagSet);
  }

  public ImmutableList<String> parseSanitizeTags(String tags) {
    if (tags == null) {
      return ImmutableList.of();
    }

    Set<String> tagSet = new HashSet<String>();

    // Теги разделяютчя пайпом или запятой
    tags = tags.replaceAll("\\|", ",");
    String[] tagsArr = tags.split(",");

    if (tagsArr.length == 0) {
      return ImmutableList.of();
    }

    for (String aTagsArr : tagsArr) {
      String tag = StringUtils.stripToNull(aTagsArr.toLowerCase());
      // плохой тег - выбрасываем
      if (tag == null) {
        continue;
      }

      // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
      if (isGoodTag(tag)) {
        tagSet.add(tag);
      }
    }

    return ImmutableList.copyOf(tagSet);
  }

  /**
   * Получить список наиболее популярных тегов.
   *
   * @return список наиболее популярных тегов
   */
  public SortedSet<String> getTopTags() {
    return tagDao.getTopTags();
  }

  /**
   * Получить все теги сообщения по идентификационному номеру сообщения.
   *
   * @param msgId идентификационный номер сообщения
   * @return все теги сообщения
   */
  public ImmutableList<String> getMessageTags(int msgId) {
    return tagDao.getMessageTags(msgId);
  }

  /**
   * Обновить список тегов сообщения по идентификационному номеру сообщения.
   *
   * @param msgId   идентификационный номер сообщения
   * @param tagList новый список тегов.
   * @return true если были произведены изменения
   */
  public boolean updateTags(final int msgId, final List<String> tagList) {
    return tagDao.updateTags(msgId, tagList);
  }

  /**
   * Обновить счётчики по тегам.
   *
   * @param oldTags список старых тегов
   * @param newTags список новых тегов
   */
  public void updateCounters(final List<String> oldTags, final List<String> newTags) {
    tagDao.updateCounters(oldTags, newTags);
  }

  /**
   * Получить уникальный список первых букв тегов.
   *
   * @return список первых букв тегов
   */
  public SortedSet<String> getFirstLetters(boolean skipEmptyUsages) {
    return tagDao.getFirstLetters(skipEmptyUsages);
  }

  /**
   * Получить список тегов по первому символу.
   *
   * @param firstLetter     первый символ
   * @param skipEmptyUsages пропускать ли неиспользуемые теги
   * @return список тегов по первому символу
   */
  public Map<String, Integer> getTagsByFirstLetter(String firstLetter, boolean skipEmptyUsages) {
    return tagDao.getTagsByFirstLetter(firstLetter, skipEmptyUsages);
  }

  /**
   * Изменить название существующего тега.
   *
   * @param oldTagName старое название тега
   * @param tagName    новое название тега
   * @param errors     обработчик ошибок ввода для формы
   */
  public void change(String oldTagName, String tagName, Errors errors) {
    // todo: Нельзя строить логику на исключениях. Это антипаттерн!
    try {
      checkTag(tagName);
      int oldTagId = tagDao.getTagIdByName(oldTagName);
      try {
        int tagId = tagDao.getTagIdByName(tagName);
        errors.rejectValue("tagName", "", "Тег с таким именем уже существует!");
      } catch (TagNotFoundException ignored) {
        tagDao.changeTag(oldTagId, tagName);
      }
    } catch (UserErrorException e) {
      errors.rejectValue("tagName", "", e.getMessage());
    } catch (TagNotFoundException e) {
      errors.rejectValue("tagName", "", "Тега с таким именем не существует!");
    }
  }

  public static String toString(Collection<String> tags) {
    if (tags.isEmpty()) {
      return "";
    }

    StringBuilder str = new StringBuilder();

    for (String tag : tags) {
      str.append(str.length() > 0 ? "," : "").append(tag);
    }

    return str.toString();
  }

  private boolean isGoodTag(String tag) {
    return tagRE.matcher(tag).matches();
  }

}
