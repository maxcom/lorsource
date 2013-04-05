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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import ru.org.linux.tag.ITagActionHandler;
import ru.org.linux.tag.TagDao;
import ru.org.linux.tag.TagNotFoundException;
import ru.org.linux.tag.TagService;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserTagService {
  private static final Log logger = LogFactory.getLog(UserTagService.class);

  private final ITagActionHandler actionHandler = new ITagActionHandler() {
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
  private UserTagDao userTagDao;

  @Autowired
  private TagDao tagDao;

  @Autowired
  private TagService tagService;

  @PostConstruct
  private void addToReplaceHandlerList() {
    tagService.getActionHandlers().add(actionHandler);
  }

  /**
   * Добавление тега к пользователю.
   *
   * @param user    объект пользователя
   * @param tagName название тега
   * @return идентификатор избранного тега
   */
  public int favoriteAdd(User user, String tagName)
    throws TagNotFoundException {
    int tagId = tagDao.getTagId(tagName);
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
  public int favoriteDel(User user, String tagName)
    throws TagNotFoundException {
    int tagId = tagDao.getTagId(tagName);
    userTagDao.deleteTag(user.getId(), tagId, true);
    return tagId;
  }

  /**
   * Добавление игнорированного тега к пользователю.
   *
   * @param user    объект пользователя
   * @param tagName название тега
   */
  public void ignoreAdd(User user, String tagName)
    throws TagNotFoundException {
    int tagId = tagDao.getTagId(tagName);
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
    int tagId = tagDao.getTagId(tagName);
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
   * @param user объект пользователя, которому не нужно слать оповещение
   * @param tags список фаворитных тегов
   * @return список ID пользователей
   */
  public List<Integer> getUserIdListByTags(User user, List<String> tags) {
    return userTagDao.getUserIdListByTags(user.getId(), tags);
  }

  /**
   * Разбор строки тегов.
   *
   * @param tags список тегов через запятую
   * @return список тегов
   */
  public ImmutableList<String> parseTags(String tags, Errors errors) {
    Set<String> tagSet = new HashSet<>();

    // Теги разделяются пайпом или запятой
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
      if (!TagService.isGoodTag(tag)) {
        errors.reject("Некорректный тег: '" + tag + '\'');
        continue;
      }

      tagSet.add(tag);
    }

    return ImmutableList.copyOf(tagSet);
  }

  /**
   * Проверяет, есть ли указанный фаворитный тег у пользователя.
   *
   * @param user    объект пользователя
   * @param tagName название тега
   * @return true если у пользователя есть тег
   */
  public boolean hasFavoriteTag(User user, String tagName) {
    ImmutableList<String> tags = favoritesGet(user);
    return tags.contains(tagName);
  }

  /**
   * Проверяет, есть ли указанный игнорируемый тег у пользователя.
   *
   * @param user    объект пользователя
   * @param tagName название тега
   * @return true если у пользователя есть тег
   */
  public boolean hasIgnoreTag(User user, String tagName) {
    ImmutableList<String> tags = ignoresGet(user);
    return tags.contains(tagName);
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
    WebDataBinder binder = new WebDataBinder("");
    Errors errors = binder.getBindingResult();
    ImmutableList<String> tagList = parseTags(tagsStr, errors);
    for (String tag : tagList) {
      try {
        if (isFavorite) {
          favoriteAdd(user, tag);
        } else {
          ignoreAdd(user, tag);
        }
      } catch (TagNotFoundException e) {
        errors.reject(e.getMessage() + ": '" + tag);
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
    List<String> strErrors = new ArrayList<>();

    if (errors.hasErrors()) {
      for (ObjectError objectError : errors.getAllErrors()) {
        strErrors.add(objectError.getCode());
      }
    }
    return strErrors;
  }

  public int countFavs(int id) {
    return userTagDao.countFavs(id);
  }
}
