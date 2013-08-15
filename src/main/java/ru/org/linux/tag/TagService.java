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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import ru.org.linux.user.UserErrorException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class TagService {
  private static final Log logger = LogFactory.getLog(TagService.class);

  private static final Pattern tagRE = Pattern.compile("([\\p{L}\\d \\+-.]+)", Pattern.CASE_INSENSITIVE);

  public static final int MIN_TAG_LENGTH = 2;
  public static final int MAX_TAG_LENGTH = 25;

  @Autowired
  private TagDao tagDao;

  private final List<ITagActionHandler> actionHandlers = new ArrayList<>();

  public List<ITagActionHandler> getActionHandlers() {
    return actionHandlers;
  }

  /**
   * Получение идентификационного номера тега по названию. Тег должен использоваться.
   *
   * @param tag название тега
   * @return идентификационный номер
   * @throws TagNotFoundException
   */
  public int getTagId(String tag) throws TagNotFoundException {
    return tagDao.getTagId(tag, true);
  }

  public static void checkTag(String tag) throws UserErrorException {
    // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
    if (!isGoodTag(tag)) {
      throw new UserErrorException("Некорректный тег: '" + tag + '\'');
    }
  }

  /**
   * Разбор строки тегов. Игнорируем некорректные теги
   *
   * @param tags список тегов через запятую
   * @return список тегов
   */
  @Nonnull
  public ImmutableList<String> parseSanitizeTags(@Nullable String tags) {
    if (tags == null) {
      return ImmutableList.of();
    }

    Set<String> tagSet = new HashSet<>();

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
   * Обновить счётчики по тегам.
   *
   * @param oldTags список старых тегов
   * @param newTags список новых тегов
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void updateCounters(final List<String> oldTags, final List<String> newTags) {
    StringBuilder logStr = new StringBuilder()
      .append("Обновление счётчиков тегов; старые теги [")
      .append(oldTags.toString())
      .append("]; новые теги [")
      .append(newTags.toString())
      .append(']');
    logger.debug(logStr);

    for (String tag : newTags) {
      if (!oldTags.contains(tag)) {
        int id = getOrCreateTag(tag);
        logger.trace("Увеличен счётчик для тега " + tag);
        tagDao.increaseCounterById(id, 1);
      }
    }

    for (String tag : oldTags) {
      if (!newTags.contains(tag)) {
        int id = getOrCreateTag(tag);
        logger.trace("Уменьшен счётчик для тега " + tag);
        tagDao.decreaseCounterById(id, 1);
      }
    }
    logger.trace("Завершено: " + logStr);
  }

  /**
   * Получить уникальный список первых букв тегов.
   *
   * @return список первых букв тегов
   */
  public SortedSet<String> getFirstLetters() {
    return tagDao.getFirstLetters();
  }

  /**
   * Получить список тегов по первому символу.
   *
   * @param firstLetter     первый символ
   * @return список тегов по первому символу
   */
  public Map<String, Integer> getTagsByFirstLetter(String firstLetter) {
    return tagDao.getTagsByFirstLetter(firstLetter);
  }

  /**
   * Создать новый тег.
   *
   * @param tagName название нового тега
   */
  public void create(String tagName) {
    tagDao.createTag(tagName);
    logger.info("Создан тег: " + tagName);
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
      int oldTagId = tagDao.getTagId(oldTagName);
      try {
        tagDao.getTagId(tagName);
        errors.rejectValue("tagName", "", "Тег с таким именем уже существует!");
      } catch (TagNotFoundException ignored) {
        tagDao.changeTag(oldTagId, tagName);
        StringBuilder logStr = new StringBuilder()
          .append("Изменено название тега. Старое значение: '")
          .append(oldTagName)
          .append("'; новое значение: '")
          .append(tagName)
          .append('\'');
        logger.info(logStr);
      }
    } catch (UserErrorException e) {
      errors.rejectValue("tagName", "", e.getMessage());
    } catch (TagNotFoundException e) {
      errors.rejectValue("tagName", "", "Тега с таким именем не существует!");
    }
  }

  /**
   * Удалить тег по названию. Заменить все использования удаляемого тега
   * новым тегом (если имя нового тега не null).
   *
   * @param tagName    название тега
   * @param newTagName новое название тега
   * @param errors     обработчик ошибок ввода для формы
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void delete(String tagName, String newTagName, Errors errors) {
    // todo: Нельзя строить логику на исключениях. Это антипаттерн!
    try {
      checkTag(tagName);
      int oldTagId = tagDao.getTagId(tagName);
      if (!Strings.isNullOrEmpty(newTagName)) {
        if (newTagName.equals(tagName)) {
          errors.rejectValue("tagName", "", "Заменяемый тег не должен быть равен удаляемому!");
          return;
        }
        checkTag(newTagName);
        int newTagId = getOrCreateTag(newTagName);
        if (newTagId != 0) {
          for (ITagActionHandler actionHandler : actionHandlers) {
            actionHandler.replaceTag(oldTagId, tagName, newTagId, newTagName);
          }
          StringBuilder logStr = new StringBuilder()
            .append("Удаляемый тег '")
            .append(tagName)
            .append("' заменён тегом '")
            .append(newTagName)
            .append('\'');
          logger.debug(logStr);
        }
      }
      for (ITagActionHandler actionHandler : actionHandlers) {
        actionHandler.deleteTag(oldTagId, tagName);
      }
      tagDao.deleteTag(oldTagId);
      logger.info("Удалён тег: " + tagName);

    } catch (UserErrorException e) {
      errors.rejectValue("tagName", "", e.getMessage());
    } catch (TagNotFoundException e) {
      errors.rejectValue("tagName", "", "Тега с таким именем не существует!");
    }
  }

  /**
   * Получение идентификационного номера тега по названию, либо создание нового тега.
   *
   * @param tagName название тега
   * @return идентификационный номер тега
   */
  public int getOrCreateTag(String tagName) {
    int id;
    // todo: Нельзя строить логику на исключениях. Это антипаттерн!
    try {
      id = tagDao.getTagId(tagName);
    } catch (TagNotFoundException e) {
      create(tagName);
      try {
        id = tagDao.getTagId(tagName);
      } catch (TagNotFoundException e2) {
        id = 0;
      }
    }
    return id;
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

  public static boolean isGoodTag(String tag) {
    return tagRE.matcher(tag).matches() && tag.length() >= MIN_TAG_LENGTH && tag.length() <= MAX_TAG_LENGTH;
  }

  /**
   * пересчёт счётчиков использования.
   */
  public void reCalculateAllCounters() {
    for (ITagActionHandler actionHandler : actionHandlers) {
      actionHandler.reCalculateAllCounters();
    }
  }

  public int getCounter(String tag) throws TagNotFoundException {
    int tagId = tagDao.getTagId(tag);

    return tagDao.getCounter(tagId);
  }
}
