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

import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, never, times, verify, when}
import ru.org.linux.spring.SiteConfig

/** Юнит-тесты для [[DeletedCommentCleaner]] на моках DAO и SiteConfig. */
class DeletedCommentCleanerTest:
  private val siteConfig = mock(classOf[SiteConfig])
  private val commentDao = mock(classOf[CommentDao])
  private val cleaner = DeletedCommentCleaner(siteConfig, commentDao)

  @Test
  def noCandidatesDoesNothing(): Unit =
    when(commentDao.getDeletableDeletedCommentIds).thenReturn(Seq.empty[Int])

    cleaner.cleanDeletedComments()

    verify(commentDao, never()).purgeDeletedComments(any(classOf[Seq[Int]]))

  @Test
  def flagOffDoesNotDelete(): Unit =
    when(commentDao.getDeletableDeletedCommentIds).thenReturn(Seq(1, 2, 3))
    when(siteConfig.cleanOldDeletedComments).thenReturn(false)

    cleaner.cleanDeletedComments()

    verify(commentDao, never()).purgeDeletedComments(any(classOf[Seq[Int]]))

  @Test
  def flagOnDeletesCandidates(): Unit =
    when(commentDao.getDeletableDeletedCommentIds).thenReturn(Seq(1, 2, 3))
    when(siteConfig.cleanOldDeletedComments).thenReturn(true)
    when(commentDao.purgeDeletedComments(Seq(1, 2, 3))).thenReturn(3)

    cleaner.cleanDeletedComments()

    verify(commentDao).purgeDeletedComments(Seq(1, 2, 3))

  @Test
  def flagOnDeletesInBatches(): Unit =
    val ids = (1 to DeletedCommentCleaner.BatchSize * 3).toSeq
    when(commentDao.getDeletableDeletedCommentIds).thenReturn(ids)
    when(siteConfig.cleanOldDeletedComments).thenReturn(true)
    when(commentDao.purgeDeletedComments(any(classOf[Seq[Int]]))).thenReturn(DeletedCommentCleaner.BatchSize)

    cleaner.cleanDeletedComments()

    verify(commentDao, times(3)).purgeDeletedComments(any(classOf[Seq[Int]]))
    verify(commentDao).purgeDeletedComments((1 to DeletedCommentCleaner.BatchSize).toSeq)
    verify(commentDao).purgeDeletedComments(
      (DeletedCommentCleaner.BatchSize + 1 to DeletedCommentCleaner.BatchSize * 2).toSeq)

  @Test
  def failedBatchDoesNotAbortOthers(): Unit =
    val ids = (1 to DeletedCommentCleaner.BatchSize * 2).toSeq
    when(commentDao.getDeletableDeletedCommentIds).thenReturn(ids)
    when(siteConfig.cleanOldDeletedComments).thenReturn(true)
    when(commentDao.purgeDeletedComments((1 to DeletedCommentCleaner.BatchSize).toSeq)).thenThrow(
      new RuntimeException("batch failed"))
    when(
      commentDao.purgeDeletedComments(
        (DeletedCommentCleaner.BatchSize + 1 to DeletedCommentCleaner.BatchSize * 2).toSeq)).thenReturn(
      DeletedCommentCleaner.BatchSize)

    cleaner.cleanDeletedComments()

    verify(commentDao, times(2)).purgeDeletedComments(any(classOf[Seq[Int]]))
