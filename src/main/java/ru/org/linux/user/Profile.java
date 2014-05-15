/*
 * Copyright 1998-2014 Linux.org.ru
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
  public static final String HOVER_PROPERTY = "hover";
  public static final String MESSAGES_PROPERTY = "messages";
  public static final String NEWFIRST_PROPERTY = "newfirst";
  public static final String TOPICS_PROPERTY = "topics";
  public static final String HIDE_ADSENSE_PROPERTY = "hideAdsense";
  public static final String PHOTOS_PROPERTY = "photos";
  public static final String MAIN_GALLERY_PROPERTY = "mainGallery";
  public static final String AVATAR_PROPERTY = "avatar";
  public static final String SHOW_ANONYMOUS_PROPERTY = "showanonymous";
  public static final String BOXES_MAIN2_PROPERTY = "main2";
  public static final String SHOW_SOCIAL_PROPERTY = "showSocial";
  public static final String TRACKER_MODE = "trackerMode";

  private String style;
  private String formatMode;
  private boolean useHover;
  private int messages;
  private boolean showNewFirst;
  private int topics;
  private boolean showPhotos;
  private boolean hideAdsense;
  private boolean showGalleryOnMain;
  private String avatarMode;
  private boolean showAnonymous;
  private boolean showSocial;
  private TrackerFilterEnum trackerMode;

  private List<String> boxes;

  public Profile(ProfileHashtable p, List<String> boxes) {
    style = fixStyle(p.getString(STYLE_PROPERTY));
    formatMode = fixFormat(p.getString(FORMAT_MODE_PROPERTY));
    useHover = p.getBoolean(HOVER_PROPERTY);
    messages = p.getInt(MESSAGES_PROPERTY);
    showNewFirst = p.getBoolean(NEWFIRST_PROPERTY);
    topics = p.getInt(TOPICS_PROPERTY);
    showPhotos = p.getBoolean(PHOTOS_PROPERTY);
    hideAdsense = p.getBoolean(HIDE_ADSENSE_PROPERTY);
    showGalleryOnMain = p.getBoolean(MAIN_GALLERY_PROPERTY);
    avatarMode = p.getString(AVATAR_PROPERTY);
    showAnonymous = p.getBoolean(SHOW_ANONYMOUS_PROPERTY);
    showSocial = p.getBoolean(SHOW_SOCIAL_PROPERTY);
    trackerMode = TrackerFilterEnum.getByValue(p.getString(TRACKER_MODE)).or(DefaultProfile.DEFAULT_TRACKER_MODE);

    this.boxes = boxes;
  }

  public Map<String, String> getSettings() {
    ProfileHashtable p = new ProfileHashtable(DefaultProfile.getDefaultProfile(), new HashMap<String, String>());

    p.setString(STYLE_PROPERTY, style);
    p.setString(FORMAT_MODE_PROPERTY, formatMode);
    p.setBoolean(HOVER_PROPERTY, useHover);
    p.setInt(MESSAGES_PROPERTY, messages);
    p.setBoolean(NEWFIRST_PROPERTY, showNewFirst);
    p.setInt(TOPICS_PROPERTY, topics);
    p.setBoolean(PHOTOS_PROPERTY, showPhotos);
    p.setBoolean(HIDE_ADSENSE_PROPERTY, hideAdsense);
    p.setBoolean(MAIN_GALLERY_PROPERTY, showGalleryOnMain);
    p.setString(AVATAR_PROPERTY, avatarMode);
    p.setBoolean(SHOW_ANONYMOUS_PROPERTY, showAnonymous);
    p.setBoolean(SHOW_SOCIAL_PROPERTY, showSocial);
    p.setString(TRACKER_MODE, trackerMode.getValue());

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

  public boolean isUseHover() {
    return useHover;
  }

  public void setUseHover(boolean hover) {
    useHover = hover;
  }

  public int getMessages() {
    return messages;
  }

  public void setMessages(int messages) {
    this.messages = messages;
  }

  public boolean isShowNewFirst() {
    return showNewFirst;
  }

  public void setShowNewFirst(boolean newFirst) {
    showNewFirst = newFirst;
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

  private static String fixFormat(String mode) {
    if (!"quot".equals(mode) &&
        !"ntobr".equals(mode) &&
        !"lorcode".equals(mode)) {
      return (String) DefaultProfile.getDefaultProfile().get("format.mode");
    }

    return mode;
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

  public boolean isShowSocial() {
    return showSocial;
  }

  public void setShowSocial(boolean showSocial) {
    this.showSocial = showSocial;
  }

  public static Profile createDefault() {
    return new Profile(new ProfileHashtable(DefaultProfile.getDefaultProfile(), new HashMap<String, String>()), null);
  }
}
