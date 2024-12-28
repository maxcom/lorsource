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

package ru.org.linux.user;

import ru.org.linux.markup.MarkupType;
import ru.org.linux.site.DefaultProfile;
import ru.org.linux.tracker.TrackerFilterEnum;
import ru.org.linux.util.ProfileHashtable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.org.linux.site.DefaultProfile.*;

public class ProfileBuilder {
  private String style;
  private MarkupType formatMode;
  private int messages;
  private int topics;
  private boolean showPhotos;
  private boolean hideAdsense;
  private boolean showGalleryOnMain;
  private String avatarMode;
  private boolean oldTracker;
  private TrackerFilterEnum trackerMode;
  private boolean reactionNotification;

  private List<String> boxes;

  public ProfileBuilder(Profile profile) {
    style = profile.style();
    formatMode = profile.formatMode();
    messages = profile.messages();
    topics = profile.topics();
    showPhotos = profile.showPhotos();
    hideAdsense = profile.hideAdsense();
    showGalleryOnMain = profile.showGalleryOnMain();
    avatarMode = profile.avatarMode();
    trackerMode = profile.trackerMode();
    oldTracker = profile.oldTracker();
    reactionNotification = profile.reactionNotification();

    this.boxes = profile.getBoxlets();
  }

  public Map<String, String> getSettings() {
    ProfileHashtable p = new ProfileHashtable(DefaultProfile.getDefaultProfile(), new HashMap<>());

    p.setString(STYLE_PROPERTY, style);
    p.setString(FORMAT_MODE_PROPERTY, formatMode.formId());
    p.setInt(MESSAGES_PROPERTY, messages);
    p.setInt(TOPICS_PROPERTY, topics);
    p.setBoolean(PHOTOS_PROPERTY, showPhotos);
    p.setBoolean(HIDE_ADSENSE_PROPERTY, hideAdsense);
    p.setBoolean(MAIN_GALLERY_PROPERTY, showGalleryOnMain);
    p.setString(AVATAR_PROPERTY, avatarMode);
    p.setString(TRACKER_MODE, trackerMode.getValue());
    p.setBoolean(OLD_TRACKER, oldTracker);
    p.setBoolean(REACTION_NOTIFICATION_PROPERTY, reactionNotification);

    return p.getSettings();
  }

  public void setStyle(String style) {
    this.style = Profile.fixStyle(style);
  }

  public void setFormatMode(String formatMode) {
    this.formatMode = Profile.fixFormat(formatMode);
  }

  public void setMessages(int messages) {
    this.messages = messages;
  }

  public void setTopics(int topics) {
    this.topics = topics;
  }

  public void setShowPhotos(boolean showPhotos) {
    this.showPhotos = showPhotos;
  }

  public void setHideAdsense(boolean hideAdsense) {
    this.hideAdsense = hideAdsense;
  }

  public void setShowGalleryOnMain(boolean showGalleryOnMain) {
    this.showGalleryOnMain = showGalleryOnMain;
  }

  public void setAvatarMode(String avatarMode) {
    this.avatarMode = avatarMode;
  }

  public void setOldTracker(boolean oldTracker) {
    this.oldTracker = oldTracker;
  }

  public void setTrackerMode(TrackerFilterEnum trackerMode) {
    this.trackerMode = trackerMode;
  }

  @Nullable
  public List<String> getCustomBoxlets() {
    return boxes;
  }

  public void setBoxlets(List<String> list) {
    boxes = new ArrayList<>(list);
  }

  public void setReactionNotification(boolean reactionNotification) {
    this.reactionNotification = reactionNotification;
  }
}
