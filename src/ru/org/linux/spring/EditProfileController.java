/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.spring;

import javax.servlet.ServletRequest;

import ru.org.linux.site.AccessViolationException;
import ru.org.linux.site.BadInputException;
import ru.org.linux.site.DefaultProfile;
import ru.org.linux.site.Template;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/edit-profile.jsp")
public class EditProfileController {
  @RequestMapping(method=RequestMethod.GET)
  public ModelAndView showForm(ServletRequest request) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    return new ModelAndView("edit-profile");
  }

  @RequestMapping(method=RequestMethod.POST)
  public ModelAndView editProfile(ServletRequest request) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    String profile = tmpl.getNick();

    int topics = Integer.parseInt(request.getParameter("topics"));
    int messages = Integer.parseInt(request.getParameter("messages"));
    int tags = Integer.parseInt(request.getParameter("tags"));

    if (topics <= 0 || topics > 500) {
      throw new BadInputException("некорректное число тем");
    }

    if (messages <= 0 || messages > 1000) {
      throw new BadInputException("некорректное число сообщений");
    }

    if (tags<=0 || tags>100) {
      throw new BadInputException("некорректное число меток в облаке");
    }

    tmpl.getProf().setTopics(topics);
    tmpl.getProf().setMessages(messages);
    tmpl.getProf().setTags(tags);
    tmpl.getProf().setShowNewFirst("on".equals(request.getParameter("newfirst")));
    tmpl.getProf().setShowPhotos("on".equals(request.getParameter("photos")));
    tmpl.getProf().setHideAdsense("on".equals(request.getParameter("hide_adsense")));
    tmpl.getProf().setShowGalleryOnMain("on".equals(request.getParameter("mainGallery")));
    tmpl.getProf().setFormatMode(request.getParameter("format_mode"));
    tmpl.getProf().setStyle(request.getParameter("style"));

    String avatar = request.getParameter("avatar");

    if (!DefaultProfile.getAvatars().contains(avatar)) {
      throw new BadInputException("invalid avatar value");
    }

    tmpl.getProf().setAvatarMode(avatar);

    tmpl.getProf().setThreeColumnsOnMain("on".equals(request.getParameter("3column")));

    tmpl.getProf().setShowAnonymous("on".equals(request.getParameter("showanonymous")));
    tmpl.getProf().setUseHover("on".equals(request.getParameter("hover")));

    tmpl.writeProfile(profile);

    return new ModelAndView(new RedirectView("/"));
  }
}
