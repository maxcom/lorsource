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
  * Кандидаты определяются в [[TopicDao.getDeletableDeletedTopicIds]] и [[TopicDao.getDeletableDraftTopicIds]]:
  *   - '''Удалённые топики''': топик удалён более 3 лет назад (по `del_info.deldate`), не имеет комментариев (включая
  *     удалённые) и непрочищенных картинок, а его автор не заходил на сайт более 10 лет, либо заблокирован и не заходил
  *     более 3 лет, либо не имеет дат регистрации и последнего входа (в т.ч. anonymous);
  *   - '''Черновики''': неопубликованный (`draft`), не помеченный как удалённый, без комментариев, автор удовлетворяет
  *     тем же критериям неактивности. Файлы картинок неопубликованных черновиков ([[ru.org.linux.gallery.OldImageCleaner]]
  *     их не обрабатывает; мягко-удалённые черновики покрывает его Случай A) удаляются непосредственно перед purge
  *     батча — и только для топиков, остающихся черновиками на момент подготовки батча: выборка картинок в
  *     [[ImageDao.unpurgedImagesOfDrafts]] перепроверяет `draft`/`deleted`, сужая окно гонки с публикацией. Топики с
  *     неуспешно удалёнными файлами пропускаются проверкой в [[TopicDao.purgeDeletedTopics]] и повторяются следующим
  *     запуском; там же — под блокировкой — перепроверяется неактивность автора (для обеих категорий кандидатов):
  *     вернувшийся на сайт между выборкой кандидатов и purge автор сохраняет и черновики, и старые удалённые топики.
  *     Исключение — файлы картинок черновика, физически удалённые [[purgeDraftImageFiles]] до этой перепроверки (она
  *     выполняется уже после удаления файлов): сам черновик выживает, но его картинки в этом узком окне теряются без
  *     восстановления.
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
