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

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import ru.org.linux.markup.MarkupType
import ru.org.linux.tracker.TrackerFilterEnum

import scala.jdk.CollectionConverters.*

object DefaultProfile:
  val StyleProperty = "style"
  val FormatModeProperty = "format.mode"
  val MessagesProperty = "messages"
  val TopicsProperty = "topics"
  val HideAdsenseProperty = "hideAdsense"
  val PhotosProperty = "photos"
  val MainGalleryProperty = "mainGallery"
  val AvatarProperty = "avatar"
  val BoxesMain2Property = "main2"
  val TrackerMode = "trackerMode"
  val OldTracker = "oldTracker"
  val ReactionNotificationProperty = "reactionNotification"

  val TopicsValues: Set[Int] = Set(30, 50, 100, 200, 300, 500)
  val CommentsValues: Set[Int] = Set(25, 50, 100, 200, 300, 500)

  val DefaultTrackerMode: TrackerFilterEnum = TrackerFilterEnum.MAIN

  val defaultProfile: ImmutableMap[String, Object] =
    val builder = ImmutableMap.builder[String, Object]()

    builder.put(StyleProperty, "tango-auto")
    builder.put(FormatModeProperty, MarkupType.Markdown.formId)
    builder.put(TopicsProperty, Int.box(30))
    builder.put(MessagesProperty, Int.box(50))
    builder.put(PhotosProperty, java.lang.Boolean.TRUE)
    builder.put(AvatarProperty, "empty")
    builder.put(HideAdsenseProperty, java.lang.Boolean.TRUE)
    builder.put(MainGalleryProperty, java.lang.Boolean.FALSE)
    builder.put(TrackerMode, DefaultTrackerMode.getValue)
    builder.put(OldTracker, java.lang.Boolean.FALSE)
    builder.put(ReactionNotificationProperty, java.lang.Boolean.TRUE)

    builder.put("DebugMode", java.lang.Boolean.FALSE)

    builder.put(BoxesMain2Property, ImmutableList.of("poll", "articles", "top10", "gallery", "tagcloud"))

    builder.build()

  private val BoxLegend: ImmutableMap[String, String] =
    ImmutableMap
      .builder[String, String]()
      .put("poll", "Текущий опрос")
      .put("articles", "Новые статьи")
      .put("top10", "Наиболее обсуждаемые темы этого месяца")
      .put("gallery", "Галерея")
      .put("tagcloud", "Облако тэгов")
      .build()

  private val AvatarTypes = Seq("empty", "identicon", "monsterid", "wavatar", "retro", "robohash")
  private val Themes: Map[String, Theme] = Theme.THEMES.asScala.view.map(t => t.getId -> t).toMap

  def getAllBoxes: ImmutableMap[String, String] = BoxLegend

  def isBox(name: String): Boolean = BoxLegend.containsKey(name)

  def isStyle(style: String): Boolean = Themes.contains(style)

  def getAvatars: Seq[String] = AvatarTypes

  def getTheme(id: String): Theme = Themes.getOrElse(id, getDefaultTheme)

  def getDefaultTheme: Theme = Theme.THEMES.getFirst
