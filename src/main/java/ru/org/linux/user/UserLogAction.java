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

package ru.org.linux.user;

public enum UserLogAction {
  RESET_USERPIC("reset_userpic", "Сброшена фотография"),
  SET_USERPIC("set_userpic", "Установлена фотография"),
  BLOCK_USER("block_user", "Заблокирован"),
  SCORE50("score50", "Задан score=50"),
  SETSCORE("set_score", "Изменен score"),
  UNBLOCK_USER("unblock_user", "Разблокирован"),
  ACCEPT_NEW_EMAIL("accept_new_email", "Установлен новый email"),
  RESET_INFO("reset_info", "Сброшен текст информации"),
  RESET_PASSWORD("reset_password", "Сброшен пароль"),
  SET_PASSWORD("set_password", "Установлен новый пароль"),
  SET_CORRECTOR("set_corrector", "Добавлены права корректора"),
  UNSET_CORRECTOR("unset_corrector", "Убраны права корректора"),
  REGISTER("register", "Зарегистрирован");

  private final String name;
  private final String description;

  UserLogAction(String name, String description) {
    this.name = name;
    this.description = description;
  }

  @Override
  public String toString() {
    return name;
  }

  public String getDescription() {
    return description;
  }
}
