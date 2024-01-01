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
package ru.org.linux.util.image;

/**
 */
public class ImageParam {
  private final String formatName;
  private final boolean animated;
  private final int height;
  private final int width;
  private final long size;
  private final String htmlCode;
  private final String humanSize;

  public ImageParam(String formatName, boolean animated, int height, int width, long size) {
    this.formatName = formatName;
    this.animated = animated;
    this.height = height;
    this.width = width;
    this.size = size;
    this.htmlCode = "width=\"" + width + "\" height=\"" + height + '"';
    this.humanSize = size / 1024 + " Kb";
  }

  public String getFormatName() {
    return formatName;
  }

  public String getHtmlCode() {
    return htmlCode;
  }

  public long getSize() {
    return size;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public boolean isAnimated() {
    return animated;
  }

  public String getHumanSize() {
    return humanSize;
  }

  public String getExtension() {
    if("JPEG".equals(formatName)) {
      return "jpg";
    } else {
      return formatName;
    }
  }
}
