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
package ru.org.linux.util.image

import org.specs2.mutable.Specification

class ImageInfoSpec extends Specification {
  "ImageInfo" should {
    "parse jpeg with exif" in {
      val info = new ImageInfo("src/test/resources/images/exif.jpg")

      info.getWidth must be equalTo 4000
      info.getHeight must be equalTo 3000
    }
  }

}
