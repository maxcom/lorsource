/*
 * Copyright 1998-2018 Linux.org.ru
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

import scala.beans.BeanProperty

object Image {
  val MaxFileSize = 5 * 1024 * 1024
  val MinDimension = 400
  val MaxDimension = 5120

  val Sizes: Seq[Int] = Seq(500, 1000, 1500) // default size first
  val MaxScaledSize: Int = Sizes.max

  private val GalleryName = "(gallery/[^.]+)(?:\\.\\w+)".r
  private val ImagesName = "images/.*".r

  private def srcset(name: String, id: Int, maxWidth: Int = Int.MaxValue): String = {
    name match {
      case GalleryName(base) ⇒
        Sizes.filter(_ < maxWidth).map(size ⇒ s"$base-${size}px.jpg ${size}w").mkString(", ")
      case ImagesName() ⇒
        Sizes.filter(_ < maxWidth).map(size ⇒ s"images/$id/${size}px.jpg ${size}w").mkString(", ")
      case _ ⇒
        throw new IllegalArgumentException(s"Not gallery path: $name")
    }
  }

  private def main(name: String, id: Int): String = {
    name match {
      case GalleryName(base) ⇒
        s"$base-${Sizes.head}px.jpg"
      case ImagesName() ⇒
        s"images/$id/${Sizes.head}px.jpg"
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
  def getMedium: String = Image.main(original, id)
  def getSrcset: String = Image.srcset(original, id)
  def getSrcsetUpTo(width: Int): String = Image.srcset(original, id, width)
}

case class PreparedGalleryItem(
  @BeanProperty item: GalleryItem,
  @BeanProperty user: User)
