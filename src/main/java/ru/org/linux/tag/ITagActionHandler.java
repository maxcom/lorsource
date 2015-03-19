/*
 * Copyright 1998-2015 Linux.org.ru
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

/**
 * Интерфейс для вызова из TagService вспомогательных обработчиков
 */
public interface ITagActionHandler {
  /**
   * Изменение существующего тега.
   *
   * @param oldTagId    идентификационный номер старого тега
   * @param newTagId    идентификационный номер нового тега
   * @param newTagName  название нового тега
   */
  void replaceTag(int oldTagId, int newTagId, String newTagName);

  /**
   * Удаление существующего тега.
   *
   * @param tagId    идентификационный номер тега
   * @param tagName  название тега
   */
  void deleteTag(int tagId, String tagName);
}
