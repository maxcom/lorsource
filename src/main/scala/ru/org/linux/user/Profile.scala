/*
 * Copyright 1998-2024 Linux.org.ru
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
  val DEFAULT: Profile = apply(new ProfileHashtable(DefaultProfile.getDefaultProfile, util.Map.of), null)

  def fixFormat(mode: String): String = if (MarkupType.AllFormIds.contains(mode)) {
    mode
  } else {
    DefaultProfile.getDefaultProfile.get("format.mode").asInstanceOf[String]
  }

  def fixStyle(style: String): String = {
    if (!DefaultProfile.isStyle(style)) {
      DefaultProfile.getDefaultProfile.get(STYLE_PROPERTY).asInstanceOf[String]
    } else {
      style
    }
  }

  def apply(p: ProfileHashtable, boxes: util.List[String]): Profile = {
    new Profile(
      style = Profile.fixStyle(p.getString(STYLE_PROPERTY)),
      formatMode = Profile.fixFormat(p.getString(FORMAT_MODE_PROPERTY)),
      messages = p.getInt(MESSAGES_PROPERTY),
      topics = p.getInt(TOPICS_PROPERTY),
      showPhotos = p.getBoolean(PHOTOS_PROPERTY),
      hideAdsense = p.getBoolean(HIDE_ADSENSE_PROPERTY),
      showGalleryOnMain = p.getBoolean(MAIN_GALLERY_PROPERTY),
      avatarMode = p.getString(AVATAR_PROPERTY),
      trackerMode = TrackerFilterEnum.getByValue(p.getString(TRACKER_MODE)).filter(_.isCanBeDefault).orElse(DefaultProfile.DEFAULT_TRACKER_MODE),
      oldTracker = p.getBoolean(OLD_TRACKER),
      reactionNotification = p.getBoolean(REACTION_NOTIFICATION_PROPERTY),
      boxes = (if (boxes != null) {
        boxes
      } else {
        DefaultProfile.getDefaultProfile.get(BOXES_MAIN2_PROPERTY).asInstanceOf[java.util.List[String]]
      }).asScala.toVector)
  }
}

case class Profile(style: String, formatMode: String, @BeanProperty messages: Int, @BeanProperty topics: Int,
                   @BooleanBeanProperty showPhotos: Boolean, @BooleanBeanProperty hideAdsense: Boolean,
                   @BooleanBeanProperty showGalleryOnMain: Boolean, @BeanProperty avatarMode: String,
                   @BooleanBeanProperty oldTracker: Boolean, @BeanProperty trackerMode: TrackerFilterEnum,
                   @BooleanBeanProperty reactionNotification: Boolean, boxes: Seq[String]) {
  def hasMiniNewsBoxlet: Boolean = getBoxlets.contains("lastMiniNews")

  // java API
  def getBoxlets: util.List[String] = boxes.asJava
}