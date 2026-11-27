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
package ru.org.linux.search

import com.typesafe.scalalogging.StrictLogging
import org.springframework.core.NestedExceptionUtils
import org.springframework.stereotype.Component
import org.springframework.util.ErrorHandler

import java.time.Duration
import scala.collection.mutable

/** Обработчик ошибок потребителя очереди индексации (jmsContainer): подавляет одинаковые повторяющиеся сбои в логе и
  * задаёт паузу между повторами. Полный stack trace пишется при первой ошибке сигнатуры и далее не чаще раза в пять
  * минут; следующая полная запись указывает число подавленных повторов.
  */
@Component
class SearchQueueErrorHandler extends ErrorHandler with StrictLogging:
  import SearchQueueErrorHandler.*

  private val suppression = Suppressor(LogInterval.toMillis, () => System.currentTimeMillis())

  override def handleError(t: Throwable): Unit =
    suppression.decide(signature(t)) match
      case LogDecision.LogFull(suppressed) =>
        if suppressed == 0 then
          logger.error("Search queue listener failed", t)
        else
          logger.error(s"Search queue listener failed (suppressed repeats: $suppressed)", t)
      case LogDecision.Suppress =>
        ()

    pauseBeforeRetry()

  /** Пауза перед повтором: без неё недоступный OpenSearch даёт около четырёх бесполезных повторов в секунду.
    * Прерываемая, чтобы не задерживать остановку контейнера.
    */
  private def pauseBeforeRetry(): Unit =
    try
      Thread.sleep(RetryPause.toMillis)
    catch
      case _: InterruptedException =>
        Thread.currentThread().interrupt()

object SearchQueueErrorHandler:
  /** Полный stack trace одной и той же ошибки — не чаще этого интервала. */
  val LogInterval: Duration = Duration.ofMinutes(5)

  /** Пауза перед повторной обработкой неудачного сообщения. */
  val RetryPause: Duration = Duration.ofSeconds(5)

  /** Сигнатура ошибки: класс исключения и его наиболее конкретная причина — повторы одного сбоя подавляются, разные
    * сбои логируются независимо.
    */
  def signature(t: Throwable): String =
    val mostSpecific = NestedExceptionUtils.getMostSpecificCause(t)
    s"${t.getClass.getName} <- ${mostSpecific.getClass.getName}"

  enum LogDecision:
    case LogFull(suppressed: Long)
    case Suppress

  /** Состояние подавления по сигнатурам, синхронизировано на Map: потребителей у контейнера четыре. Вытеснения нет —
    * число сигнатур ограничено набором типов исключений.
    *
    * @param intervalMillis
    *   интервал между полными записями одной сигнатуры
    * @param clock
    *   время в миллисекундах; подставляется в тесте
    */
  final class Suppressor(intervalMillis: Long, clock: () => Long):
    private case class Entry(lastLoggedAt: Long, suppressed: Long)

    private val entries = mutable.Map.empty[String, Entry]

    def decide(sig: String): LogDecision =
      entries.synchronized:
        val now = clock()

        entries.get(sig) match
          case None =>
            entries.update(sig, Entry(now, 0))
            LogDecision.LogFull(0)
          case Some(entry) if now - entry.lastLoggedAt >= intervalMillis =>
            entries.update(sig, Entry(now, 0))
            LogDecision.LogFull(entry.suppressed)
          case Some(entry) =>
            // окно не сдвигается: при непрерывном сбое полная запись повторяется раз в интервал, а не один раз
            entries.update(sig, Entry(entry.lastLoggedAt, entry.suppressed + 1))
            LogDecision.Suppress
