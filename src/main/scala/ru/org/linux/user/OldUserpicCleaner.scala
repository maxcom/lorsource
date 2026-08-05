/*
 * Copyright 1998-2026 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.org.linux.user

import com.typesafe.scalalogging.StrictLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.org.linux.spring.SiteConfig

import java.nio.file.{Files, Path}
import java.nio.file.attribute.FileTime
import java.time.{Duration, OffsetDateTime}
import scala.jdk.CollectionConverters.IteratorHasAsScala

/** Периодическое удаление старых файлов аватарок пользователей.
  *
  * Удаляются файлы, которые:
  *   - не являются активными (имя не совпадает с `users.photo` никакого пользователя);
  *   - не загружались в течение последнего часа (защита от race conditions при загрузке);
  *   - упомянуты в `user_log` (через `old_userpic` или `new_userpic`) и последнее упоминание было более чем 3 года
  *     назад; или не упомянуты вовсе (загружены до 2013 года, тогда в `user_log` ничего не фиксировалось).
  *
  * При включённом флаге `cleanOldUserpics` файлы удаляются с диска; при выключенном — только логгируются кандидаты.
  * Каталог перебирается потоком через `Files.newDirectoryStream`, чтобы не загружать весь список файлов в память;
  * запросы к `user_log` выполняются батчами по `BatchSize` файлов.
  */
@Component
class OldUserpicCleaner(siteConfig: SiteConfig, userDao: UserDao, userLogDao: UserLogDao) extends StrictLogging:
  /** Имя файла аватарки: или старый формат `{userid}.{ext}`, или современный `{userid}:{random}.{ext}`.
    */
  private val AvatarNamePattern = """^(\d+)(?::(-?\d+))?\.\w+$""".r

  /** Каталог хранения файлов аватарок. */
  private def photosDir: Path = Path.of(siteConfig.getUploadPath, "photos")

  /** Размер батча имён при запросе last-упоминания в user_log. Ограничивает память при обработке и число круглых
    * запросов к БД.
    */
  private val BatchSize = 500

  /** Не удаляем файлы свежее этого возраста — защита от race при загрузке. */
  private val RaceGuard: Duration = Duration.ofHours(1)

  /** Срок хранения устаревшей аватарки после последнего упоминания в user_log. */
  private val Retention: Duration = Duration.ofDays(3 * 365L)

  @Scheduled(cron = "0 30 4 * * *")
  def cleanOldUserpics(): Unit =
    val dir = photosDir

    if !Files.isDirectory(dir) then
      logger.warn(s"Photos directory does not exist: $dir")
      return

    val active = userDao.getAllActivePhotos
    val deleteMode = siteConfig.cleanOldUserpics
    logger.info(s"OldUserpicCleaner start; active photos: ${active.size}; delete mode: $deleteMode")

    val now = OffsetDateTime.now
    val oneHourAgoInstant = now.minus(RaceGuard).toInstant
    val threeYearsAgo = now.minus(Retention)

    var processed = 0
    var candidates = 0
    var deleted = 0

    val stream = Files.newDirectoryStream(dir)
    try
      val candidatesByBatch = stream
        .iterator
        .asScala
        .filter(Files.isRegularFile(_))
        .map { path =>
          processed += 1;
          path
        }
        .filter { p =>
          val name = String.valueOf(p.getFileName)
          val isAvatar = AvatarNamePattern.matches(name)
          if !isAvatar then
            logger.warn(s"Unexpected file in photos directory: $name")
          isAvatar
        }
        .filter(p => isOlderThan(p, oneHourAgoInstant))
        .filter(p => !active.contains(String.valueOf(p.getFileName)))
        .grouped(BatchSize)

      candidatesByBatch.foreach { batch =>
        val names = batch.map(p => String.valueOf(p.getFileName)).toVector
        val mentions = userLogDao.getLatestUserpicMentions(names)
        batch.foreach { path =>
          val name = String.valueOf(path.getFileName)
          mentions.get(name).flatten match
            case None =>
              // не упомянут в user_log — загружен до 2013 года
              candidates += 1
              if deleteMode then
                deleteFile(path, name, "no user_log entry (pre-2013)", () => deleted += 1)
              else
                logger.info(s"Would delete userpic $name (no user_log entry (pre-2013))")
            case Some(date) if date.isBefore(threeYearsAgo) =>
              candidates += 1
              val reason = s"last mentioned at $date"
              if deleteMode then
                deleteFile(path, name, reason, () => deleted += 1)
              else
                logger.info(s"Would delete userpic $name ($reason)")
            case Some(date) =>
            // упоминание свежее 3 лет — не трогаем
        }
      }
    finally
      stream.close()

    logger.info(s"OldUserpicCleaner done; processed: $processed; candidates: $candidates; deleted: $deleted")

  /** Проверяет, что mtime файла старее указанного момента. Ошибки чтения атрибутов трактуем как «файл свежий»
    * (пропускаем), чтобы не удалить что-то вслепую.
    */
  private def isOlderThan(path: Path, threshold: java.time.Instant): Boolean =
    try
      Files.getLastModifiedTime(path).toInstant.isBefore(threshold)
    catch
      case e: java.io.IOException =>
        logger.warn(s"Cannot read mtime for ${path.getFileName}: ${e.getMessage}")
        false

  private def deleteFile(path: Path, name: String, reason: String, onDeleted: () => Unit): Unit =
    try
      if Files.deleteIfExists(path) then
        onDeleted.apply()
        logger.info(s"Deleted userpic $name ($reason)")
      else
        logger.info(s"Userpic $name already removed ($reason)")
    catch
      case e: java.io.IOException =>
        logger.warn(s"Failed to delete userpic $name: ${e.getMessage}")
