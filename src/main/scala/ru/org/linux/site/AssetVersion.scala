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

import java.io.{File, IOException}
import java.time.{Duration, Instant, ZoneOffset}
import java.time.format.DateTimeFormatter

/** Штамп версии статических файлов: время изменения самого свежего из [[AssetVersion.Files]].
  *
  * JSP подставляет его в URL (`/js/lor.js?<штамп>`, `WEB-INF/tags/assetVersion.tag`). Сборка пересоздаёт эти файлы, и
  * штамп меняется вместе с ними — браузер (`Cache-Control: max-age=315360000` из `urlrewrite.xml`) забирает новые файлы
  * вместо закэшированных. Штамп читается из развёрнутого webapp, поэтому пересборка JS и CSS (`mvn compile`) не требует
  * перезапуска сервера.
  */
object AssetVersion:
  /** Файлы, URL которых в JSP несут штамп; новый такой файл добавляется сюда */
  val Files: Seq[String] =
    Seq(
      "/js/lor.js",
      "/js/plugins.js",
      "/js/realtime.js",
      "/js/markup-editor.js",
      "/js/add-form.js",
      "/js/highlight.min.js",
      "/tango/combined.css",
      "/waltz/combined.css",
      "/black/combined.css",
      "/white2/combined.css",
      "/zomg_ponies/combined.css"
    )

  private val StampFormat = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)
  private val RecheckInterval = Duration.ofSeconds(1).toNanos

  private final case class Checked(at: Long, value: String)

  @volatile
  private var lastCheck: Option[Checked] = None

  def get(context: ServletContext): String = get(context, System.nanoTime())

  // nowNanos передаётся, чтобы тест управлял временем. Блокировки нет: поток с просроченной проверкой делает свою,
  // остальные продолжают читать прежний штамп.
  private[site] def get(context: ServletContext, nowNanos: Long): String =
    lastCheck match
      case Some(checked) if nowNanos - checked.at < RecheckInterval =>
        checked.value
      case _ =>
        val checked = Checked(nowNanos, format(newestModified(context)))
        lastCheck = Some(checked)
        checked.value

  /** Файлов из Files может не быть — они не учитываются */
  private def newestModified(context: ServletContext): Long = Files.map(path => lastModified(context, path)).max

  private def lastModified(context: ServletContext, path: String): Long =
    val realPath = context.getRealPath(path)

    if realPath != null then
      File(realPath).lastModified()
    else
      urlLastModified(context, path)

  /** Webapp запущен из WAR-файла, а не из каталога */
  private def urlLastModified(context: ServletContext, path: String): Long =
    try
      val url = context.getResource(path)
      if url == null then
        0L
      else
        url.openConnection().getLastModified
    catch
      case _: IOException =>
        0L

  private def format(millis: Long): String = StampFormat.format(Instant.ofEpochMilli(millis))
