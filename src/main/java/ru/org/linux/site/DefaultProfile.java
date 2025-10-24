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

package ru.org.linux.site;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import ru.org.linux.markup.MarkupType;
import ru.org.linux.tracker.TrackerFilterEnum;

import javax.annotation.Nonnull;
import java.util.List;

public final class DefaultProfile {
  public static String STYLE_PROPERTY = "style";
  public static String FORMAT_MODE_PROPERTY = "format.mode";
  public static String MESSAGES_PROPERTY = "messages";
  public static String TOPICS_PROPERTY = "topics";
  public static String HIDE_ADSENSE_PROPERTY = "hideAdsense";
  public static String PHOTOS_PROPERTY = "photos";
  public static String MAIN_GALLERY_PROPERTY = "mainGallery";
  public static String AVATAR_PROPERTY = "avatar";
  public static String BOXES_MAIN2_PROPERTY = "main2";
  public static String TRACKER_MODE = "trackerMode";
  public static String OLD_TRACKER = "oldTracker";
  public static String REACTION_NOTIFICATION_PROPERTY = "reactionNotification";

  private static final ImmutableMap<String, String> BOX_LEGEND = new ImmutableMap.Builder<String,String>()
      .put("poll", "Текущий опрос")
      .put("articles", "Новые статьи")
      .put("top10", "Наиболее обсуждаемые темы этого месяца")
      .put("gallery", "Галерея")
      .put("tagcloud", "Облако тэгов")
      .put("lastMiniNews", "Последние мининовости (не будут отображаться в ленте новостей на главной странице)").build();

  private static final ImmutableSet<String> BOX_SET = BOX_LEGEND.keySet();

  private static final ImmutableMap<String, Theme> THEMES = Maps.uniqueIndex(Theme.THEMES, Theme::getId);

  private static final ImmutableList<String> AVATAR_TYPES =
          ImmutableList.of("empty", "identicon", "monsterid", "wavatar", "retro", "robohash");

  public static final TrackerFilterEnum DEFAULT_TRACKER_MODE = TrackerFilterEnum.MAIN;

  private static final ImmutableMap<String, Object> defaultProfile = createDefaultProfile();

  private DefaultProfile() {
  }

  public static ImmutableMap<String, String> getAllBoxes() {
    return BOX_LEGEND;
  }

  public static boolean isBox(String name) {
    return BOX_SET.contains(name);
  }

  public static ImmutableMap<String, Object> getDefaultProfile() {
    return defaultProfile;
  }

  private static ImmutableMap<String, Object> createDefaultProfile() {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

    builder.put(STYLE_PROPERTY, "tango");
    builder.put(FORMAT_MODE_PROPERTY, MarkupType.Markdown$.MODULE$.formId());
    builder.put(TOPICS_PROPERTY, 30);
    builder.put(MESSAGES_PROPERTY, 50);
    builder.put(PHOTOS_PROPERTY, Boolean.TRUE);
    builder.put(AVATAR_PROPERTY, "empty");
    builder.put(HIDE_ADSENSE_PROPERTY, true);
    builder.put(MAIN_GALLERY_PROPERTY, false);
    builder.put(TRACKER_MODE, DEFAULT_TRACKER_MODE.getValue());
    builder.put(OLD_TRACKER, false);
    builder.put(REACTION_NOTIFICATION_PROPERTY, true);

    builder.put("DebugMode", Boolean.FALSE);

    // main page settings

    builder.put(BOXES_MAIN2_PROPERTY, ImmutableList.of("poll", "articles", "top10", "gallery", "tagcloud"));

    return builder.build();
  }

  public static final ImmutableSet<Integer> TOPICS_VALUES = ImmutableSet.of(30, 50, 100, 200, 300, 500);
  public static final ImmutableSet<Integer> COMMENTS_VALUES = ImmutableSet.of(25, 50, 100, 200, 300, 500);

  public static boolean isStyle(String style) {
    return THEMES.containsKey(style);
  }

  public static List<String> getAvatars() {
    return AVATAR_TYPES;
  }

  @Nonnull
  public static Theme getTheme(String id) {
    Theme theme = THEMES.get(id);

    if (theme==null) {
      return getDefaultTheme();
    }

    return theme;
  }

  @Nonnull
  public static Theme getDefaultTheme() {
    return Theme.THEMES.getFirst();
  }
}
