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

import com.google.common.collect.ImmutableList;
import ru.org.linux.markup.MarkupType$;
import ru.org.linux.site.DefaultProfile;
import ru.org.linux.tracker.TrackerFilterEnum;
import ru.org.linux.util.ProfileHashtable;

import java.util.List;
import java.util.Map;

public class Profile {
  public static final Profile DEFAULT =
          new Profile(new ProfileHashtable(DefaultProfile.getDefaultProfile(), Map.of()), null);

  public static final String STYLE_PROPERTY = "style";
  public static final String FORMAT_MODE_PROPERTY = "format.mode";
  public static final String MESSAGES_PROPERTY = "messages";
  public static final String TOPICS_PROPERTY = "topics";
  public static final String HIDE_ADSENSE_PROPERTY = "hideAdsense";
  public static final String PHOTOS_PROPERTY = "photos";
  public static final String MAIN_GALLERY_PROPERTY = "mainGallery";
  public static final String AVATAR_PROPERTY = "avatar";
  public static final String BOXES_MAIN2_PROPERTY = "main2";
  public static final String TRACKER_MODE = "trackerMode";
  public static final String OLD_TRACKER = "oldTracker";

  public static final String REACTION_NOTIFICATION_PROPERTY = "reactionNotification";

  private final String style;
  private final String formatMode;
  private final int messages;
  private final int topics;
  private final boolean showPhotos;
  private final boolean hideAdsense;
  private final boolean showGalleryOnMain;
  private final String avatarMode;
  private final boolean oldTracker;
  private final TrackerFilterEnum trackerMode;
  private final boolean reactionNotification;

  private final ImmutableList<String> boxes;

  public Profile(ProfileHashtable p, List<String> boxes) {
    style = fixStyle(p.getString(STYLE_PROPERTY));
    formatMode = fixFormat(p.getString(FORMAT_MODE_PROPERTY));
    messages = p.getInt(MESSAGES_PROPERTY);
    topics = p.getInt(TOPICS_PROPERTY);
    showPhotos = p.getBoolean(PHOTOS_PROPERTY);
    hideAdsense = p.getBoolean(HIDE_ADSENSE_PROPERTY);
    showGalleryOnMain = p.getBoolean(MAIN_GALLERY_PROPERTY);
    avatarMode = p.getString(AVATAR_PROPERTY);

    trackerMode = TrackerFilterEnum.getByValue(p.getString(TRACKER_MODE))
            .filter(TrackerFilterEnum::isCanBeDefault)
            .orElse(DefaultProfile.DEFAULT_TRACKER_MODE);

    oldTracker = p.getBoolean(OLD_TRACKER);
    reactionNotification = p.getBoolean(REACTION_NOTIFICATION_PROPERTY);

    if (boxes!=null) {
      this.boxes = ImmutableList.copyOf(boxes);
    } else {
      this.boxes = (ImmutableList<String>) DefaultProfile.getDefaultProfile().get(BOXES_MAIN2_PROPERTY);
    }
  }

  public String getStyle() {
    return style;
  }

  public String getFormatMode() {
    return formatMode;
  }

  public int getMessages() {
    return messages;
  }

  public int getTopics() {
    return topics;
  }

  public boolean isShowPhotos() {
    return showPhotos;
  }

  public boolean isHideAdsense() {
    return hideAdsense;
  }

  public boolean isShowGalleryOnMain() {
    return showGalleryOnMain;
  }

  public String getAvatarMode() {
    return avatarMode;
  }

  public boolean isMiniNewsBoxletOnMainPage() {
    return getBoxlets().contains("lastMiniNews");
  }

  public boolean isOldTracker() {
    return oldTracker;
  }

  public static String fixFormat(String mode) {
    if (MarkupType$.MODULE$.AllFormIds().contains(mode)) {
      return mode;
    } else {
      return (String) DefaultProfile.getDefaultProfile().get("format.mode");
    }
  }

  public static String fixStyle(String style) {
    if (!DefaultProfile.isStyle(style)) {
      return (String) DefaultProfile.getDefaultProfile().get(STYLE_PROPERTY);
    }

    return style;
  }

  public TrackerFilterEnum getTrackerMode() {
    return trackerMode;
  }

  public List<String> getBoxlets() {
    return boxes;
  }

  public boolean isReactionNotificationEnabled() {
    return reactionNotification;
  }
}
