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

package ru.org.linux.topic;

import ru.org.linux.gallery.Image;
import ru.org.linux.util.ImageCheck;
import ru.org.linux.util.ImageInfo;

public class PreparedImage {
  private final String mediumName;
  private final String fullName;

  private final ImageCheck mediumInfo;
  private final ImageCheck fullInfo;

  private final Image image;

  public PreparedImage(String mediumName, ImageCheck mediumInfo, String fullName, ImageCheck fullInfo, Image image) {
    this.mediumName = mediumName;
    this.mediumInfo = mediumInfo;
    this.fullName = fullName;
    this.fullInfo = fullInfo;
    this.image = image;
  }

  public String getMediumName() {
    return mediumName;
  }

  public ImageCheck getMediumInfo() {
    return mediumInfo;
  }

  public String getFullName() {
    return fullName;
  }

  public ImageCheck getFullInfo() {
    return fullInfo;
  }

  public Image getImage() {
    return image;
  }
}
