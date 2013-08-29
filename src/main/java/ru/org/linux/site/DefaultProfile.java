/*
 * Copyright 1998-2013 Linux.org.ru
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

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.org.linux.user.Profile.*;

public final class DefaultProfile {
  private static final ImmutableMap<String, String> BOX_LEGEND = new ImmutableMap.Builder<String,String>()
      .put("poll", "Текущий опрос")
      .put("top10", "Наиболее обсуждаемые темы этого месяца")
      .put("gallery", "Галерея")
      .put("tagcloud", "Облако тэгов")
      .put("archive", "Архив новостей")
      .put("ibm", "IBM developerWorks")
      .put("lastMiniNews", "Последние мининовости (не будут отображаться в ленте новостей на главной странице)").build();

  private static final ImmutableSet<String> BOX_SET = BOX_LEGEND.keySet();

  private static final ImmutableMap<String, Theme> THEMES = Maps.uniqueIndex(Theme.THEMES, new Function<Theme, String>() {
    @Override
    public String apply(Theme input) {
      return input.getId();
    }
  });

  private static final ImmutableSet<String> STYLE_NAMES = ImmutableSet.copyOf(THEMES.keySet());

  private static final ImmutableList<String> AVATAR_TYPES = ImmutableList.of("empty", "identicon", "monsterid", "wavatar", "retro");
  private static final Predicate<String> isBoxPredicate = new Predicate<String>() {
    @Override
    public boolean apply(String s) {
      return isBox(s);
    }
  };

  private static final ImmutableMap<String, Object> defaultProfile = ImmutableMap.copyOf(createDefaultProfile());

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

  private static Map<String, Object> createDefaultProfile() {
    Map<String, Object> defaults = new HashMap<>();

    defaults.put(NEWFIRST_PROPERTY, Boolean.FALSE);
    defaults.put(HOVER_PROPERTY, Boolean.TRUE);
    defaults.put(STYLE_PROPERTY, "tango");
    defaults.put(FORMAT_MODE_PROPERTY, "quot");
    defaults.put(TOPICS_PROPERTY, 30);
    defaults.put(MESSAGES_PROPERTY, 50);
    defaults.put(PHOTOS_PROPERTY, Boolean.TRUE);
    defaults.put(SHOW_ANONYMOUS_PROPERTY, Boolean.TRUE);
    defaults.put(AVATAR_PROPERTY, "empty");
    defaults.put(HIDE_ADSENSE_PROPERTY, true);
    defaults.put(MAIN_GALLERY_PROPERTY, false);
    defaults.put(SHOW_SOCIAL_PROPERTY, true);

    defaults.put("DebugMode", Boolean.FALSE);

// main page settings
    ImmutableList<String> boxes = ImmutableList.of(
      "ibm", "poll", "top10", "gallery", "tagcloud", "archive"
    );

    defaults.put(BOXES_MAIN2_PROPERTY, boxes);

    return defaults;
  }

  public static Predicate<String> boxPredicate() {
    return isBoxPredicate;
  }

  public static boolean isStyle(String style) {
    return THEMES.containsKey(style);
  }

  @Deprecated
  public static Collection<String> getStyles() {
    return STYLE_NAMES;
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
