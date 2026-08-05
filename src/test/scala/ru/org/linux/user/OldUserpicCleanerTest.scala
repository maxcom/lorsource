/*
 * Copyright 1998-2026 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.org.linux.user

import org.junit.Assert.{assertFalse, assertTrue}
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, never, verify, when}
import ru.org.linux.spring.SiteConfig

import java.nio.file.{Files, Path}
import java.nio.file.attribute.FileTime
import java.time.{OffsetDateTime, ZoneOffset}

/** Юнит-тесты для [[OldUserpicCleaner]] на моках DAO и SiteConfig. Временные каталоги создаются через
  * `Files.createTempDirectory`, т.к. JUnit4 `@Rule TemporaryFolder` требует public-поле, что в Scala 3 (без
  * `@JvmField`) невозможно.
  */
class OldUserpicCleanerTest:
  private val siteConfig = mock(classOf[SiteConfig])
  private val userDao = mock(classOf[UserDao])
  private val userLogDao = mock(classOf[UserLogDao])
  private val cleaner = OldUserpicCleaner(siteConfig, userDao, userLogDao)

  private val Now = OffsetDateTime.now(ZoneOffset.UTC)
  private val HoursAgo = Now.minusHours(2)
  private val FiveYearsAgo = Now.minusYears(5)
  private val OneYearAgo = Now.minusYears(1)

  /** Создаёт временный каталог `photos` и возвращает его путь. */
  private def newPhotosDir(): Path =
    val tmp = Files.createTempDirectory("old-userpic-cleaner-test")
    Files.createDirectories(tmp.resolve("photos"))
    tmp.resolve("photos")

  private def touch(dir: Path, name: String, mtime: OffsetDateTime): Path =
    val p = dir.resolve(name)
    Files.createFile(p)
    Files.setLastModifiedTime(p, FileTime.from(mtime.toInstant))
    p

  @Test
  def deleteOffDoesNotDelete(): Unit =
    val dir = newPhotosDir()
    val file = touch(dir, "123:456.jpg", HoursAgo)

    when(siteConfig.getUploadPath).thenReturn(dir.getParent.toString)
    when(siteConfig.cleanOldUserpics).thenReturn(false)
    when(userDao.getAllActivePhotos).thenReturn(Set.empty[String])
    when(userLogDao.getLatestUserpicMentions(any(classOf[Seq[String]]))).thenReturn(
      Map("123:456.jpg" -> Some(FiveYearsAgo)))

    cleaner.cleanOldUserpics()

    assertTrue(Files.exists(file))
    verify(userLogDao).getLatestUserpicMentions(any(classOf[Seq[String]]))

  @Test
  def deleteOnRemovesOldFile(): Unit =
    val dir = newPhotosDir()
    val file = touch(dir, "123:456.jpg", HoursAgo)

    when(siteConfig.getUploadPath).thenReturn(dir.getParent.toString)
    when(siteConfig.cleanOldUserpics).thenReturn(true)
    when(userDao.getAllActivePhotos).thenReturn(Set.empty[String])
    when(userLogDao.getLatestUserpicMentions(any(classOf[Seq[String]]))).thenReturn(
      Map("123:456.jpg" -> Some(FiveYearsAgo)))

    cleaner.cleanOldUserpics()

    assertFalse(Files.exists(file))

  @Test
  def noLogEntryMeansPre2013Deleted(): Unit =
    val dir = newPhotosDir()
    val file = touch(dir, "42.jpg", HoursAgo)

    when(siteConfig.getUploadPath).thenReturn(dir.getParent.toString)
    when(siteConfig.cleanOldUserpics).thenReturn(true)
    when(userDao.getAllActivePhotos).thenReturn(Set.empty[String])
    when(userLogDao.getLatestUserpicMentions(any(classOf[Seq[String]]))).thenReturn(
      Map.empty[String, Option[OffsetDateTime]])

    cleaner.cleanOldUserpics()

    assertFalse(Files.exists(file))

  @Test
  def recentLogEntryIsKept(): Unit =
    val dir = newPhotosDir()
    val file = touch(dir, "123:456.jpg", HoursAgo)

    when(siteConfig.getUploadPath).thenReturn(dir.getParent.toString)
    when(siteConfig.cleanOldUserpics).thenReturn(true)
    when(userDao.getAllActivePhotos).thenReturn(Set.empty[String])
    when(userLogDao.getLatestUserpicMentions(any(classOf[Seq[String]]))).thenReturn(
      Map("123:456.jpg" -> Some(OneYearAgo)))

    cleaner.cleanOldUserpics()

    assertTrue(Files.exists(file))

  @Test
  def activeFileIsKept(): Unit =
    val dir = newPhotosDir()
    val file = touch(dir, "123:456.jpg", HoursAgo)

    when(siteConfig.getUploadPath).thenReturn(dir.getParent.toString)
    when(siteConfig.cleanOldUserpics).thenReturn(true)
    when(userDao.getAllActivePhotos).thenReturn(Set("123:456.jpg"))
    // даже если лог говорит «старое» — файл активный, не трогаем
    when(userLogDao.getLatestUserpicMentions(any(classOf[Seq[String]]))).thenReturn(
      Map("123:456.jpg" -> Some(FiveYearsAgo)))

    cleaner.cleanOldUserpics()

    assertTrue(Files.exists(file))
    // активные файлы не попадают в батч, поэтому DAO не должен вызываться
    verify(userLogDao, never()).getLatestUserpicMentions(any(classOf[Seq[String]]))

  @Test
  def freshUploadIsKept(): Unit =
    val dir = newPhotosDir()
    // файл создан «только что», mtime сейчас
    val file = touch(dir, "123:456.jpg", Now)

    when(siteConfig.getUploadPath).thenReturn(dir.getParent.toString)
    when(siteConfig.cleanOldUserpics).thenReturn(true)
    when(userDao.getAllActivePhotos).thenReturn(Set.empty[String])
    when(userLogDao.getLatestUserpicMentions(any(classOf[Seq[String]]))).thenReturn(
      Map("123:456.jpg" -> Some(FiveYearsAgo)))

    cleaner.cleanOldUserpics()

    assertTrue(Files.exists(file))

  @Test
  def unexpectedFileNameIsKept(): Unit =
    val dir = newPhotosDir()
    val file = touch(dir, "garbage.txt", HoursAgo)

    when(siteConfig.getUploadPath).thenReturn(dir.getParent.toString)
    when(siteConfig.cleanOldUserpics).thenReturn(true)
    when(userDao.getAllActivePhotos).thenReturn(Set.empty[String])
    when(userLogDao.getLatestUserpicMentions(any(classOf[Seq[String]]))).thenReturn(
      Map.empty[String, Option[OffsetDateTime]])

    cleaner.cleanOldUserpics()

    assertTrue(Files.exists(file))
    verify(userLogDao, never()).getLatestUserpicMentions(any(classOf[Seq[String]]))

  @Test
  def missingDirectoryIsIgnored(): Unit =
    when(siteConfig.getUploadPath).thenReturn("/nonexistent-path-for-OldUserpicCleanerTest")
    when(siteConfig.cleanOldUserpics).thenReturn(true)
    // не должно быть исключений и не должно быть обращений к DAO
    cleaner.cleanOldUserpics()
    verify(userDao, never()).getAllActivePhotos
