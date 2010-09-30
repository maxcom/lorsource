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

import java.util.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections.Predicate;

public final class DefaultProfile {
  private static final String[] BOXLIST = {"poll", "top10", "gallery", "tagcloud", "archive", "ibm", "tshirt"};
  private static final ImmutableSet<String> BOX_SET = ImmutableSet.of(BOXLIST);

  private static final String[] STYLES = { "black", "white", "white2", "tango" };
  private static final ImmutableList<String> STYLE_LIST = ImmutableList.of(STYLES);
  private static final ImmutableSet<String> STYLE_SET = ImmutableSet.of(STYLES);

  private static final ImmutableList<String> AVATAR_TYPES = ImmutableList.of("empty", "identicon", "monsterid", "wavatar");

  private static final Predicate isBoxPredicate = new Predicate() {
      @Override
      public boolean evaluate(Object o) {
        return isBox((String)o);
      }
    };
  public static final String HIDE_ADSENSE = "hideAdsense";
  public static final String MAIN_GALLERY = "mainGallery";


  private static final ImmutableMap<String, Object> defaultProfile = ImmutableMap.copyOf(createDefaultProfile());

  private DefaultProfile() {
  }

  public static ImmutableSet<String> getAllBoxes() {
    return BOX_SET;
  }

  public static boolean isBox(String name) {
    return BOX_SET.contains(name);
  }

  public static ImmutableMap<String, Object> getDefaultProfile() {
    return defaultProfile;
  }

  private static Map<String, Object> createDefaultProfile() {
    Map<String, Object> defaults = new HashMap<String, Object>();

    defaults.put("newfirst", Boolean.FALSE);
    defaults.put("hover", Boolean.TRUE);
    defaults.put("style", "black");
    defaults.put("format.mode", "quot");
    defaults.put("topics", 30);
    defaults.put("messages", 50);
    defaults.put("tags", 50);
    defaults.put("photos", Boolean.TRUE);
    defaults.put("system.timestamp", new Date().getTime());
    defaults.put("showinfo", Boolean.TRUE);
    defaults.put("showanonymous", Boolean.TRUE);
    defaults.put("showsticky", Boolean.TRUE);
    defaults.put("avatar", "empty");
    defaults.put(HIDE_ADSENSE, true);
    defaults.put(MAIN_GALLERY, false);

    defaults.put("DebugMode", Boolean.FALSE);

// main page settings
    defaults.put("main.3columns", Boolean.FALSE);

    ImmutableList<String> boxes = ImmutableList.of(
      "ibm", "poll", "top10", "gallery", "tagcloud", "archive", "tshirt"
    );

    defaults.put("main2", boxes);

    ImmutableList<String> boxes31 = ImmutableList.of(
      "ibm", "poll", "archive", "tagcloud"
    );

    defaults.put("main3-1", boxes31);

    ImmutableList<String> boxes32 = ImmutableList.of(
      "top10", "gallery", "tshirt"
    );

    defaults.put("main3-2", boxes32);

    return defaults;
  }

  public static Predicate getBoxPredicate() {
    return isBoxPredicate;
  }

  public static boolean isStyle(String style) {
    return STYLE_SET.contains(style);
  }

  public static List<String> getStyleList() {
    return STYLE_LIST;
  }

  public static List<String> getAvatars() {
    return AVATAR_TYPES;
  }
}
