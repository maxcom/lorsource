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
import scala.jdk.CollectionConverters.*

import java.util as ju

class ProfileBuilder(profile: Profile):
  private var style: String = profile.style
  private var formatMode: MarkupType = profile.formatMode
  private var messages: Int = profile.messages
  private var topics: Int = profile.topics
  private var showPhotos: Boolean = profile.showPhotos
  private var hideAdsense: Boolean = profile.hideAdsense
  private var showGalleryOnMain: Boolean = profile.showGalleryOnMain
  private var avatarMode: String = profile.avatarMode
  private var oldTracker: Boolean = profile.oldTracker
  private var trackerMode: TrackerFilterEnum = profile.trackerMode
  private var reactionNotification: Boolean = profile.reactionNotification

  private var boxes: Seq[String] = profile.boxes

  def getSettings: ju.Map[String, String] =
    val p = new ProfileHashtable(DefaultProfile.defaultProfile, new ju.HashMap[String, String]())

    p.setString(DefaultProfile.StyleProperty, style)
    p.setString(DefaultProfile.FormatModeProperty, formatMode.formId)
    p.setInt(DefaultProfile.MessagesProperty, messages)
    p.setInt(DefaultProfile.TopicsProperty, topics)
    p.setBoolean(DefaultProfile.PhotosProperty, showPhotos)
    p.setBoolean(DefaultProfile.HideAdsenseProperty, hideAdsense)
    p.setBoolean(DefaultProfile.MainGalleryProperty, showGalleryOnMain)
    p.setString(DefaultProfile.AvatarProperty, avatarMode)
    p.setString(DefaultProfile.TrackerMode, trackerMode.getValue)
    p.setBoolean(DefaultProfile.OldTracker, oldTracker)
    p.setBoolean(DefaultProfile.ReactionNotificationProperty, reactionNotification)

    p.getSettings

  def setStyle(style: String): Unit = this.style = Profile.fixStyle(style)

  def setFormatMode(formatMode: String): Unit = this.formatMode = Profile.fixFormat(formatMode)

  def setMessages(messages: Int): Unit = this.messages = messages

  def setTopics(topics: Int): Unit = this.topics = topics

  def setShowPhotos(showPhotos: Boolean): Unit = this.showPhotos = showPhotos

  def setHideAdsense(hideAdsense: Boolean): Unit = this.hideAdsense = hideAdsense

  def setShowGalleryOnMain(showGalleryOnMain: Boolean): Unit = this.showGalleryOnMain = showGalleryOnMain

  def setAvatarMode(avatarMode: String): Unit = this.avatarMode = avatarMode

  def setOldTracker(oldTracker: Boolean): Unit = this.oldTracker = oldTracker

  def setTrackerMode(trackerMode: TrackerFilterEnum): Unit = this.trackerMode = trackerMode

  def getCustomBoxlets: ju.List[String] = boxes.asJava

  def setBoxlets(list: collection.Seq[String]): Unit = boxes = list.toVector

  def setReactionNotification(reactionNotification: Boolean): Unit = this.reactionNotification = reactionNotification
