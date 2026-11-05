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

package ru.org.linux.auth

import com.github.benmanes.caffeine.cache.{Caffeine, LoadingCache}
import org.springframework.stereotype.Service

import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

object IpBlockService:
  private val CacheMaximumSize = 50000L
  private val CacheExpireSeconds = 60L

@Service
class IpBlockService(ipBlockDao: IpBlockDao):
  // getBlockInfo вызывается из CommonContextFilter на каждом запросе; кешируем результат
  // на 60 секунд (в т.ч. отрицательные результаты — большинство IP не заблокированы).
  // Инвалидация при ручной блокировке — только в пределах данной JVM, поэтому срок
  // ограничен TTL.
  private val blockInfoCache: LoadingCache[String, IpBlockInfo] =
    Caffeine
      .newBuilder()
      .maximumSize(IpBlockService.CacheMaximumSize)
      .expireAfterWrite(IpBlockService.CacheExpireSeconds, TimeUnit.SECONDS)
      .build((ip: String) => ipBlockDao.getBlockInfo(ip))

  def getBlockInfo(addr: String): IpBlockInfo = blockInfoCache.get(addr)

  /** Ручная блокировка/разблокировка IP модератором; инвалидирует кешированное значение. */
  def blockIP(
      ip: String,
      moderatorId: Int,
      reason: String,
      banUntil: Option[OffsetDateTime],
      allowPosting: Boolean,
      captchaRequired: Boolean): Unit =
    ipBlockDao.blockIP(ip, moderatorId, reason, banUntil, allowPosting, captchaRequired)
    blockInfoCache.invalidate(ip)

end IpBlockService
