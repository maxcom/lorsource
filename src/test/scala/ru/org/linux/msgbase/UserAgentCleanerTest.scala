/*
 * Copyright 1998-2026 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package ru.org.linux.msgbase

import munit.FunSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, never, times, verify, when}
import ru.org.linux.spring.SiteConfig

/** Юнит-тесты для [[UserAgentCleaner]] на моках DAO и SiteConfig. */
class UserAgentCleanerTest extends FunSuite:
  // В JUnit на каждый тест создавался новый экземпляр с свежими моками; в munit экземпляр один,
  // поэтому моки и cleaner пересоздаются перед каждым тестом (иначе verify(..., times(...)) видел бы
  // вызовы из предыдущих тестов)
  private var siteConfig: SiteConfig = scala.compiletime.uninitialized
  private var userAgentDao: UserAgentDao = scala.compiletime.uninitialized
  private var cleaner: UserAgentCleaner = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    siteConfig = mock(classOf[SiteConfig])
    userAgentDao = mock(classOf[UserAgentDao])
    cleaner = UserAgentCleaner(siteConfig, userAgentDao)

  test("noCandidatesDoesNothing"):
    when(userAgentDao.getUnreferencedUserAgentIds).thenReturn(Seq.empty[Int])

    cleaner.cleanUserAgents()

    verify(userAgentDao, never()).purgeUnreferencedUserAgents(any(classOf[Seq[Int]]))

  test("flagOffDoesNotDelete"):
    when(userAgentDao.getUnreferencedUserAgentIds).thenReturn(Seq(1, 2, 3))
    when(siteConfig.cleanUserAgents).thenReturn(false)

    cleaner.cleanUserAgents()

    verify(userAgentDao, never()).purgeUnreferencedUserAgents(any(classOf[Seq[Int]]))

  test("flagOnDeletesCandidates"):
    when(userAgentDao.getUnreferencedUserAgentIds).thenReturn(Seq(1, 2, 3))
    when(siteConfig.cleanUserAgents).thenReturn(true)
    when(userAgentDao.purgeUnreferencedUserAgents(Seq(1, 2, 3))).thenReturn(3)

    cleaner.cleanUserAgents()

    verify(userAgentDao).purgeUnreferencedUserAgents(Seq(1, 2, 3))

  test("flagOnDeletesInBatches"):
    val ids = (1 to UserAgentCleaner.BatchSize * 3).toSeq
    when(userAgentDao.getUnreferencedUserAgentIds).thenReturn(ids)
    when(siteConfig.cleanUserAgents).thenReturn(true)
    when(userAgentDao.purgeUnreferencedUserAgents(any(classOf[Seq[Int]]))).thenReturn(UserAgentCleaner.BatchSize)

    cleaner.cleanUserAgents()

    verify(userAgentDao, times(3)).purgeUnreferencedUserAgents(any(classOf[Seq[Int]]))
    verify(userAgentDao).purgeUnreferencedUserAgents((1 to UserAgentCleaner.BatchSize).toSeq)
    verify(userAgentDao).purgeUnreferencedUserAgents(
      (UserAgentCleaner.BatchSize + 1 to UserAgentCleaner.BatchSize * 2).toSeq)

  test("purgeFailurePropagates"):
    when(userAgentDao.getUnreferencedUserAgentIds).thenReturn(Seq(1, 2, 3))
    when(siteConfig.cleanUserAgents).thenReturn(true)
    when(userAgentDao.purgeUnreferencedUserAgents(Seq(1, 2, 3))).thenThrow(new RuntimeException("batch failed"))

    intercept[RuntimeException] {
      cleaner.cleanUserAgents()
    }
