/*
 * Copyright 1998-2017 Linux.org.ru
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

import ru.org.linux.gallery.Image.{Medium2xWidth, MediumWidth}
import ru.org.linux.user.User

import scala.beans.BeanProperty

object Image {
  val MaxFileSize = 3 * 1024 * 1024
  val MinDimension = 400
  val MaxDimension = 5120

  val MediumWidth = 500
  val Medium2xWidth = 1000

  private val GalleryName = "(gallery/[^.]+)(?:\\.\\w+)".r
  private val ImagesName = "images/.*".r

  private def mediumName(name: String, doubleSize: Boolean, id: Int): String = {
    name match {
      case GalleryName(base) ⇒
        if (doubleSize) {
          s"$base-med-2x.jpg"
        } else {
          s"$base-med.jpg"
        }
      case ImagesName() ⇒
        if (doubleSize) {
          s"images/$id/${Medium2xWidth}px.jpg"
        } else {
          s"images/$id/${MediumWidth}px.jpg"
        }
      case _ ⇒
        throw new IllegalArgumentException(s"Not gallery path: $name")
    }
  }
}

case class Image(
  @BeanProperty id: Int,
  @BeanProperty topicId: Int,
  @BeanProperty original: String
) {
  def getMedium: String = Image.mediumName(original, doubleSize = false, id)
  private def getMedium2x: String = Image.mediumName(original, doubleSize = true, id)

  def getSrcset: String =
    s"$getMedium2x ${Medium2xWidth}w, " +
    s"$getMedium ${MediumWidth}w"
}

case class PreparedGalleryItem(
  @BeanProperty item: GalleryItem,
  @BeanProperty user: User)
