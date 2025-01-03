package ru.org.linux.common

object DeleteReasons {
  val DeleteReasons: Seq[String] = Seq(
    "3.1 Дубль",
    "3.2 Неверная кодировка",
    "3.3 Некорректное форматирование",
    "3.4 Пустое сообщение",
    "4.1 Offtopic",
    "4.2 Вызывающе неверная информация",
    "4.3 Провокация flame",
    "4.4 Обсуждение действий модераторов",
    "4.5 Тестовые сообщения",
    "4.6 Спам",
    "4.7 Флуд",
    "4.8 Дискуссия не на русском языке",
    "5.1 Нецензурные выражения",
    "5.2 Оскорбление участников дискуссии",
    "5.3 Национальные/политические/религиозные споры",
    "5.4 Личная переписка",
    "5.5 Преднамеренное нарушение правил русского языка",
    "6 Нарушение copyright",
    "6.2 Warez",
    "7.1 Ответ на некорректное сообщение")

  def replyBonusAndReason(dropScore: Boolean, depth: Int): (Int, String) = {
    if (dropScore) {
      depth match {
        case 0 =>
          (-2, "7.1 Ответ на некорректное сообщение (авто, уровень 0)")
        case 1 =>
          (-1, "7.1 Ответ на некорректное сообщение (авто, уровень 1)")
        case _ =>
          (0, "7.1 Ответ на некорректное сообщение (авто, уровень >1)")
      }
    } else {
      (0, "7.1 Ответ на некорректное сообщение (авто)")
    }
  }
}
