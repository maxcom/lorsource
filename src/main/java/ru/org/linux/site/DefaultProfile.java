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

package ru.org.linux.site;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import ru.org.linux.tracker.TrackerFilterEnum;

import javax.annotation.Nonnull;
import java.util.List;

import static ru.org.linux.user.Profile.*;

public final class DefaultProfile {
  private static final ImmutableMap<String, String> BOX_LEGEND = new ImmutableMap.Builder<String,String>()
      .put("poll", "Текущий опрос")
      .put("top10", "Наиболее обсуждаемые темы этого месяца")
      .put("gallery", "Галерея")
      .put("tagcloud", "Облако тэгов")
      .put("archive", "Архив новостей")
      //.put("ibm", "IBM developerWorks")
      .put("lastMiniNews", "Последние мининовости (не будут отображаться в ленте новостей на главной странице)").build();

  private static final ImmutableSet<String> BOX_SET = BOX_LEGEND.keySet();

  private static final ImmutableMap<String, Theme> THEMES = Maps.uniqueIndex(Theme.THEMES, new Function<Theme, String>() {
    @Override
    public String apply(Theme input) {
      return input.getId();
    }
  });

  private static final ImmutableList<String> AVATAR_TYPES = ImmutableList.of("empty", "identicon", "monsterid", "wavatar", "retro");
  private static final Predicate<String> isBoxPredicate = new Predicate<String>() {
    @Override
    public boolean apply(String s) {
      return isBox(s);
    }
  };

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

    builder.put(NEWFIRST_PROPERTY, Boolean.FALSE);
    builder.put(HOVER_PROPERTY, Boolean.TRUE);
    builder.put(STYLE_PROPERTY, "tango");
    builder.put(FORMAT_MODE_PROPERTY, "quot");
    builder.put(TOPICS_PROPERTY, 30);
    builder.put(MESSAGES_PROPERTY, 50);
    builder.put(PHOTOS_PROPERTY, Boolean.TRUE);
    builder.put(SHOW_ANONYMOUS_PROPERTY, Boolean.TRUE);
    builder.put(AVATAR_PROPERTY, "empty");
    builder.put(HIDE_ADSENSE_PROPERTY, true);
    builder.put(MAIN_GALLERY_PROPERTY, false);
    builder.put(SHOW_SOCIAL_PROPERTY, true);
    builder.put(TRACKER_MODE, DEFAULT_TRACKER_MODE.getValue());

    builder.put("DebugMode", Boolean.FALSE);

// main page settings
    ImmutableList<String> boxes = ImmutableList.of(
      "poll", "top10", "gallery", "tagcloud", "archive"/* , "ibm" */
    );

    builder.put(BOXES_MAIN2_PROPERTY, boxes);

    return builder.build();
  }

  public static Predicate<String> boxPredicate() {
    return isBoxPredicate;
  }

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
    return Theme.THEMES.get(0);
  }
}
