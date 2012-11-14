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

package ru.org.linux.gallery;

import ru.org.linux.user.User;
import ru.org.linux.util.ImageInfo;

public class PreparedGalleryItem {
  private final GalleryItem item;
  private final User user;

  private final ImageInfo iconInfo;
  private final ImageInfo fullInfo;

  public PreparedGalleryItem(GalleryItem item, User user, ImageInfo iconInfo, ImageInfo fullInfo) {
    this.item = item;
    this.user = user;
    this.iconInfo = iconInfo;
    this.fullInfo = fullInfo;
  }

  public GalleryItem getItem() {
    return item;
  }

  public User getUser() {
    return user;
  }

  public ImageInfo getIconInfo() {
    return iconInfo;
  }

  public ImageInfo getFullInfo() {
    return fullInfo;
  }
}
