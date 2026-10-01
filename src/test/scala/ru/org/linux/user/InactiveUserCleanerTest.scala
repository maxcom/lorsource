/*
 * Copyright 1998-2026 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.org.linux.user

import munit.FunSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, never, times, verify, when}
import ru.org.linux.spring.SiteConfig

/** Юнит-тесты для [[InactiveUserCleaner]] на моках DAO и SiteConfig. */
class InactiveUserCleanerTest extends FunSuite:
  private var siteConfig: SiteConfig = scala.compiletime.uninitialized
  private var userDao: UserDao = scala.compiletime.uninitialized
  private var cleaner: InactiveUserCleaner = scala.compiletime.uninitialized

  // В JUnit каждый тест получал новый экземпляр класса (и новые моки); в munit экземпляр один,
  // поэтому моки пересоздаются в beforeEach — иначе verify(never())/verify(times(n)) учитывал бы
  // вызовы из предыдущих тестов.
  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    siteConfig = mock(classOf[SiteConfig])
    userDao = mock(classOf[UserDao])
    cleaner = InactiveUserCleaner(siteConfig, userDao)
    when(userDao.getDeletableBlockedUserIds).thenReturn(Seq.empty[Int])
    when(userDao.getDeletableInactiveUserIds).thenReturn(Seq.empty[Int])

  test("noCandidatesDoesNothing"):
    cleaner.cleanInactiveUsers()

    verify(userDao, never()).deleteUsers(any(classOf[Seq[Int]]))

  test("flagOffDoesNotDelete"):
    when(userDao.getDeletableBlockedUserIds).thenReturn(Seq(1, 2, 3))
    when(userDao.getDeletableInactiveUserIds).thenReturn(Seq(4, 5))
    when(siteConfig.cleanInactiveUsers).thenReturn(false)

    cleaner.cleanInactiveUsers()

    verify(userDao, never()).deleteUsers(any(classOf[Seq[Int]]))

  test("flagOnDeletesCandidates"):
    when(userDao.getDeletableBlockedUserIds).thenReturn(Seq(1, 2, 3))
    when(userDao.getDeletableInactiveUserIds).thenReturn(Seq(4, 5))
    when(siteConfig.cleanInactiveUsers).thenReturn(true)

    cleaner.cleanInactiveUsers()

    verify(userDao).deleteUsers(Seq(1, 2, 3))
    verify(userDao).deleteUsers(Seq(4, 5))

  test("largeCandidateSetDeletedInSingleCall"):
    val ids = (1 to UserDao.DeleteBatchSize * 3).toSeq
    when(userDao.getDeletableInactiveUserIds).thenReturn(ids)
    when(siteConfig.cleanInactiveUsers).thenReturn(true)

    cleaner.cleanInactiveUsers()

    verify(userDao, times(1)).deleteUsers(ids)

  test("deleteFailurePropagates"):
    when(userDao.getDeletableInactiveUserIds).thenReturn(Seq(1, 2, 3))
    when(siteConfig.cleanInactiveUsers).thenReturn(true)
    when(userDao.deleteUsers(Seq(1, 2, 3))).thenThrow(new RuntimeException("batch failed"))

    intercept[RuntimeException] {
      cleaner.cleanInactiveUsers()
    }

  test("deleteInactivatedWorksRegardlessOfFlag"):
    when(siteConfig.cleanInactiveUsers).thenReturn(false)
    when(userDao.deleteInactivatedAccounts()).thenReturn((1, 2))
    cleaner.deleteInactivated()

    when(siteConfig.cleanInactiveUsers).thenReturn(true)
    when(userDao.deleteInactivatedAccounts()).thenReturn((3, 4))
    cleaner.deleteInactivated()

    verify(userDao, times(2)).deleteInactivatedAccounts()

  test("deleteInactivatedFailurePropagates"):
    when(userDao.deleteInactivatedAccounts()).thenThrow(new RuntimeException("delete failed"))

    intercept[RuntimeException] {
      cleaner.deleteInactivated()
    }
