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

package ru.org.linux.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.BadInputException;
import ru.org.linux.site.DefaultProfile;
import ru.org.linux.site.Template;

import javax.servlet.ServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/people/{nick}/settings")
public class EditProfileController {
  private UserDao userDao;

  @Autowired
  public void setUserDao(UserDao userDao) {
    this.userDao = userDao;
  }

  
  @RequestMapping(method=RequestMethod.GET)
  public ModelAndView showForm(ServletRequest request, @PathVariable String nick) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    if(!tmpl.getNick().equals(nick)) {
      throw new AccessViolationException("Not authorized");
    }

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("stylesList", DefaultProfile.getStyleList());
    params.put("avatarsList", DefaultProfile.getAvatars());


    return new ModelAndView("edit-profile", params);
  }

  @RequestMapping(method=RequestMethod.POST)
  public ModelAndView editProfile(
          ServletRequest request,
          @RequestParam("tags") int tags,
          @RequestParam("topics") int topics,
          @RequestParam("messages") int messages,
          @PathVariable String nick
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    if(!tmpl.getNick().equals(nick)) {
      throw new AccessViolationException("Not authorized");
    }

    if (topics <= 0 || topics > 500) {
      throw new BadInputException("некорректное число тем");
    }

    if (messages <= 0 || messages > 1000) {
      throw new BadInputException("некорректное число сообщений");
    }

    if (tags<=0 || tags>100) {
      throw new BadInputException("некорректное число меток в облаке");
    }

    if(!DefaultProfile.getStyleList().contains(request.getParameter("style"))) {
      throw new BadInputException("неправльное название темы");
    }

    tmpl.getProf().setTopics(topics);
    tmpl.getProf().setMessages(messages);
    tmpl.getProf().setTags(tags);
    tmpl.getProf().setShowNewFirst("on".equals(request.getParameter("newfirst")));
    tmpl.getProf().setShowPhotos("on".equals(request.getParameter("photos")));
    tmpl.getProf().setHideAdsense("on".equals(request.getParameter("hideAdsense")));
    tmpl.getProf().setShowGalleryOnMain("on".equals(request.getParameter("mainGallery")));
    tmpl.getProf().setFormatMode(request.getParameter("format_mode"));
    tmpl.getProf().setStyle(request.getParameter("style")); // TODO убрать как только
    userDao.setStyle(tmpl.getCurrentUser(), request.getParameter("style"));
    
    tmpl.getProf().setShowSocial("on".equals(request.getParameter("showSocial")));

    String avatar = request.getParameter("avatar");

    if (!DefaultProfile.getAvatars().contains(avatar)) {
      throw new BadInputException("invalid avatar value");
    }

    tmpl.getProf().setAvatarMode(avatar);

    tmpl.getProf().setShowAnonymous("on".equals(request.getParameter("showanonymous")));
    tmpl.getProf().setUseHover("on".equals(request.getParameter("hover")));

    tmpl.writeProfile(nick);

    return new ModelAndView(new RedirectView("/people/" + nick + "/profile"));
  }
}
