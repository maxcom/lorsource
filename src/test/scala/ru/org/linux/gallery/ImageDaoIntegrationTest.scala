/*
 * Copyright 1998-2026 Linux.org.ru
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

import munit.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import ru.org.linux.test.SpringTestSupport

import java.util.Date

@ContextHierarchy(
  Array(
    new ContextConfiguration(value = Array("classpath:database.xml")),
    new ContextConfiguration(classes = Array(classOf[ImageDaoIntegrationTestConfiguration]))
  ))
class ImageDaoIntegrationTest extends FunSuite with SpringTestSupport:

  @Autowired
  var imageDao: ImageDao = scala.compiletime.uninitialized

  test("getGalleryItemsTest"):
    val galleryDtoList = imageDao.getGalleryItems(3)
    assertEquals(galleryDtoList.size, 3)

    val commitDates = galleryDtoList.map(_.commitDate)
    val sortedDesc = commitDates.sorted(using Ordering[Date].reverse)
    assertEquals(commitDates, sortedDesc, "gallery items must be ordered by commitdate DESC")

end ImageDaoIntegrationTest
