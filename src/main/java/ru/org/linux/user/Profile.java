/*
 * Copyright 1998-2022 Linux.org.ru
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

import ru.org.linux.markup.MarkupType$;
import ru.org.linux.site.DefaultProfile;
import ru.org.linux.tracker.TrackerFilterEnum;
import ru.org.linux.util.ProfileHashtable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Profile {
  public static final String STYLE_PROPERTY = "style";
  public static final String FORMAT_MODE_PROPERTY = "format.mode";
  public static final String MESSAGES_PROPERTY = "messages";
  public static final String TOPICS_PROPERTY = "topics";
  public static final String HIDE_ADSENSE_PROPERTY = "hideAdsense";
  public static final String PHOTOS_PROPERTY = "photos";
  public static final String MAIN_GALLERY_PROPERTY = "mainGallery";
  public static final String AVATAR_PROPERTY = "avatar";
  public static final String SHOW_ANONYMOUS_PROPERTY = "showanonymous";
  public static final String BOXES_MAIN2_PROPERTY = "main2";
  public static final String TRACKER_MODE = "trackerMode";
  public static final String OLD_TRACKER = "oldTracker";

  private String style;
  private String formatMode;
  private int messages;
  private int topics;
  private boolean showPhotos;
  private boolean hideAdsense;
  private boolean showGalleryOnMain;
  private String avatarMode;
  private boolean showAnonymous;
  private boolean oldTracker;
  private TrackerFilterEnum trackerMode;

  private List<String> boxes;

  public Profile(ProfileHashtable p, List<String> boxes) {
    style = fixStyle(p.getString(STYLE_PROPERTY));
    formatMode = fixFormat(p.getString(FORMAT_MODE_PROPERTY));
    messages = p.getInt(MESSAGES_PROPERTY);
    topics = p.getInt(TOPICS_PROPERTY);
    showPhotos = p.getBoolean(PHOTOS_PROPERTY);
    hideAdsense = p.getBoolean(HIDE_ADSENSE_PROPERTY);
    showGalleryOnMain = p.getBoolean(MAIN_GALLERY_PROPERTY);
    avatarMode = p.getString(AVATAR_PROPERTY);
    showAnonymous = p.getBoolean(SHOW_ANONYMOUS_PROPERTY);

    trackerMode = TrackerFilterEnum.getByValue(p.getString(TRACKER_MODE), false)
            .filter(TrackerFilterEnum::isCanBeDefault)
            .orElse(DefaultProfile.DEFAULT_TRACKER_MODE);

    oldTracker = p.getBoolean(OLD_TRACKER);

    this.boxes = boxes;
  }

  public Map<String, String> getSettings() {
    ProfileHashtable p = new ProfileHashtable(DefaultProfile.getDefaultProfile(), new HashMap<>());

    p.setString(STYLE_PROPERTY, style);
    p.setString(FORMAT_MODE_PROPERTY, formatMode);
    p.setInt(MESSAGES_PROPERTY, messages);
    p.setInt(TOPICS_PROPERTY, topics);
    p.setBoolean(PHOTOS_PROPERTY, showPhotos);
    p.setBoolean(HIDE_ADSENSE_PROPERTY, hideAdsense);
    p.setBoolean(MAIN_GALLERY_PROPERTY, showGalleryOnMain);
    p.setString(AVATAR_PROPERTY, avatarMode);
    p.setBoolean(SHOW_ANONYMOUS_PROPERTY, showAnonymous);
    p.setString(TRACKER_MODE, trackerMode.getValue());
    p.setBoolean(OLD_TRACKER, oldTracker);

    return p.getSettings();
  }

  public String getStyle() {
    return style;
  }

  public void setStyle(String style) {
    this.style = fixStyle(style);
  }

  public String getFormatMode() {
    return formatMode;
  }

  public void setFormatMode(String formatMode) {
    this.formatMode = fixFormat(formatMode);
  }

  public int getMessages() {
    return messages;
  }

  public void setMessages(int messages) {
    this.messages = messages;
  }

  public int getTopics() {
    return topics;
  }

  public void setTopics(int topics) {
    this.topics = topics;
  }

  public boolean isShowPhotos() {
    return showPhotos;
  }

  public void setShowPhotos(boolean showPhotos) {
    this.showPhotos = showPhotos;
  }

  public boolean isHideAdsense() {
    return hideAdsense;
  }

  public void setHideAdsense(boolean hideAdsense) {
    this.hideAdsense = hideAdsense;
  }

  public boolean isShowGalleryOnMain() {
    return showGalleryOnMain;
  }

  public void setShowGalleryOnMain(boolean showGalleryOnMain) {
    this.showGalleryOnMain = showGalleryOnMain;
  }

  public String getAvatarMode() {
    return avatarMode;
  }

  public void setAvatarMode(String avatarMode) {
    this.avatarMode = avatarMode;
  }

  public boolean isShowAnonymous() {
    return showAnonymous;
  }

  public void setShowAnonymous(boolean showAnonymous) {
    this.showAnonymous = showAnonymous;
  }

  public boolean isMiniNewsBoxletOnMainPage() {
    return getBoxlets().contains("lastMiniNews");
  }

  public boolean isOldTracker() {
    return oldTracker;
  }

  public void setOldTracker(boolean oldTracker) {
    this.oldTracker = oldTracker;
  }

  private static String fixFormat(String mode) {
    if (MarkupType$.MODULE$.AllFormIds().contains(mode)) {
      return mode;
    } else {
      return (String) DefaultProfile.getDefaultProfile().get("format.mode");
    }
  }

  private static String fixStyle(String style) {
    if (!DefaultProfile.isStyle(style)) {
      return (String) DefaultProfile.getDefaultProfile().get(STYLE_PROPERTY);
    }

    return style;
  }

  public TrackerFilterEnum getTrackerMode() {
    return trackerMode;
  }

  public void setTrackerMode(TrackerFilterEnum trackerMode) {
    this.trackerMode = trackerMode;
  }

  @Nonnull
  public List<String> getBoxlets() {
    List<String> list = boxes;

    if (list==null) {
      return (List<String>) DefaultProfile.getDefaultProfile().get(BOXES_MAIN2_PROPERTY);
    } else {
      return list;
    }
  }

  @Nullable
  public List<String> getCustomBoxlets() {
    return boxes;
  }

  public void setBoxlets(List<String> list) {
    boxes = new ArrayList<>(list);
  }

  public static Profile createDefault() {
    return new Profile(new ProfileHashtable(DefaultProfile.getDefaultProfile(), new HashMap<>()), null);
  }
}
