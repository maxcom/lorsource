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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import ru.org.linux.user.UserErrorException;
import scala.Option;

import java.util.ArrayList;
import java.util.List;

@Service
public class TagModificationService {
  private static final Logger logger = LoggerFactory.getLogger(TagModificationService.class);

  @Autowired
  private TagDao tagDao;

  @Autowired
  private TagService tagService;

  private final List<ITagActionHandler> actionHandlers = new ArrayList<>();

  public List<ITagActionHandler> getActionHandlers() {
    return actionHandlers;
  }

  /**
   * Изменить название существующего тега.
   *
   * @param oldTagName старое название тега
   * @param tagName    новое название тега
   * @param errors     обработчик ошибок ввода для формы
   */
  public void change(String oldTagName, String tagName, Errors errors) {
    try {
      TagName.checkTag(tagName);
      int oldTagId = tagService.getTagId(oldTagName);

      if (tagDao.getTagId(tagName).isDefined()) {
        errors.rejectValue("tagName", "", "Тег с таким именем уже существует!");
      } else {
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
    try {
      int oldTagId = tagService.getTagId(tagName);
      if (!Strings.isNullOrEmpty(newTagName)) {
        if (newTagName.equals(tagName)) {
          errors.rejectValue("tagName", "", "Заменяемый тег не должен быть равен удаляемому!");
          return;
        }
        TagName.checkTag(newTagName);
        int newTagId = getOrCreateTag(newTagName);

        for (ITagActionHandler actionHandler : actionHandlers) {
          actionHandler.replaceTag(oldTagId, newTagId, newTagName);
        }
        logger.debug("Удаляемый тег '{}' заменён тегом '{}'", tagName, newTagName);
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
  public int getOrCreateTag(final String tagName) {
    Option<Integer> tagId = tagDao.getTagId(tagName);

    if (tagId.isDefined()) {
      return tagId.get();
    } else {
      return tagDao.createTag(tagName);
    }
  }
}
