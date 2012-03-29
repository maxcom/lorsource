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
import ru.org.linux.user.UserTagDao;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class TagService {
  private static final Log logger = LogFactory.getLog(TagService.class);

  private static final Pattern tagRE = Pattern.compile("([\\p{L}\\d \\+-]+)", Pattern.CASE_INSENSITIVE);

  public static final int MIN_TAG_LENGTH = 2;
  public static final int MAX_TAG_LENGTH = 25;
  
  public static final int MAX_TAGS_PER_TOPIC = 5;
  public static final int MAX_TAGS_IN_TITLE = 3;

  @Autowired
  private TagDao tagDao;

  @Autowired
  private UserTagDao userTagDao;

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

  /**
   * Разбор сторки тегов. Error при ошибках
   *
   * @param tags список тегов через запятую
   * @param errors класс для ошибок валидации (параметр 'tags')
   * @return список тегов
   */
  public ImmutableList<String> parseTags(String tags, Errors errors) {
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
      if (tag.length()>MAX_TAG_LENGTH) {
        errors.rejectValue("tags", null, "Слишком длиный тег: '" + tag + "\' (максимум "+MAX_TAG_LENGTH +" символов)");
      } else if (!isGoodTag(tag)) {
        errors.rejectValue("tags", null, "Некорректный тег: '" + tag + '\'');
      }

      tagSet.add(tag);
    }

    if (tagSet.size()>MAX_TAGS_PER_TOPIC) {
      errors.rejectValue("tags", null, "Слишком много тегов (максимум "+MAX_TAGS_PER_TOPIC+")");
    }

    return ImmutableList.copyOf(tagSet);
  }

  /**
   * Разбор строки тегов. Игнорируем некорректные теги
   *
   * @param tags список тегов через запятую
   * @return список тегов
   */
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
   * Получить все теги сообщения по идентификационному номеру сообщения.
   * Ограничение по числу тегов для показа в заголовке в таблице
   *
   * @param msgId идентификационный номер сообщения
   * @return все теги сообщения
   */
  public ImmutableList<String> getMessageTagsForTitle(int msgId) {
    ImmutableList<String> tags = tagDao.getMessageTags(msgId);
    return tags.subList(0, Math.min(tags.size(), TagService.MAX_TAGS_IN_TITLE));
  }

  /**
   * Обновить список тегов сообщения по идентификационному номеру сообщения.
   *
   * @param msgId   идентификационный номер сообщения
   * @param tagList новый список тегов.
   * @return true если были произведены изменения
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public synchronized boolean updateTags(final int msgId, final List<String> tagList) {
    logger.debug("Обновление списка тегов [" + tagList.toString() + "] для топика msgId=" + msgId);
    final List<String> oldTags = getMessageTags(msgId);

    boolean modified = false;
    for (String tag : tagList) {
      if (!oldTags.contains(tag)) {
        int id = getOrCreateTag(tag);
        logger.trace("Добавлен тег '" + tag + "' к топику msgId=" + msgId);
        tagDao.addTagToTopic(msgId, id);
        modified = true;
      }
    }

    for (String tag : oldTags) {
      if (!tagList.contains(tag)) {
        int id = getOrCreateTag(tag);
        logger.trace("Удалён тег '" + tag + "' к топику msgId=" + msgId);
        tagDao.deleteTagFromTopic(msgId, id);
        modified = true;
      }
    }
    logger.trace("Завершено: обновление списка тегов для топика msgId=" + msgId);
    return modified;
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
      .append("]");
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
   * Создать новый тег.
   *
   * @param tagName название нового тега
   */
  public void create(String tagName) {
    tagDao.createTag(tagName);
    logger.info("Создан тег: " + tagName);
  }

  /**
   * Создать новый тег с проверкой на существующий.
   *
   * @param tagName название нового тега
   * @param errors  обработчик ошибок ввода для формы
   */
  public void create(String tagName, Errors errors) {
    // todo: Нельзя строить логику на исключениях. Это антипаттерн!
    try {
      checkTag(tagName);
      int tagId = tagDao.getTagIdByName(tagName);
      errors.rejectValue("tagName", "", "Тег с таким именем уже существует!");
    } catch (TagNotFoundException ignored) {
      create(tagName);
    } catch (UserErrorException e) {
      errors.rejectValue("tagName", "", e.getMessage());
    }
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
        StringBuilder logStr = new StringBuilder()
          .append("Изменено название тега. Старое значение: '")
          .append(oldTagName)
          .append("'; новое значение: '")
          .append(tagName)
          .append("'");
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
      int oldTagId = tagDao.getTagIdByName(tagName);
      if (!Strings.isNullOrEmpty(newTagName)) {
        if (newTagName.equals(tagName)) {
          errors.rejectValue("tagName", "", "Заменяемый тег не должен быть равен удаляемому!");
          return;
        }
        checkTag(newTagName);
        int newTagId = getOrCreateTag(newTagName);
        if (newTagId != 0) {
          int tagCount = tagDao.getCountReplacedTagsForTopic(oldTagId, newTagId);
          tagDao.replaceTagForTopics(oldTagId, newTagId);
          userTagDao.replaceTag(oldTagId, newTagId);
          StringBuilder logStr = new StringBuilder()
            .append("Удаляемый тег '")
            .append(tagName)
            .append("' заменён тегом '")
            .append(newTagName)
            .append("'");
          logger.debug(logStr);

          tagDao.increaseCounterById(newTagId, tagCount);
          logStr = new StringBuilder()
            .append("Счётчик использование тега '")
            .append(newTagName)
            .append("' увеличен на ")
            .append(tagCount);
          logger.trace(logStr);
        }
      }
      tagDao.deleteTagFromTopics(oldTagId);
      logger.trace("Удалено использование тега '" + tagName + "' в топиках");
      userTagDao.deleteTags(oldTagId);
      logger.trace("Удалено использование тега '" + tagName + "' у всех пользователей");
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
      id = tagDao.getTagIdByName(tagName);
    } catch (TagNotFoundException e) {
      create(tagName);
      try {
        id = tagDao.getTagIdByName(tagName);
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
}
