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

import com.typesafe.scalalogging.StrictLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.org.linux.spring.SiteConfig

/** Периодическое физическое удаление файлов старых удалённых изображений. Запускается раз в день.
  *
  * Удаляются файлы для двух категорий изображений (порог — 3 года):
  *   - '''Случай A''': все изображения топиков, удалённых более 3 лет назад (по `del_info.deldate`). Файлы удаляются
  *     независимо от флага `images.deleted` (топик уже удалён, картинки больше не нужны).
  *   - '''Случай B''': soft-deleted изображения (`images.deleted=true`) активных топиков, у которых `topics.lastmod`
  *     старее 3 лет.
  *
  * После успешного удаления файлов изображения помечаются `images.purged=true`, чтобы:
  *   - чистильщик не пересканировал их повторно (идемпотность);
  *   - история редактирования могла показать плейсхолдер «Изображение удалено» без обращения к диску.
  *
  * История редактирования не чистится: в ней остаются ссылки на удалённые изображения, которые рендерятся как
  * «Изображение удалено (id=N)» без самого изображения.
  *
  * При выключенном флаге `cleanOldImages` (по умолчанию) файлы не удаляются, но все найденные кандидаты логгируются
  * (dry-run режим).
  */
@Component
class OldImageCleaner(siteConfig: SiteConfig, imageDao: ImageDao, imageService: ImageService) extends StrictLogging:

  /** Срок хранения файлов после удаления топика / soft-delete изображения. */
  private val RetentionYears = 3

  @Scheduled(cron = "0 0 5 * * *")
  def cleanOldImages(): Unit =
    val deleteMode = siteConfig.cleanOldImages

    logger.info(s"OldImageCleaner start; retention=$RetentionYears years; delete mode: $deleteMode")

    val caseA = imageDao.imagesOfOldDeletedTopics(RetentionYears)
    val caseB = imageDao.deletedImagesOfOldTopics(RetentionYears)

    logger.info(s"OldImageCleaner candidates: deleted-topics=${caseA.size}, deleted-images=${caseB.size}")

    if !deleteMode then
      caseA.foreach { image =>
        logger.info(s"Would purge image id=${image.id} topic=${image.topicId} (deleted topic, dry-run)")
      }
      caseB.foreach { image =>
        logger.info(s"Would purge image id=${image.id} topic=${image.topicId} (deleted image, dry-run)")
      }
      logger.info(s"OldImageCleaner done (dry-run); candidates: ${caseA.size + caseB.size}")
    else
      val purged = scala.collection.mutable.ListBuffer.empty[Int]

      caseA.foreach { image =>
        if imageService.purgeImageFiles(image) then
          purged += image.id
      }

      caseB.foreach { image =>
        if imageService.purgeImageFiles(image) then
          purged += image.id
      }

      if purged.nonEmpty then
        imageService.markPurged(purged.toSeq)
        logger.info(s"OldImageCleaner marked purged: ${purged.size}")

      logger.info(s"OldImageCleaner done; purged files: ${purged.size}")
