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
package ru.org.linux.topic

import munit.FunSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, never, times, verify, when}
import ru.org.linux.gallery.{Image, ImageDao, ImageService}
import ru.org.linux.spring.SiteConfig

/** Юнит-тесты для [[DeletedTopicCleaner]] на моках DAO и SiteConfig. */
class DeletedTopicCleanerTest extends FunSuite:
  // В JUnit на каждый тест создавался новый экземпляр с свежими моками; в munit экземпляр один,
  // поэтому моки и cleaner пересоздаются перед каждым тестом (иначе verify(..., times(...)) видел бы
  // вызовы из предыдущих тестов)
  private var siteConfig: SiteConfig = scala.compiletime.uninitialized
  private var topicDao: TopicDao = scala.compiletime.uninitialized
  private var imageDao: ImageDao = scala.compiletime.uninitialized
  private var imageService: ImageService = scala.compiletime.uninitialized
  private var cleaner: DeletedTopicCleaner = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    siteConfig = mock(classOf[SiteConfig])
    topicDao = mock(classOf[TopicDao])
    imageDao = mock(classOf[ImageDao])
    imageService = mock(classOf[ImageService])
    cleaner = DeletedTopicCleaner(siteConfig, topicDao, imageDao, imageService)

  private def image(id: Int, topicId: Int): Image =
    Image(id = id, topicId = topicId, original = s"images/$id/100px.jpg", deleted = false, purged = false)

  test("noCandidatesDoesNothing"):
    when(topicDao.getDeletableDeletedTopicIds).thenReturn(Seq.empty[Int])
    when(topicDao.getDeletableDraftTopicIds).thenReturn(Seq.empty[Int])

    cleaner.cleanDeletedTopics()

    verify(topicDao, never()).purgeDeletedTopics(any(classOf[Seq[Int]]))
    verify(imageService, never()).purgeImageFiles(any(classOf[Image]))

  test("flagOffDoesNotDelete"):
    when(topicDao.getDeletableDeletedTopicIds).thenReturn(Seq(1, 2, 3))
    when(topicDao.getDeletableDraftTopicIds).thenReturn(Seq.empty[Int])
    when(siteConfig.cleanOldDeletedTopics).thenReturn(false)

    cleaner.cleanDeletedTopics()

    verify(topicDao, never()).purgeDeletedTopics(any(classOf[Seq[Int]]))

  test("flagOnDeletesCandidates"):
    when(topicDao.getDeletableDeletedTopicIds).thenReturn(Seq(1, 2, 3))
    when(topicDao.getDeletableDraftTopicIds).thenReturn(Seq.empty[Int])
    when(siteConfig.cleanOldDeletedTopics).thenReturn(true)
    when(topicDao.purgeDeletedTopics(Seq(1, 2, 3))).thenReturn(3)

    cleaner.cleanDeletedTopics()

    verify(topicDao).purgeDeletedTopics(Seq(1, 2, 3))

  test("flagOnDeletesInBatches"):
    val ids = (1 to DeletedTopicCleaner.BatchSize * 3).toSeq
    when(topicDao.getDeletableDeletedTopicIds).thenReturn(ids)
    when(topicDao.getDeletableDraftTopicIds).thenReturn(Seq.empty[Int])
    when(siteConfig.cleanOldDeletedTopics).thenReturn(true)
    when(topicDao.purgeDeletedTopics(any(classOf[Seq[Int]]))).thenReturn(DeletedTopicCleaner.BatchSize)

    cleaner.cleanDeletedTopics()

    verify(topicDao, times(3)).purgeDeletedTopics(any(classOf[Seq[Int]]))
    verify(topicDao).purgeDeletedTopics((1 to DeletedTopicCleaner.BatchSize).toSeq)
    verify(topicDao).purgeDeletedTopics((DeletedTopicCleaner.BatchSize + 1 to DeletedTopicCleaner.BatchSize * 2).toSeq)

  test("purgeFailurePropagates"):
    when(topicDao.getDeletableDeletedTopicIds).thenReturn(Seq(1, 2, 3))
    when(topicDao.getDeletableDraftTopicIds).thenReturn(Seq.empty[Int])
    when(siteConfig.cleanOldDeletedTopics).thenReturn(true)
    when(topicDao.purgeDeletedTopics(Seq(1, 2, 3))).thenThrow(new RuntimeException("batch failed"))

    intercept[RuntimeException] {
      cleaner.cleanDeletedTopics()
    }

  test("flagOnDeletesDrafts"):
    when(topicDao.getDeletableDeletedTopicIds).thenReturn(Seq.empty[Int])
    when(topicDao.getDeletableDraftTopicIds).thenReturn(Seq(10, 11))
    when(siteConfig.cleanOldDeletedTopics).thenReturn(true)
    when(imageDao.unpurgedImagesOfDrafts(Seq(10, 11))).thenReturn(Seq.empty[Image])
    when(topicDao.purgeDeletedTopics(Seq(10, 11))).thenReturn(2)

    cleaner.cleanDeletedTopics()

    verify(imageDao).unpurgedImagesOfDrafts(Seq(10, 11))
    verify(topicDao).purgeDeletedTopics(Seq(10, 11))

  test("flagOnPurgesDraftImagesBeforePurge"):
    val img1 = image(101, 10)
    val img2 = image(102, 11)
    when(topicDao.getDeletableDeletedTopicIds).thenReturn(Seq.empty[Int])
    when(topicDao.getDeletableDraftTopicIds).thenReturn(Seq(10, 11))
    when(siteConfig.cleanOldDeletedTopics).thenReturn(true)
    when(imageDao.unpurgedImagesOfDrafts(Seq(10, 11))).thenReturn(Seq(img1, img2))
    when(imageService.purgeImageFiles(img1)).thenReturn(true)
    when(imageService.purgeImageFiles(img2)).thenReturn(true)
    when(topicDao.purgeDeletedTopics(Seq(10, 11))).thenReturn(2)

    val inOrder = org.mockito.Mockito.inOrder(imageService, topicDao)

    cleaner.cleanDeletedTopics()

    inOrder.verify(imageService).purgeImageFiles(img1)
    inOrder.verify(imageService).purgeImageFiles(img2)
    inOrder.verify(imageService).markPurged(Seq(101, 102))
    inOrder.verify(topicDao).purgeDeletedTopics(Seq(10, 11))

  test("draftImageFailureStillMarksSuccessfulAndPurges"):
    val img1 = image(101, 10)
    val img2 = image(102, 11)
    when(topicDao.getDeletableDeletedTopicIds).thenReturn(Seq.empty[Int])
    when(topicDao.getDeletableDraftTopicIds).thenReturn(Seq(10, 11))
    when(siteConfig.cleanOldDeletedTopics).thenReturn(true)
    when(imageDao.unpurgedImagesOfDrafts(Seq(10, 11))).thenReturn(Seq(img1, img2))
    when(imageService.purgeImageFiles(img1)).thenReturn(false)
    when(imageService.purgeImageFiles(img2)).thenReturn(true)
    when(topicDao.purgeDeletedTopics(Seq(10, 11))).thenReturn(1)

    cleaner.cleanDeletedTopics()

    verify(imageService).markPurged(Seq(102))
    verify(topicDao).purgeDeletedTopics(Seq(10, 11))

  test("flagOffDoesNotPurgeDraftImages"):
    when(topicDao.getDeletableDeletedTopicIds).thenReturn(Seq.empty[Int])
    when(topicDao.getDeletableDraftTopicIds).thenReturn(Seq(10, 11))
    when(siteConfig.cleanOldDeletedTopics).thenReturn(false)

    cleaner.cleanDeletedTopics()

    verify(imageDao, never()).unpurgedImagesOfDrafts(any(classOf[Seq[Int]]))
    verify(imageService, never()).purgeImageFiles(any(classOf[Image]))
    verify(topicDao, never()).purgeDeletedTopics(any(classOf[Seq[Int]]))

end DeletedTopicCleanerTest
