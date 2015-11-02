/*
 * Copyright 1998-2015 Linux.org.ru
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
package ru.org.linux.gallery

import ru.org.linux.user.User
import ru.org.linux.util.image.ImageInfo

import scala.beans.BeanProperty

object Image {
  private val GalleryName = "(gallery/[^.]+)(?:\\.\\w+)".r

  private def mediumName(name: String, doubleSize: Boolean): String = {
    name match {
      case GalleryName(base) ⇒
        if (doubleSize) {
          s"$base-med-2x.jpg"
        } else {
          s"$base-med.jpg"
        }
      case _ ⇒
        throw new IllegalArgumentException("Not gallery path: " + name)
    }
  }
}

case class Image(
  @BeanProperty id:Int,
  @BeanProperty topicId:Int,
  @BeanProperty original: String,
  @BeanProperty icon: String
) {
  def getMedium = Image.mediumName(original, doubleSize = false)
  def getMedium2x = Image.mediumName(original, doubleSize = true)

  def getSrcset = s"$getMedium2x ${Screenshot.MEDIUM_2X_WIDTH}w, $getMedium ${Screenshot.MEDIUM_WIDTH}w, $icon ${Screenshot.ICON_WIDTH}w"
}

case class PreparedGalleryItem(
  @BeanProperty item:GalleryItem,
  @BeanProperty user:User,
  @BeanProperty iconInfo:ImageInfo,
  @BeanProperty fullInfo:ImageInfo
)