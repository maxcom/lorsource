package ru.org.linux.topic

import java.time.{ZoneId, ZonedDateTime}

class TopicListToolsTest extends munit.FunSuite {
  test("partitionOf should return correct partitions for Europe/Moscow") {
    val timezone = ZoneId.of("Europe/Moscow")
    val now = ZonedDateTime.of(2023, 10, 15, 12, 0, 0, 0, timezone).toInstant

    // Today
    val today = ZonedDateTime.of(2023, 10, 15, 8, 0, 0, 0, timezone).toInstant
    assertEquals(TopicListTools.partitionOf(today, timezone, now), "Сегодня")

    // Yesterday
    val yesterday = ZonedDateTime.of(2023, 10, 14, 23, 59, 0, 0, timezone).toInstant
    assertEquals(TopicListTools.partitionOf(yesterday, timezone, now), "Вчера")

    // Within a year
    val lastMonth = ZonedDateTime.of(2023, 9, 15, 12, 0, 0, 0, timezone).toInstant
    assertEquals(TopicListTools.partitionOf(lastMonth, timezone, now), "Сентябрь 2023")

    // More than a year ago
    val yearAgo = ZonedDateTime.of(2022, 9, 15, 12, 0, 0, 0, timezone).toInstant
    assertEquals(TopicListTools.partitionOf(yearAgo, timezone, now), "2022")
  }
}
