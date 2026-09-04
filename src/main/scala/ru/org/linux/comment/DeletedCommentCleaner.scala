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

import com.typesafe.scalalogging.StrictLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.org.linux.spring.SiteConfig

/** Периодическое окончательное удаление старых удалённых комментариев неактивных пользователей.
  *
  * Кандидаты определяются в [[CommentDao.getDeletableDeletedCommentIds]]: комментарий удалён более 3 лет назад (по
  * `del_info.deldate`), не имеет ответов (включая удалённые), а его автор не заходил на сайт более 10 лет, либо
  * заблокирован и не заходил более 3 лет, либо не имеет дат регистрации и последнего входа (в т.ч. anonymous).
  * Комментарии с удалёнными ответами вычищаются постепенно: сначала листья цепочки, затем их родители.
  *
  * При выключенном флаге `cleanOldDeletedComments` (по умолчанию) вместо удаления кандидаты только логгируются
  * (dry-run). При ошибке удаления исключение пробрасывается наружу — его обработает обработчик ошибок планировщика (лог
  * + письмо администратору); оставшиеся кандидаты будут удалены следующим запуском.
  */
@Component
class DeletedCommentCleaner(siteConfig: SiteConfig, commentDao: CommentDao) extends StrictLogging:

  @Scheduled(cron = "0 0 6 * * *")
  def cleanDeletedComments(): Unit =
    val ids = commentDao.getDeletableDeletedCommentIds

    if ids.isEmpty then
      logger.info("DeletedCommentCleaner: no candidates")
    else if siteConfig.cleanOldDeletedComments then
      var deleted = 0
      ids
        .grouped(DeletedCommentCleaner.BatchSize)
        .foreach { batch =>
          val purged = commentDao.purgeDeletedComments(batch)
          deleted += purged
          logger.info(s"DeletedCommentCleaner: purged $purged of ${batch.size}: ${batch.mkString(", ")}")
        }
      logger.info(s"DeletedCommentCleaner: deleted $deleted of ${ids.size} candidates")
    else
      logger.info(s"DeletedCommentCleaner: would delete ${ids.size} comments")
      ids
        .grouped(DeletedCommentCleaner.BatchSize)
        .foreach { batch =>
          logger.info(s"DeletedCommentCleaner candidates: ${batch.mkString(", ")}")
        }

object DeletedCommentCleaner:
  val BatchSize = 500
