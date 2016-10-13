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
 */

package ru.org.linux.user;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ObjectError;
import ru.org.linux.tag.*;
import scala.collection.JavaConverters;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserTagService {
  private static final Logger logger = LoggerFactory.getLogger(UserTagService.class);

  private final ITagActionHandler actionHandler = new ITagActionHandler() {
    @Override
    public void replaceTag(int oldTagId, int newTagId, String newTagName) {
      userTagDao.replaceTag(oldTagId, newTagId);
    }

    @Override
    public void deleteTag(int tagId, String tagName) {
      userTagDao.deleteTags(tagId);
      logger.debug("Удалено использование тега '" + tagName + "' у всех пользователей");
    }
  };

  @Autowired
  private UserTagDao userTagDao;

  @Autowired
  private TagModificationService tagModificationService;

  @Autowired
  private TagService tagService;

  @PostConstruct
  private void addToReplaceHandlerList() {
    tagModificationService.getActionHandlers().add(actionHandler);
  }

  /**
   * Добавление тега к пользователю.
   *
   * @param user    объект пользователя
   * @param tagName название тега
   * @return идентификатор избранного тега
   */
  public int favoriteAdd(User user, String tagName) throws TagNotFoundException {
    int tagId = tagService.getTagId(tagName);
    userTagDao.addTag(user.getId(), tagId, true);
    return tagId;
  }

  /**
   * Удаление тега у пользователя.
   *
   * @param user    объект пользователя
   * @param tagName название тега
   * @return идентификатор тега
   */
  public int favoriteDel(User user, String tagName) throws TagNotFoundException {
    int tagId = tagService.getTagId(tagName);
    userTagDao.deleteTag(user.getId(), tagId, true);
    return tagId;
  }

  /**
   * Добавление игнорированного тега к пользователю.
   *
   * @param user    объект пользователя
   * @param tagName название тега
   */
  public int ignoreAdd(User user, String tagName) throws TagNotFoundException {
    int tagId = tagService.getTagId(tagName);
    userTagDao.addTag(user.getId(), tagService.getTagId(tagName), false);
    return tagId;
  }

  /**
   * Удаление игнорированного тега у пользователя.
   *
   * @param user    объект пользователя
   * @param tagName название тега
   */
  public int ignoreDel(User user, String tagName) throws TagNotFoundException {
    int tagId = tagService.getTagId(tagName);
    userTagDao.deleteTag(user.getId(), tagService.getTagId(tagName), false);
    return tagId;
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
   * @param userid id пользователя, которому не нужно слать оповещение
   * @param tags список фаворитных тегов
   * @return список ID пользователей
   */
  public List<Integer> getUserIdListByTags(int userid, List<String> tags) {
    return userTagDao.getUserIdListByTags(userid, tags);
  }

  /**
   * Разбор строки тегов.
   *
   * @param tags список тегов через запятую
   * @return список тегов
   */
  private List<String> parseTags(String tags, Errors errors) {
    return JavaConverters.seqAsJavaListConverter(TagName.parseAndValidateTags(tags, errors, Integer.MAX_VALUE)).asJava();
  }

  /**
   * Проверяет, есть ли указанный фаворитный тег у пользователя.
   *
   * @param user    объект пользователя
   * @param tagName название тега
   * @return true если у пользователя есть тег
   */
  public boolean hasFavoriteTag(User user, String tagName) {
    return favoritesGet(user).contains(tagName);
  }

  /**
   * Проверяет, есть ли указанный игнорируемый тег у пользователя.
   *
   * @param user    объект пользователя
   * @param tagName название тега
   * @return true если у пользователя есть тег
   */
  public boolean hasIgnoreTag(User user, String tagName) {
    return ignoresGet(user).contains(tagName);
  }

  /**
   * Добавление нескольких тегов из строки. разделённых запятой.
   *
   * @param user        объект пользователя
   * @param tagsStr     строка, содержащая разделённые запятой теги
   * @param isFavorite  добавлять фаворитные теги (true) или игнорируемые (false)
   * @return null если не было ошибок; строка если были ошибки при добавлении.
   */
  public List<String> addMultiplyTags(User user, String tagsStr, boolean isFavorite) {
    Errors errors = new MapBindingResult(ImmutableMap.of(), "");
    List<String> tagList = parseTags(tagsStr, errors);
    for (String tag : tagList) {
      try {
        if (isFavorite) {
          favoriteAdd(user, tag);
        } else {
          ignoreAdd(user, tag);
        }
      } catch (TagNotFoundException e) {
        errors.reject(e.getMessage() + ": '" + tag + '\'');
      } catch (DuplicateKeyException e) {
        errors.reject("Тег уже добавлен: '" + tag);
      }
    }
    return errorsToStringList(errors);
  }

  /**
   * преобразование ошибок в массив строк.
   *
   * @param errors  объект ошибок
   * @return массив строк, содержащий описания ошибок
   */
  private static List<String> errorsToStringList(Errors errors) {
    if (errors.hasErrors()) {
      return errors.getAllErrors().stream().map(ObjectError::getCode).collect(Collectors.toList());
    } else {
      return ImmutableList.of();
    }
  }

  public int countFavs(int id) {
    return userTagDao.countFavs(id);
  }

  public int countIgnore(int id) {
    return userTagDao.countIgnore(id);
  }
}
