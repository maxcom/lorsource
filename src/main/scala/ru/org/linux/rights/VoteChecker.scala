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
package ru.org.linux.rights

import ru.org.linux.auth.AnySession
import ru.org.linux.topic.Topic

object VoteChecker:
  def check(topic: Topic)(using AnySession): Permission =
    Unrestricted
      .restrict(topic.deleted, "опрос удален")
      .restrict(!topic.commited, "опрос еще не подтвержден модератором")
      .restrict(topic.expired, "опрос завершен")
      .restrict(!summon[AnySession].authorized, "только для зарегистрированных")
      .restrict(FrozenUserChecker.checkChain)
      .restrict(IpBlockChecker.checkChain)
      .seal
