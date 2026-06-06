/*
 * Copyright 1998-2026 Linux.org.ru
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

package ru.org.linux.user

enum UserLogAction(val name: String, val description: String):
  case ResetUserpic extends UserLogAction("reset_userpic", "Сброшена фотография")
  case SetUserpic extends UserLogAction("set_userpic", "Установлена фотография")
  case BlockUser extends UserLogAction("block_user", "Заблокирован")
  case Score50 extends UserLogAction("score50", "Задан score=50")
  case UnblockUser extends UserLogAction("unblock_user", "Разблокирован")
  case AcceptNewEmail extends UserLogAction("accept_new_email", "Установлен новый email")
  case ResetInfo extends UserLogAction("reset_info", "Сброшен текст информации")
  case ResetUrl extends UserLogAction("reset_url", "Сброшен URL")
  case ResetTown extends UserLogAction("reset_town", "Сброшено поле \"город\"")
  case ResetPassword extends UserLogAction("reset_password", "Сброшен пароль")
  case SetPassword extends UserLogAction("set_password", "Установлен новый пароль")
  case SetInfo extends UserLogAction("set_info", "Обновлен профиль")
  case SetCorrector extends UserLogAction("set_corrector", "Добавлены права корректора")
  case UnsetCorrector extends UserLogAction("unset_corrector", "Убраны права корректора")
  case Register extends UserLogAction("register", "Зарегистрирован")
  case Frozen extends UserLogAction("frozen", "Заморожен")
  case Defrosted extends UserLogAction("defrosted", "Разморожен")
  case SentPasswordReset extends UserLogAction("sent_password_reset", "Отправлен код сброса пароля")

  override def toString: String = name

  def getDescription: String = description

  def toDbName: String = name

object UserLogAction:
  private val byName = values.map(a => a.name -> a).toMap

  def fromDbName(s: String): UserLogAction =
    byName.getOrElse(s, throw new IllegalArgumentException(s"No UserLogAction with name $s"))
