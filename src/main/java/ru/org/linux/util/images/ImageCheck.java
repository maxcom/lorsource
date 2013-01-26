/*
 * Copyright 1998-2012 Linux.org.ru
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
package ru.org.linux.util.images;


/**
 */
public class ImageCheck {
  private final String formatName;
  private final boolean animated;
  private final int height;
  private final int width;
  private final long size;
  private final String code;
  private final String sizeString;

  public ImageCheck(String formatName, boolean animated, int width, int height, long size) {
    this.formatName = formatName;
    this.animated = animated;
    this.width = width;
    this.height = height;
    this.size = size;
    this.code = "width=\"" + width + "\" height=\"" + height + '"';
    this.sizeString =  size / 1024 + " Kb";
  }

  public String getFormatName() {
    return formatName;
  }

  public boolean isAnimated() {
    return animated;
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  public long getSize() {
    return size;
  }

  /**
   * get file size in user-printable form
   */
  public String getSizeString() {
    return sizeString;
  }

  /**
   * get HTML code for inclusion into IMG tag
   */
  public String getCode() {
    return code;
  }

  /**
   * Get extension for filename
   * @return ext
   */
  public String getExtension() {
    if("JPEG".equals(formatName)) {
      return "jpg";
    } else {
      return formatName;
    }
  }
}
