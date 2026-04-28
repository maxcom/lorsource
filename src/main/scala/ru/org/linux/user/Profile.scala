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
package ru.org.linux.user

import ru.org.linux.markup.MarkupType
import ru.org.linux.site.DefaultProfile
import ru.org.linux.tracker.TrackerFilterEnum
import ru.org.linux.util.ProfileHashtable

import java.util
import scala.beans.{BeanProperty, BooleanBeanProperty}
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import DefaultProfile.*

object Profile {
  val DEFAULT: Profile = apply(new ProfileHashtable(DefaultProfile.defaultProfile, util.Map.of), null)

  def fixFormat(mode: String): MarkupType = if (MarkupType.AllFormIds.contains(mode)) {
    MarkupType.ofFormId(mode)
  } else {
    MarkupType.ofFormId(DefaultProfile.defaultProfile.get("format.mode").asInstanceOf[String])
  }

  def fixStyle(style: String): String = {
    if (!DefaultProfile.isStyle(style)) {
      DefaultProfile.defaultProfile.get(StyleProperty).asInstanceOf[String]
    } else {
      style
    }
  }

  def apply(p: ProfileHashtable, boxes: util.List[String]): Profile = {
    new Profile(
      style = Profile.fixStyle(p.getString(StyleProperty)),
      formatMode = Profile.fixFormat(p.getString(FormatModeProperty)),
      messages = p.getInt(MessagesProperty),
      topics = p.getInt(TopicsProperty),
      showPhotos = p.getBoolean(PhotosProperty),
      hideAdsense = p.getBoolean(HideAdsenseProperty),
      showGalleryOnMain = p.getBoolean(MainGalleryProperty),
      avatarMode = p.getString(AvatarProperty),
      trackerMode = TrackerFilterEnum.getByValue(p.getString(TrackerMode)).filter(_.isCanBeDefault).orElse(DefaultProfile.DefaultTrackerMode),
      oldTracker = p.getBoolean(OldTracker),
      reactionNotification = p.getBoolean(ReactionNotificationProperty),
      boxes = (if (boxes != null) {
        boxes
      } else {
        DefaultProfile.defaultProfile.get(BoxesMain2Property).asInstanceOf[java.util.List[String]]
      }).asScala.toVector)
  }
}

case class Profile(style: String, formatMode: MarkupType, @BeanProperty messages: Int, @BeanProperty topics: Int,
                   @BooleanBeanProperty showPhotos: Boolean, @BooleanBeanProperty hideAdsense: Boolean,
                   @BooleanBeanProperty showGalleryOnMain: Boolean, @BeanProperty avatarMode: String,
                   @BooleanBeanProperty oldTracker: Boolean, @BeanProperty trackerMode: TrackerFilterEnum,
                   @BooleanBeanProperty reactionNotification: Boolean, boxes: Seq[String]) {
  // java API
  def getBoxlets: util.List[String] = boxes.asJava
}