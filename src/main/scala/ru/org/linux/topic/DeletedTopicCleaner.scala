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
import ru.org.linux.spring.SiteConfig

/** Периодическое окончательное удаление старых удалённых топиков без комментариев неактивных пользователей.
  *
  * Кандидаты определяются в [[TopicDao.getDeletableDeletedTopicIds]]: топик удалён более 3 лет назад (по
  * `del_info.deldate`), не имеет комментариев (включая удалённые) и непрочищенных картинок, а его автор не заходил на
  * сайт более 10 лет, либо заблокирован и не заходил более 3 лет, либо не имеет дат регистрации и последнего входа (в
  * т.ч. anonymous).
  *
  * При выключенном флаге `cleanOldDeletedTopics` (по умолчанию) вместо удаления кандидаты только логгируются (dry-run).
  * При ошибке удаления исключение пробрасывается наружу — его обработает обработчик ошибок планировщика (лог + письмо
  * администратору); оставшиеся кандидаты будут удалены следующим запуском.
  */
@Component
class DeletedTopicCleaner(siteConfig: SiteConfig, topicDao: TopicDao) extends StrictLogging:

  @Scheduled(cron = "0 30 6 * * *")
  def cleanDeletedTopics(): Unit =
    val ids = topicDao.getDeletableDeletedTopicIds

    if ids.isEmpty then
      logger.info("DeletedTopicCleaner: no candidates")
    else if siteConfig.cleanOldDeletedTopics then
      var deleted = 0
      ids
        .grouped(DeletedTopicCleaner.BatchSize)
        .foreach { batch =>
          val purged = topicDao.purgeDeletedTopics(batch)
          deleted += purged
          logger.info(s"DeletedTopicCleaner: purged $purged of ${batch.size}: ${batch.mkString(", ")}")
        }
      logger.info(s"DeletedTopicCleaner: deleted $deleted of ${ids.size} candidates")
    else
      logger.info(s"DeletedTopicCleaner: would delete ${ids.size} topics")
      ids
        .grouped(DeletedTopicCleaner.BatchSize)
        .foreach { batch =>
          logger.info(s"DeletedTopicCleaner candidates: ${batch.mkString(", ")}")
        }

object DeletedTopicCleaner:
  val BatchSize = 500
