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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import ru.org.linux.tag.ITagActionHandler;
import ru.org.linux.tag.TagDao;
import ru.org.linux.tag.TagService;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TopicTagService {
  private static final Log logger = LogFactory.getLog(TopicTagService.class);

  private ITagActionHandler actionHandler = new ITagActionHandler() {
    @Override
    public void replaceTag(int oldTagId, String oldTagName, int newTagId, String newTagName) {
      int tagCount = topicTagDao.getCountReplacedTags(oldTagId, newTagId);
      topicTagDao.replaceTag(oldTagId, newTagId);
      tagDao.increaseCounterById(newTagId, tagCount);

      StringBuilder logStr = new StringBuilder()
        .append("Счётчик использование тега '")
        .append(newTagName)
        .append("' увеличен на ")
        .append(tagCount);
      logger.debug(logStr);
    }

    @Override
    public void deleteTag(int tagId, String tagName) {
      topicTagDao.deleteTag(tagId);
      logger.debug("Удалено использование тега '" + tagName + "' в топиках");
    }

    @Override
    public void reCalculateAllCounters() {
      topicTagDao.reCalculateAllCounters();
    }
  };


  public static final int MAX_TAGS_PER_TOPIC = 5;
  public static final int MAX_TAGS_IN_TITLE = 3;

  @Autowired
  private TagService tagService;

  @Autowired
  private TopicTagDao topicTagDao;

  @Autowired
  private TagDao tagDao;

  @PostConstruct
  private void addToReplaceHandlerList() {
    tagService.getActionHandlers().add(actionHandler);
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
        int id = tagService.getOrCreateTag(tag);
        logger.trace("Добавлен тег '" + tag + "' к топику msgId=" + msgId);
        topicTagDao.addTag(msgId, id);
        modified = true;
      }
    }

    for (String tag : oldTags) {
      if (!tagList.contains(tag)) {
        int id = tagService.getOrCreateTag(tag);
        logger.trace("Удалён тег '" + tag + "' к топику msgId=" + msgId);
        topicTagDao.deleteTag(msgId, id);
        modified = true;
      }
    }
    logger.trace("Завершено: обновление списка тегов для топика msgId=" + msgId);
    return modified;
  }

  /**
   * Получить все теги сообщения по идентификационному номеру сообщения.
   *
   * @param msgId идентификационный номер сообщения
   * @return все теги сообщения
   */
  @Nonnull
  public ImmutableList<String> getMessageTags(int msgId) {
    return topicTagDao.getTags(msgId);
  }

  /**
   * Получить все теги сообщения по идентификационному номеру сообщения.
   * Ограничение по числу тегов для показа в заголовке в таблице
   *
   * @param msgId идентификационный номер сообщения
   * @return все теги сообщения
   */
  @Nonnull
  public ImmutableList<String> getMessageTagsForTitle(int msgId) {
    ImmutableList<String> tags = topicTagDao.getTags(msgId);
    return tags.subList(0, Math.min(tags.size(), MAX_TAGS_IN_TITLE));
  }

  /**
   * Разбор строки тегов. Error при ошибках
   *
   * @param tags   список тегов через запятую
   * @param errors класс для ошибок валидации (параметр 'tags')
   * @return список тегов
   */
  public ImmutableList<String> parseTags(String tags, Errors errors) {
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
      if (tag.length() > TagService.MAX_TAG_LENGTH) {
        errors.rejectValue("tags", null, "Слишком длиный тег: '" + tag + "\' (максимум " + TagService.MAX_TAG_LENGTH + " символов)");
      } else if (!TagService.isGoodTag(tag)) {
        errors.rejectValue("tags", null, "Некорректный тег: '" + tag + '\'');
      }

      tagSet.add(tag);
    }

    if (tagSet.size() > MAX_TAGS_PER_TOPIC) {
      errors.rejectValue("tags", null, "Слишком много тегов (максимум " + MAX_TAGS_PER_TOPIC + ')');
    }

    return ImmutableList.copyOf(tagSet);
  }
}
