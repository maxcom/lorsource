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
package ru.org.linux.site

import jakarta.servlet.ServletContext
import munit.FunSuite
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.{mock, never, times, verify, when}

import java.net.MalformedURLException
import java.nio.file.{Files, Path}
import java.nio.file.attribute.FileTime
import java.time.{Duration, Instant, ZoneOffset}
import java.time.format.DateTimeFormatter

/** [[AssetVersion]] на моке ServletContext: файлы лежат во временном каталоге, время задаёт тест. */
class AssetVersionTest extends FunSuite:
  private val StampFormat = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)

  private val Earlier = Instant.parse("2026-11-26T18:30:47Z")
  private val Later = Instant.parse("2026-11-26T18:31:03Z")

  private var servletContext: ServletContext = scala.compiletime.uninitialized
  // Время, передаваемое в get: перед каждым тестом уходит дальше интервала перепроверки, так что первый вызов в
  // тесте всегда перечитывает файлы
  private var now: Long = 0L

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    servletContext = mock(classOf[ServletContext])
    now += Duration.ofSeconds(10).toNanos

  /** Создаёт файл с заданным временем изменения и говорит моку, что он и есть `path` в webapp */
  private def fileWithTime(path: String, modified: Instant): Path =
    val file = Files.createTempFile("asset-version-test", null)
    Files.setLastModifiedTime(file, FileTime.from(modified))
    when(servletContext.getRealPath(path)).thenReturn(file.toString)
    file

  private def stamp(modified: Instant): String = StampFormat.format(modified)

  test("штамп — время изменения самого свежего из файлов, UTC с секундами"):
    fileWithTime("/js/lor.js", Earlier)
    fileWithTime("/js/plugins.js", Later)

    assertEquals(AssetVersion.get(servletContext, now), "20261126-183103")

  test("webapp развёрнут каталогом: время берётся по реальным путям, URL ресурсов не нужны"):
    AssetVersion.Files.foreach(fileWithTime(_, Later))
    AssetVersion.get(servletContext, now)

    verify(servletContext, never()).getResource(anyString())

  test("нет ни одного файла — штамп остаётся от эпохи"):
    assertEquals(AssetVersion.get(servletContext, now), stamp(Instant.EPOCH))

  test("отсутствующий файл не влияет на штамп остальных"):
    fileWithTime("/js/realtime.js", Earlier)
    fileWithTime("/js/highlight.min.js", Later)

    assertEquals(AssetVersion.get(servletContext, now), stamp(Later))

  test("в пределах секунды файлы не перечитываются, по истечении — штамп новый"):
    val file = fileWithTime("/js/plugins.js", Earlier)
    assertEquals(AssetVersion.get(servletContext, now), stamp(Earlier))

    Files.setLastModifiedTime(file, FileTime.from(Later))
    assertEquals(AssetVersion.get(servletContext, now + Duration.ofMillis(500).toNanos), stamp(Earlier))
    verify(servletContext, times(1)).getRealPath("/js/plugins.js")

    assertEquals(AssetVersion.get(servletContext, now + Duration.ofSeconds(1).toNanos), stamp(Later))
    verify(servletContext, times(2)).getRealPath("/js/plugins.js")

  test("webapp из WAR-файла: время файла берётся из getResource"):
    val file = Files.createTempFile("asset-version-test", null)
    Files.setLastModifiedTime(file, FileTime.from(Later))
    when(servletContext.getResource("/js/lor.js")).thenReturn(file.toUri.toURL)

    assertEquals(AssetVersion.get(servletContext, now), stamp(Later))

  test("файл, недоступный через URL, не мешает остальным"):
    fileWithTime("/js/plugins.js", Later)
    when(servletContext.getResource("/js/lor.js")).thenThrow(new MalformedURLException("lor.js"))

    assertEquals(AssetVersion.get(servletContext, now), stamp(Later))
