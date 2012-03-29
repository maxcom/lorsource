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

package ru.org.linux.user;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.tag.ITagActionHandler;
import ru.org.linux.tag.IncorrectTagException;
import ru.org.linux.tag.TagDao;
import ru.org.linux.tag.TagNotFoundException;
import ru.org.linux.tag.TagService;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserTagService {
  private static final Log logger = LogFactory.getLog(UserTagService.class);

  private ITagActionHandler actionHandler = new ITagActionHandler() {
    @Override
    public void replaceTag(int oldTagId, String oldTagName, int newTagId, String newTagName) {
      userTagDao.replaceTag(oldTagId, newTagId);
    }

    @Override
    public void deleteTag(int tagId, String tagName) {
      userTagDao.deleteTags(tagId);
      logger.debug("Удалено использование тега '" + tagName + "' у всех пользователей");
    }

    @Override
    public void reCalculateAllCounters() {
    }
  };

  @Autowired
  UserTagDao userTagDao;

  @Autowired
  TagDao tagDao;

  @Autowired
  TagService tagService;

  @PostConstruct
  private void addToReplaceHandlerList() {
    tagService.getActionHandlers().add(actionHandler);
  }

  /**
   * Добавление тега к пользователю.
   *
   * @param user    объект пользователя
   * @param tagName название тега
   */
  public void favoriteAdd(User user, String tagName)
    throws TagNotFoundException {
    int tagId = tagDao.getTagIdByName(tagName);
    userTagDao.addTag(user.getId(), tagId, true);
  }

  /**
   * Удаление тега у пользователя.
   *
   * @param user    объект пользователя
   * @param tagName название тега
   */
  public void favoriteDel(User user, String tagName)
    throws TagNotFoundException {
    int tagId = tagDao.getTagIdByName(tagName);
    userTagDao.deleteTag(user.getId(), tagId, true);
  }

  /**
   * Добавление игнорированного тега к пользователю.
   *
   * @param user    объект пользователя
   * @param tagName название тега
   */
  public void ignoreAdd(User user, String tagName)
    throws TagNotFoundException {
    int tagId = tagDao.getTagIdByName(tagName);
    userTagDao.addTag(user.getId(), tagId, false);
  }

  /**
   * Удаление игнорированного тега у пользователя.
   *
   * @param user    объект пользователя
   * @param tagName название тега
   */
  public void ignoreDel(User user, String tagName)
    throws TagNotFoundException {
    int tagId = tagDao.getTagIdByName(tagName);
    userTagDao.deleteTag(user.getId(), tagId, false);
  }

  /**
   * Получение списка тегов пользователя.
   *
   * @param user объект пользователя
   * @return список тегов пользователя
   */
  public ImmutableList<String> favoritesGet(User user) {
    return userTagDao.getTags(user.getId(), true);
  }

  /**
   * Получение списка игнорированных тегов пользователя.
   *
   * @param user объект пользователя
   * @return список игнорированных тегов пользователя
   */
  public ImmutableList<String> ignoresGet(User user) {
    return userTagDao.getTags(user.getId(), false);
  }

  /**
   * Получить список ID пользователей, у которых в профиле есть перечисленные фаворитные теги.
   *
   * @param tags список фаворитных тегов
   * @return список ID пользователей
   */
  public List<Integer> getUserIdListByTags(List<String> tags) {
    return userTagDao.getUserIdListByTags(tags);
  }

  /**
   * Разбор строки тегов.
   *
   * @param tags список тегов через запятую
   * @return список тегов
   * @throws IncorrectTagException
   */
  public ImmutableList<String> parseTags(String tags)
    throws IncorrectTagException {
    Set<String> tagSet = new HashSet<String>();

    // Теги разделяютчя пайпом или запятой
    tags = tags.replaceAll("\\|", ",");
    String[] tagsArr = tags.split(",");

    if (tagsArr.length == 0) {
      return ImmutableList.of();
    }

    for (String aTagsArr : tagsArr) {
      String tag = StringUtils.stripToNull(aTagsArr);
      // плохой тег - выбрасываем
      if (tag == null) {
        continue;
      }

      // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
      if (!tagService.isGoodTag(tag)) {
        throw new IncorrectTagException("Некорректный тег: '" + tag + '\'');
      }

      tagSet.add(tag);
    }
    return ImmutableList.copyOf(tagSet);
  }


}
