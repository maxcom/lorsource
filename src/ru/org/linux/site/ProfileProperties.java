/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.site;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.org.linux.util.ProfileHashtable;

public class ProfileProperties {
  public static final String STYLE_PROPERTY = "style";
  public static final String FORMAT_MODE_PROPERTY = "format.mode";
  public static final String HOVER_PROPERTY = "hover";
  public static final String MESSAGES_PROPERTY = "messages";
  public static final String NEWFIRST_PROPERTY = "newfirst";
  public static final String TOPICS_PROPERTY = "topics";
  public static final String TAGS_PROPERTY = "tags";
  public static final String HIDE_ADSENSE_PROPERTY = "hideAdsense";
  public static final String PHOTOS_PROPERTY = "photos";
  public static final String MAIN_GALLERY_PROPERTY = "mainGallery";
  public static final String AVATAR_PROPERTY = "avatar";
  public static final String MAIN_3COLUMNS_PROPERTY = "main.3columns";
  public static final String SHOWINFO_PROPERTY = "showinfo";
  public static final String SHOW_ANONYMOUS_PROPERTY = "showanonymous";
  public static final String BOXES_MAIN2_PROPERTY = "main2";
  public static final String BOXES_MAIN31_PROPERTY = "main3-1";
  public static final String BOXES_MAIN32_PROPERTY = "main3-2";
  public static final String TIMESTAMP_PROPERTY = "system.timestamp";

  private String style;
  private String formatMode;
  private boolean useHover;
  private int messages;
  private boolean showNewFirst;
  private int topics;
  private int tags;
  private boolean showPhotos;
  private boolean hideAdsense;
  private boolean showGalleryOnMain;
  private String avatarMode;
  private boolean threeColumnsOnMain;
  private boolean showInfo;
  private boolean showAnonymous;
  private final long timestamp;

  private final Map<String, List<String>> boxes = new HashMap<String, List<String>>();

  public ProfileProperties(ProfileHashtable p) {
    style = fixStyle(p.getString(STYLE_PROPERTY));
    formatMode = fixFormat(p.getString(FORMAT_MODE_PROPERTY));
    useHover = p.getBoolean(HOVER_PROPERTY);
    messages = p.getInt(MESSAGES_PROPERTY);
    showNewFirst = p.getBoolean(NEWFIRST_PROPERTY);
    topics = p.getInt(TOPICS_PROPERTY);
    tags = p.getInt(TAGS_PROPERTY);
    showPhotos = p.getBoolean(PHOTOS_PROPERTY);
    hideAdsense = p.getBoolean(HIDE_ADSENSE_PROPERTY);
    showGalleryOnMain = p.getBoolean(MAIN_GALLERY_PROPERTY);
    avatarMode = p.getString(AVATAR_PROPERTY);
    threeColumnsOnMain = p.getBoolean(MAIN_3COLUMNS_PROPERTY);
    showInfo = p.getBoolean(SHOWINFO_PROPERTY);
    showAnonymous = p.getBoolean(SHOW_ANONYMOUS_PROPERTY);
    timestamp = p.getLong(TIMESTAMP_PROPERTY);

    boxes.put(BOXES_MAIN2_PROPERTY, (List<String>) p.getSettings().get(BOXES_MAIN2_PROPERTY));
    boxes.put(BOXES_MAIN31_PROPERTY, (List<String>) p.getSettings().get(BOXES_MAIN31_PROPERTY));
    boxes.put(BOXES_MAIN32_PROPERTY, (List<String>) p.getSettings().get(BOXES_MAIN32_PROPERTY));
  }

  public ProfileHashtable getHashtable() {
    ProfileHashtable p = new ProfileHashtable(DefaultProfile.getDefaultProfile(), new HashMap<String, Object>());

    p.setString(STYLE_PROPERTY, style);
    p.setString(FORMAT_MODE_PROPERTY, formatMode);
    p.setBoolean(HOVER_PROPERTY, useHover);
    p.setInt(MESSAGES_PROPERTY, messages);
    p.setBoolean(NEWFIRST_PROPERTY, showNewFirst);
    p.setInt(TOPICS_PROPERTY, topics);
    p.setInt(TAGS_PROPERTY, tags);
    p.setBoolean(PHOTOS_PROPERTY, showPhotos);
    p.setBoolean(HIDE_ADSENSE_PROPERTY, hideAdsense);
    p.setBoolean(MAIN_GALLERY_PROPERTY, showGalleryOnMain);
    p.setString(AVATAR_PROPERTY, avatarMode);
    p.setBoolean(MAIN_3COLUMNS_PROPERTY, threeColumnsOnMain);
    p.setBoolean(SHOWINFO_PROPERTY, showInfo);
    p.setBoolean(SHOW_ANONYMOUS_PROPERTY, showAnonymous);

    p.setObject(BOXES_MAIN2_PROPERTY, boxes.get(BOXES_MAIN2_PROPERTY));
    p.setObject(BOXES_MAIN31_PROPERTY, boxes.get(BOXES_MAIN31_PROPERTY));
    p.setObject(BOXES_MAIN32_PROPERTY, boxes.get(BOXES_MAIN32_PROPERTY));

    return p;
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
    this.useHover = hover;
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
    this.showNewFirst = newFirst;
  }

  public int getTopics() {
    return topics;
  }

  public void setTopics(int topics) {
    this.topics = topics;
  }

  public int getTags() {
    return tags;
  }

  public void setTags(int tags) {
    this.tags = tags;
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

  public boolean isThreeColumnsOnMain() {
    return threeColumnsOnMain;
  }

  public void setThreeColumnsOnMain(boolean threeColumnsOnMain) {
    this.threeColumnsOnMain = threeColumnsOnMain;
  }

  public boolean isShowInfo() {
    return showInfo;
  }

  public void setShowInfo(boolean showInfo) {
    this.showInfo = showInfo;
  }

  public boolean isShowAnonymous() {
    return showAnonymous;
  }

  public void setShowAnonymous(boolean showAnonymous) {
    this.showAnonymous = showAnonymous;
  }

  private static String fixFormat(String mode) {
    if (!"ntobrq".equals(mode) &&
        !"quot".equals(mode) &&
        !"tex".equals(mode) &&
        !"ntobr".equals(mode) &&
        !"lorcode".equals(mode)) {
      return (String) Profile.getDefaults().get("format.mode");
    }

    return mode;
  }

  private static String fixStyle(String style) {
    if (!DefaultProfile.isStyle(style)) {
      return (String) Profile.getDefaults().get(STYLE_PROPERTY);
    }

    return style;
  }

  public List<String> getList(String name) {
    List<String> list = boxes.get(name);

    if (list==null) {
      return (List<String>) DefaultProfile.getDefaultProfile().get(name);
    } else {
      return list;
    }
  }

  public void setList(String name, List<String> list) {
    boxes.put(name, new ArrayList<String>(list));
  }

  public long getTimestamp() {
    return timestamp;
  }
}
