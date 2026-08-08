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
package ru.org.linux.gallery

import org.junit.Test
import org.mockito.{ArgumentCaptor, Mockito}
import org.mockito.Mockito.{never, verify, when}
import org.mockito.ArgumentMatchers.any
import ru.org.linux.spring.SiteConfig

/** Юнит-тесты для [[OldImageCleaner]] на моках DAO и SiteConfig. */
class OldImageCleanerTest:
  private val siteConfig = Mockito.mock(classOf[SiteConfig])
  private val imageDao = Mockito.mock(classOf[ImageDao])
  private val imageService = Mockito.mock(classOf[ImageService])
  private val cleaner = OldImageCleaner(siteConfig, imageDao, imageService)

  private val purgeCaptor = ArgumentCaptor.forClass(classOf[Seq[Int]])

  private def image(id: Int, topicId: Int = 1, deleted: Boolean = false): Image =
    Image(id, topicId, s"images/$id/original.jpg", deleted = deleted, purged = false)

  @Test
  def disabledFlagLogsCandidatesDoesNotDelete(): Unit =
    val img1 = image(id = 1, topicId = 10, deleted = false)
    val img2 = image(id = 2, topicId = 20, deleted = true)

    when(siteConfig.cleanOldImages).thenReturn(false)
    when(imageDao.imagesOfOldDeletedTopics(any(classOf[Int]))).thenReturn(Seq(img1))
    when(imageDao.deletedImagesOfOldTopics(any(classOf[Int]))).thenReturn(Seq(img2))

    cleaner.cleanOldImages()

    // dry-run: DAO queries выполняются для логгирования кандидатов
    verify(imageDao).imagesOfOldDeletedTopics(any(classOf[Int]))
    verify(imageDao).deletedImagesOfOldTopics(any(classOf[Int]))
    // но файлы не удаляются и markPurged не вызывается
    verify(imageService, never()).purgeImageFiles(any(classOf[Image]))
    verify(imageService, never()).markPurged(any(classOf[Seq[Int]]))

  @Test
  def caseADeletedTopicImagesArePurged(): Unit =
    val img1 = image(id = 1, topicId = 10, deleted = false)
    val img2 = image(id = 2, topicId = 10, deleted = false)

    when(siteConfig.cleanOldImages).thenReturn(true)
    when(imageDao.imagesOfOldDeletedTopics(any(classOf[Int]))).thenReturn(Seq(img1, img2))
    when(imageDao.deletedImagesOfOldTopics(any(classOf[Int]))).thenReturn(Seq.empty)
    when(imageService.purgeImageFiles(any(classOf[Image]))).thenReturn(true)

    cleaner.cleanOldImages()

    verify(imageService).purgeImageFiles(img1)
    verify(imageService).purgeImageFiles(img2)
    verify(imageService).markPurged(purgeCaptor.capture())
    val marked = purgeCaptor.getValue
    assert(marked.contains(1) && marked.contains(2) && marked.size == 2)

  @Test
  def caseBDeletedImagesArePurged(): Unit =
    val img1 = image(id = 5, topicId = 20, deleted = true)
    val img2 = image(id = 6, topicId = 21, deleted = true)

    when(siteConfig.cleanOldImages).thenReturn(true)
    when(imageDao.imagesOfOldDeletedTopics(any(classOf[Int]))).thenReturn(Seq.empty)
    when(imageDao.deletedImagesOfOldTopics(any(classOf[Int]))).thenReturn(Seq(img1, img2))
    when(imageService.purgeImageFiles(any(classOf[Image]))).thenReturn(true)

    cleaner.cleanOldImages()

    verify(imageService).purgeImageFiles(img1)
    verify(imageService).purgeImageFiles(img2)
    verify(imageService).markPurged(purgeCaptor.capture())
    val marked = purgeCaptor.getValue
    assert(marked.contains(5) && marked.contains(6) && marked.size == 2)

  @Test
  def emptyCandidatesDoesNotMarkPurged(): Unit =
    when(siteConfig.cleanOldImages).thenReturn(true)
    when(imageDao.imagesOfOldDeletedTopics(any(classOf[Int]))).thenReturn(Seq.empty)
    when(imageDao.deletedImagesOfOldTopics(any(classOf[Int]))).thenReturn(Seq.empty)

    cleaner.cleanOldImages()

    verify(imageService, never()).purgeImageFiles(any(classOf[Image]))
    verify(imageService, never()).markPurged(any(classOf[Seq[Int]]))

  @Test
  def failedPurgeExcludedFromMark(): Unit =
    val img1 = image(id = 1, deleted = false)
    val img2 = image(id = 2, deleted = false)

    when(siteConfig.cleanOldImages).thenReturn(true)
    when(imageDao.imagesOfOldDeletedTopics(any(classOf[Int]))).thenReturn(Seq(img1, img2))
    when(imageDao.deletedImagesOfOldTopics(any(classOf[Int]))).thenReturn(Seq.empty)
    when(imageService.purgeImageFiles(img1)).thenReturn(true)
    when(imageService.purgeImageFiles(img2)).thenReturn(false)

    cleaner.cleanOldImages()

    verify(imageService).purgeImageFiles(img1)
    verify(imageService).purgeImageFiles(img2)
    verify(imageService).markPurged(purgeCaptor.capture())
    val marked = purgeCaptor.getValue
    assert(marked.contains(1) && !marked.contains(2) && marked.size == 1)

  @Test
  def caseAandBAreBothProcessed(): Unit =
    val imgA = image(id = 1, topicId = 10, deleted = false)
    val imgB = image(id = 2, topicId = 20, deleted = true)

    when(siteConfig.cleanOldImages).thenReturn(true)
    when(imageDao.imagesOfOldDeletedTopics(any(classOf[Int]))).thenReturn(Seq(imgA))
    when(imageDao.deletedImagesOfOldTopics(any(classOf[Int]))).thenReturn(Seq(imgB))
    when(imageService.purgeImageFiles(any(classOf[Image]))).thenReturn(true)

    cleaner.cleanOldImages()

    verify(imageService).purgeImageFiles(imgA)
    verify(imageService).purgeImageFiles(imgB)
    verify(imageService).markPurged(purgeCaptor.capture())
    val marked = purgeCaptor.getValue
    assert(marked.size == 2)
