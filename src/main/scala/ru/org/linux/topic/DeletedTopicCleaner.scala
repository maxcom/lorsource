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

import com.typesafe.scalalogging.StrictLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.org.linux.gallery.{ImageDao, ImageService}
import ru.org.linux.spring.SiteConfig

/** Периодическое окончательное удаление старых удалённых топиков и черновиков неактивных пользователей.
  *
  * Критерии описаны в doc/data-retention.md
  *
  * При выключенном флаге `cleanOldDeletedTopics` (по умолчанию) вместо удаления кандидаты только логгируются (dry-run).
  * При ошибке удаления исключение пробрасывается наружу — его обработает обработчик ошибок планировщика (лог + письмо
  * администратору); оставшиеся кандидаты будут удалены следующим запуском.
  */
@Component
class DeletedTopicCleaner(siteConfig: SiteConfig, topicDao: TopicDao, imageDao: ImageDao, imageService: ImageService)
    extends StrictLogging:

  @Scheduled(cron = "0 30 6 * * *")
  def cleanDeletedTopics(): Unit =
    cleanCandidates(topicDao.getDeletableDeletedTopicIds, "topics", _ => ())
    cleanCandidates(topicDao.getDeletableDraftTopicIds, "drafts", purgeDraftImageFiles)

  /** Батчевое окончательное удаление кандидатов: dry-run лог при выключенном флаге, иначе подготовка батча (удаление
    * файлов картинок черновиков) и purge.
    */
  private def cleanCandidates(ids: Seq[Int], kind: String, prepareBatch: Seq[Int] => Unit): Unit =
    if ids.isEmpty then
      logger.info(s"DeletedTopicCleaner: no $kind candidates")
    else if !siteConfig.cleanOldDeletedTopics then
      logger.info(s"DeletedTopicCleaner: would delete ${ids.size} $kind")
      ids
        .grouped(DeletedTopicCleaner.BatchSize)
        .foreach { batch =>
          logger.info(s"DeletedTopicCleaner $kind candidates: ${batch.mkString(", ")}")
        }
    else
      var deleted = 0
      ids
        .grouped(DeletedTopicCleaner.BatchSize)
        .foreach { batch =>
          prepareBatch(batch)
          val purged = topicDao.purgeDeletedTopics(batch)
          deleted += purged
          logger.info(s"DeletedTopicCleaner: purged $purged of ${batch.size} $kind: ${batch.mkString(", ")}")
        }
      logger.info(s"DeletedTopicCleaner: deleted $deleted of ${ids.size} $kind")

  /** Физически удаляет файлы непрочищенных картинок черновиков батча и помечает их `purged`. Удаляются только файлы
    * топиков, остающихся черновиками на момент выборки ([[ImageDao.unpurgedImagesOfDrafts]]). Ошибки удаления
    * отдельных файлов логгируются; соответствующие топики будут пропущены [[TopicDao.purgeDeletedTopics]].
    */
  private def purgeDraftImageFiles(batch: Seq[Int]): Unit =
    val images = imageDao.unpurgedImagesOfDrafts(batch)

    if images.nonEmpty then
      val purged = images.filter(image => imageService.purgeImageFiles(image)).map(_.id)

      if purged.nonEmpty then
        imageService.markPurged(purged)
        logger.info(s"DeletedTopicCleaner: purged ${purged.size} of ${images.size} draft image files")

object DeletedTopicCleaner:
  val BatchSize = 500
