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

package ru.org.linux.tag;

import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import ru.org.linux.user.UserErrorException;

import java.util.*;

@Service
public class TagService {
  private static final Logger logger = LoggerFactory.getLogger(TagService.class);

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

  /**
   * Получить список наиболее популярных тегов.
   *
   * @return список наиболее популярных тегов
   */
  public SortedSet<String> getTopTags() {
    return tagDao.getTopTags();
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
   * Получить список тегов по префиксу.
   *
   * @param prefix     префикс
   * @return список тегов по первому символу
   */
  public Map<String, Integer> getTagsByPrefix(String prefix, int threshold) {
    return tagDao.getTagsByPrefix(prefix, threshold);
  }

  /**
   * Получить список популярных тегов по префиксу.
   *
   * @param prefix     префикс
   * @param count      количество тегов
   * @return список тегов по первому символу
   */
  public SortedSet<String> suggestTagsByPrefix(String prefix, int count) {
    return tagDao.getTopTagsByPrefix(prefix, 2, count);
  }

  /**
   * Создать новый тег.
   *
   * @param tagName название нового тега
   */
  private void create(String tagName) {
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
      TagName.checkTag(tagName);
      int oldTagId = tagDao.getTagId(oldTagName);
      try {
        tagDao.getTagId(tagName);
        errors.rejectValue("tagName", "", "Тег с таким именем уже существует!");
      } catch (TagNotFoundException ignored) {
        tagDao.changeTag(oldTagId, tagName);
        logger.info(
                "Изменено название тега. Старое значение: '{}'; новое значение: '{}'",
                oldTagName,
                tagName
        );
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
      int oldTagId = tagDao.getTagId(tagName);
      if (!Strings.isNullOrEmpty(newTagName)) {
        if (newTagName.equals(tagName)) {
          errors.rejectValue("tagName", "", "Заменяемый тег не должен быть равен удаляемому!");
          return;
        }
        TagName.checkTag(newTagName);
        int newTagId = getOrCreateTag(newTagName);
        if (newTagId != 0) {
          for (ITagActionHandler actionHandler : actionHandlers) {
            actionHandler.replaceTag(oldTagId, tagName, newTagId, newTagName);
          }
          logger.debug("Удаляемый тег '{}' заменён тегом '{}'", tagName, newTagName);
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

  public List<String> getRelatedTags(int tagId) {
    return Ordering.natural().immutableSortedCopy(tagDao.relatedTags(tagId));
  }
}
