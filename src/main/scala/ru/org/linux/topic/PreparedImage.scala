/*
 * Copyright 1998-2025 Linux.org.ru
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
package ru.org.linux.topic

import ru.org.linux.gallery.{GalleryItem, Image}
import ru.org.linux.user.User
import ru.org.linux.util.image.ImageInfo

import scala.beans.BeanProperty

case class PreparedImage(@BeanProperty mediumName: String, @BeanProperty mediumInfo: ImageInfo,
                         @BeanProperty fullName: String, @BeanProperty fullInfo: ImageInfo, @BeanProperty image: Image,
                         @BeanProperty lazyLoad: Boolean) {
  def getSrcset: String = {
    if (fullInfo.getWidth <= Image.MaxScaledSize) {
      image.getSrcsetUpTo(fullInfo.getWidth) + ", " + fullName + " " + fullInfo.getWidth + "w"
    } else {
      image.getSrcset
    }
  }

  def getLoadingCode: String = if (lazyLoad) "loading=\"lazy\"" else ""
}

case class PreparedGalleryItem(@BeanProperty item: GalleryItem, @BeanProperty user: User,
                               @BeanProperty mediumInfo: ImageInfo)