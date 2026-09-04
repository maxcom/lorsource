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

import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, never, times, verify, when}
import ru.org.linux.spring.SiteConfig

/** Юнит-тесты для [[BlockedUserCleaner]] на моках DAO и SiteConfig. */
class BlockedUserCleanerTest:
  private val siteConfig = mock(classOf[SiteConfig])
  private val userDao = mock(classOf[UserDao])
  private val cleaner = BlockedUserCleaner(siteConfig, userDao)

  @Test
  def noCandidatesDoesNothing(): Unit =
    when(userDao.getDeletableBlockedUserIds).thenReturn(Seq.empty[Int])

    cleaner.cleanBlockedUsers()

    verify(userDao, never()).deleteBlockedUsers(any(classOf[Seq[Int]]))

  @Test
  def flagOffDoesNotDelete(): Unit =
    when(userDao.getDeletableBlockedUserIds).thenReturn(Seq(1, 2, 3))
    when(siteConfig.cleanOldBlockedUsers).thenReturn(false)

    cleaner.cleanBlockedUsers()

    verify(userDao, never()).deleteBlockedUsers(any(classOf[Seq[Int]]))

  @Test
  def flagOnDeletesCandidates(): Unit =
    when(userDao.getDeletableBlockedUserIds).thenReturn(Seq(1, 2, 3))
    when(siteConfig.cleanOldBlockedUsers).thenReturn(true)

    cleaner.cleanBlockedUsers()

    verify(userDao).deleteBlockedUsers(Seq(1, 2, 3))

  @Test
  def flagOnDeletesInBatches(): Unit =
    val ids = (1 to BlockedUserCleaner.BatchSize * 3).toSeq
    when(userDao.getDeletableBlockedUserIds).thenReturn(ids)
    when(siteConfig.cleanOldBlockedUsers).thenReturn(true)

    cleaner.cleanBlockedUsers()

    verify(userDao, times(3)).deleteBlockedUsers(any(classOf[Seq[Int]]))
    verify(userDao).deleteBlockedUsers((1 to BlockedUserCleaner.BatchSize).toSeq)
    verify(userDao).deleteBlockedUsers((BlockedUserCleaner.BatchSize + 1 to BlockedUserCleaner.BatchSize * 2).toSeq)

  @Test
  def deleteFailurePropagates(): Unit =
    when(userDao.getDeletableBlockedUserIds).thenReturn(Seq(1, 2, 3))
    when(siteConfig.cleanOldBlockedUsers).thenReturn(true)
    when(userDao.deleteBlockedUsers(Seq(1, 2, 3))).thenThrow(new RuntimeException("batch failed"))

    assertThrows(classOf[RuntimeException], () => cleaner.cleanBlockedUsers())
