/*
 * Copyright 1998-2024 Linux.org.ru
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

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam}
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.auth.{AccessViolationException, AuthorizedSession}
import ru.org.linux.group.GroupPermissionService
import ru.org.linux.topic.*

@Controller
@RequestMapping(Array("/delete_image"))
class DeleteImageController(imageDao: ImageDao, imageService: ImageService, topicDao: TopicDao,
                            prepareService: TopicPrepareService, permissionService: GroupPermissionService) {
  @throws[AccessViolationException]
  private def checkDelete(topic: PreparedTopic)(implicit user: AuthorizedSession): Unit = {
    if (!permissionService.isEditable(topic)) {
      throw new AccessViolationException("Вы не можете редактировать эту тему")
    }

    if (topic.section.isImagepost) {
      throw new AccessViolationException("В этой теме нельзя удалять изображения")
    }
  }

  @RequestMapping(method = Array(RequestMethod.GET))
  def deleteForm(@RequestParam id: Int): ModelAndView = AuthorizedOnly { implicit currentUser =>
    val image = imageDao.getImage(id)
    val topic = topicDao.getById(image.topicId)

    val preparedTopic = prepareService.prepareTopic(topic)

    checkDelete(preparedTopic)

    val mv = new ModelAndView("delete_image")

    mv.addObject("image", image)
    mv.addObject("preparedTopic", preparedTopic)

    mv
  }

  @RequestMapping(method = Array(RequestMethod.POST))
  def deleteImage(@RequestParam id: Int): RedirectView = AuthorizedOnly { implicit currentUser =>
    val image = imageDao.getImage(id)
    val topic = topicDao.getById(image.topicId)

    val preparedTopic = prepareService.prepareTopic(topic)

    checkDelete(preparedTopic)

    imageService.deleteImage(image)

    new RedirectView(TopicLinkBuilder.baseLink(topic).forceLastmod.build)
  }
}