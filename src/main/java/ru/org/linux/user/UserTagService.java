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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.topic.TagDao;
import ru.org.linux.topic.TagNotFoundException;

import java.util.List;

@Service
public class UserTagService {
  @Autowired
  UserTagDao userTagDao;

  @Autowired
  TagDao tagDao;

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
   * @param user    объект пользователя
   * @return список тегов пользователя
   */
  public ImmutableList<String> favoritesGet(User user) {
    return userTagDao.getTags(user.getId(), true);
  }

  /**
   * Получение списка игнорированных тегов пользователя.
   *
   * @param user    объект пользователя
   * @return список игнорированных тегов пользователя
   */
  public ImmutableList<String> ignoresGet(User user) {
    return userTagDao.getTags(user.getId(), false);
  }

  /**
   * Получить список ID пользователей, у которых в профиле есть перечисленные фаворитные теги.
   *
   * @param tags  список фаворитных тегов
   * @return список ID пользователей
   */
  public List<Integer> getUserIdListByTags (List<String> tags) {
    return userTagDao.getUserIdListByTags (tags);
  }
}
