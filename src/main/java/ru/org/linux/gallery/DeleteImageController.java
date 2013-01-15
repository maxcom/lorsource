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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.group.GroupPermissionService;
import ru.org.linux.site.Template;
import ru.org.linux.topic.*;
import ru.org.linux.user.User;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/delete_image")
public class DeleteImageController {
  @Autowired
  private ImageDao imageDao;

  @Autowired
  private ImageService imageService;

  @Autowired
  private TopicDao topicDao;

  @Autowired
  private TopicPrepareService prepareService;

  @Autowired
  private GroupPermissionService permissionService;

  private void checkDelete(PreparedTopic topic, User user) throws AccessViolationException {
    if (!permissionService.isEditable(topic, user)) {
      throw new AccessViolationException("Вы не можете редактировать эту тему");
    }

    if (topic.getSection().isImagepost()) {
      throw new AccessViolationException("В этой теме нельзя удалять изображения");
    }
  }

  @RequestMapping(method = RequestMethod.GET)
  public ModelAndView deleteForm(
          @RequestParam(required = true) int id,
          HttpServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    Image image = imageDao.getImage(id);
    Topic topic = topicDao.getById(image.getTopicId());
    PreparedTopic preparedTopic = prepareService.prepareTopic(topic, request.isSecure(), tmpl.getCurrentUser());

    checkDelete(preparedTopic, tmpl.getCurrentUser());

    ModelAndView mv = new ModelAndView("delete_image");

    mv.addObject("image", image);
    mv.addObject("preparedTopic", preparedTopic);

    return mv;
  }

  @RequestMapping(method = RequestMethod.POST)
  public RedirectView deleteImage(
          @RequestParam(required = true) int id,
          HttpServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    Image image = imageDao.getImage(id);
    Topic topic = topicDao.getById(image.getTopicId());
    PreparedTopic preparedTopic = prepareService.prepareTopic(topic, request.isSecure(), tmpl.getCurrentUser());

    checkDelete(preparedTopic, tmpl.getCurrentUser());

    imageService.deleteImage(tmpl.getCurrentUser(), image);

    return new RedirectView(TopicLinkBuilder.baseLink(topic).forceLastmod().build());
  }
}
