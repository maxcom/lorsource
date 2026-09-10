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
package ru.org.linux.comment

import munit.FunSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, never, times, verify, when}
import ru.org.linux.spring.SiteConfig

/** Юнит-тесты для [[DeletedCommentCleaner]] на моках DAO и SiteConfig. */
class DeletedCommentCleanerTest extends FunSuite:
  // В JUnit на каждый тест создавался новый экземпляр с свежими моками; в munit экземпляр один,
  // поэтому моки и cleaner пересоздаются перед каждым тестом (иначе verify(..., times(...)) видел бы
  // вызовы из предыдущих тестов)
  private var siteConfig: SiteConfig = scala.compiletime.uninitialized
  private var commentDao: CommentDao = scala.compiletime.uninitialized
  private var cleaner: DeletedCommentCleaner = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    siteConfig = mock(classOf[SiteConfig])
    commentDao = mock(classOf[CommentDao])
    cleaner = DeletedCommentCleaner(siteConfig, commentDao)

  test("noCandidatesDoesNothing"):
    when(commentDao.getDeletableDeletedCommentIds).thenReturn(Seq.empty[Int])

    cleaner.cleanDeletedComments()

    verify(commentDao, never()).purgeDeletedComments(any(classOf[Seq[Int]]))

  test("flagOffDoesNotDelete"):
    when(commentDao.getDeletableDeletedCommentIds).thenReturn(Seq(1, 2, 3))
    when(siteConfig.cleanOldDeletedComments).thenReturn(false)

    cleaner.cleanDeletedComments()

    verify(commentDao, never()).purgeDeletedComments(any(classOf[Seq[Int]]))

  test("flagOnDeletesCandidates"):
    when(commentDao.getDeletableDeletedCommentIds).thenReturn(Seq(1, 2, 3))
    when(siteConfig.cleanOldDeletedComments).thenReturn(true)
    when(commentDao.purgeDeletedComments(Seq(1, 2, 3))).thenReturn(3)

    cleaner.cleanDeletedComments()

    verify(commentDao).purgeDeletedComments(Seq(1, 2, 3))

  test("flagOnDeletesInBatches"):
    val ids = (1 to DeletedCommentCleaner.BatchSize * 3).toSeq
    when(commentDao.getDeletableDeletedCommentIds).thenReturn(ids)
    when(siteConfig.cleanOldDeletedComments).thenReturn(true)
    when(commentDao.purgeDeletedComments(any(classOf[Seq[Int]]))).thenReturn(DeletedCommentCleaner.BatchSize)

    cleaner.cleanDeletedComments()

    verify(commentDao, times(3)).purgeDeletedComments(any(classOf[Seq[Int]]))
    verify(commentDao).purgeDeletedComments((1 to DeletedCommentCleaner.BatchSize).toSeq)
    verify(commentDao).purgeDeletedComments(
      (DeletedCommentCleaner.BatchSize + 1 to DeletedCommentCleaner.BatchSize * 2).toSeq)

  test("purgeFailurePropagates"):
    when(commentDao.getDeletableDeletedCommentIds).thenReturn(Seq(1, 2, 3))
    when(siteConfig.cleanOldDeletedComments).thenReturn(true)
    when(commentDao.purgeDeletedComments(Seq(1, 2, 3))).thenThrow(new RuntimeException("batch failed"))

    intercept[RuntimeException] {
      cleaner.cleanDeletedComments()
    }
