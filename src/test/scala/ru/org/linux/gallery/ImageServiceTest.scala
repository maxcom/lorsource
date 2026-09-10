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
import org.mockito.Mockito
import ru.org.linux.edithistory.EditHistoryDao
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.TopicDao
import ru.org.linux.user.UserService

/** Юнит-тесты для [[ImageService]] (выборочно). */
class ImageServiceTest extends FunSuite:
  private val siteConfig = Mockito.mock(classOf[SiteConfig])
  private val imageDao = Mockito.mock(classOf[ImageDao])
  private val editHistoryDao = Mockito.mock(classOf[EditHistoryDao])
  private val topicDao = Mockito.mock(classOf[TopicDao])
  private val userService = Mockito.mock(classOf[UserService])
  private val springDB = Mockito.mock(classOf[SpringDB])

  private def newService(): ImageService =
    Mockito.when(siteConfig.getUploadPath).thenReturn("/tmp/lor-image-service-test")
    Mockito.when(siteConfig.getSecureUrl).thenReturn("http://localhost/")
    new ImageService(imageDao, editHistoryDao, topicDao, userService, siteConfig, springDB)

  test("prepareImageReturnsNoneForPurged"):
    val service = newService()
    val image = Image(id = 42, topicId = 1, original = "images/42/original.jpg", deleted = true, purged = true)

    val prepared = service.prepareImage(image)

    assert(prepared.isEmpty, "purged image should not be prepared")

  test("prepareImageReturnsNoneForPurgedLazy"):
    val service = newService()
    val image = Image(id = 43, topicId = 1, original = "images/43/original.jpg", deleted = false, purged = true)

    val prepared = service.prepareImage(image, lazyLoad = true)

    assert(prepared.isEmpty, "purged image should not be prepared (lazy)")
