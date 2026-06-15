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

import org.springframework.stereotype.Service
import org.springframework.validation.Errors
import ru.org.linux.auth.{IpBlockDao, IpBlockInfo}
import ru.org.linux.user.User

import javax.annotation.Nullable

object IpBlockChecker:
  // temporary transitional method
  def check(ipBlockInfo: IpBlockInfo, user: Option[User]): Permission = checkChain(ipBlockInfo, user).seal

  def checkChain(ipBlockInfo: IpBlockInfo, user: Option[User]): RestrictionChain =
    Unrestricted
      .restrict(
        ipBlockInfo.isBlocked && (user.isEmpty || user.exists(_.anonymous)),
        "анонимный постинг с этого IP адреса заблокирован")
      .restrict(
        ipBlockInfo.isBlocked && user.exists(_.isAnonymousScore),
        s"постинг с этого IP адреса ограничен для пользователей с score < ${User.AnonymousLevelScore}"
      )
      .restrict(
        ipBlockInfo.isBlocked && user.isDefined && !ipBlockInfo.isAllowRegisteredPosting,
        s"постинг с этого IP адреса заблокирован")

  // temporary transitional method
  def checkBlockIP(
      block: IpBlockInfo,
      errors: Errors,
      @Nullable
      user: User): Unit =
    if block.isBlocked && (user == null || user.isAnonymousScore || !block.isAllowRegisteredPosting) then
      errors.reject(null, "Постинг заблокирован: " + block.reason)
