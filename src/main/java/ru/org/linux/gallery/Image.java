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

package ru.org.linux.gallery;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Image {
  private static final Pattern GALLERY_NAME = Pattern.compile("(gallery/[^.]+)(\\.\\w+)");

  private final int id;
  private final int topicId;
  private final String original;
  private final String icon;

  public Image(int id, int topicId, String original, String icon) {
    this.id = id;
    this.topicId = topicId;
    this.original = original;
    this.icon = icon;
  }

  public int getId() {
    return id;
  }

  public int getTopicId() {
    return topicId;
  }

  public String getOriginal() {
    return original;
  }

  public String getIcon() {
    return icon;
  }

  public String getMedium() {
    return getMediumName(original);
  }

  private static String getMediumName(String name) {
    Matcher m = GALLERY_NAME.matcher(name);

    if (!m.matches()) {
      throw new IllegalArgumentException("Not gallery path: "+name);
    }

    return m.group(1)+"-med.jpg";
  }
}
