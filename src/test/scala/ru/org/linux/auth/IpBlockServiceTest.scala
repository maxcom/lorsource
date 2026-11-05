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

import munit.FunSuite

import java.time.OffsetDateTime

class IpBlockServiceTest extends FunSuite:
  test("getBlockInfo reads from dao only once per ip"):
    val dao = new RecordingIpBlockDao
    val service = new IpBlockService(dao)

    val first = service.getBlockInfo("192.168.1.1")
    val second = service.getBlockInfo("192.168.1.1")

    assertEquals(dao.getBlockInfoCount, 1)
    assert(first eq second, "cached value should be the same instance")

  test("getBlockInfo caches negative results for unknown ips"):
    val dao = new RecordingIpBlockDao
    val service = new IpBlockService(dao)

    val info = service.getBlockInfo("192.168.1.1")

    assertEquals(dao.getBlockInfoCount, 1)
    assert(!info.initialized)

  test("getBlockInfo distinguishes different ips"):
    val dao = new RecordingIpBlockDao
    val service = new IpBlockService(dao)

    service.getBlockInfo("10.0.0.1")
    service.getBlockInfo("10.0.0.2")

    assertEquals(dao.getBlockInfoCount, 2)

  test("blockIP invalidates cached value"):
    val dao = new RecordingIpBlockDao
    val service = new IpBlockService(dao)

    service.getBlockInfo("10.0.0.1")
    assert(dao.getBlockInfoCount == 1)

    service.blockIP("10.0.0.1", 1, "test reason", None, allowPosting = false, captchaRequired = true)
    service.getBlockInfo("10.0.0.1")

    assertEquals(dao.getBlockInfoCount, 2)
    assertEquals(dao.blockIPCount, 1)

  test("blockIP invalidates only the blocked ip"):
    val dao = new RecordingIpBlockDao
    val service = new IpBlockService(dao)

    service.getBlockInfo("10.0.0.1")
    service.getBlockInfo("10.0.0.2")
    service.blockIP("10.0.0.1", 1, "test reason", None, allowPosting = false, captchaRequired = true)
    service.getBlockInfo("10.0.0.1")
    service.getBlockInfo("10.0.0.2")

    assertEquals(dao.getBlockInfoCount, 3)

/** Заглушка dao со счётчиками вызовов; БД не используется. */
private class RecordingIpBlockDao extends IpBlockDao(null):
  var getBlockInfoCount: Int = 0
  var blockIPCount: Int = 0

  override def getBlockInfo(addr: String): IpBlockInfo =
    getBlockInfoCount += 1
    IpBlockInfo(addr)

  override def blockIP(
      ip: String,
      moderatorId: Int,
      reason: String,
      banUntil: Option[OffsetDateTime],
      allowPosting: Boolean,
      captchaRequired: Boolean): Unit = blockIPCount += 1

end RecordingIpBlockDao
