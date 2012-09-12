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

import javax.servlet.ServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.auth.FileProfileReader;
import ru.org.linux.site.BadInputException;
import ru.org.linux.site.DefaultProfile;

import static ru.org.linux.auth.AuthUtil.*;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.spring.Configuration;

@Controller
@RequestMapping("/people/{nick}/settings")
public class EditProfileController {

  @Autowired
  private Configuration configuration;

  private UserDao userDao;

  @Autowired
  public void setUserDao(UserDao userDao) {
    this.userDao = userDao;
  }

  
  @RequestMapping(method=RequestMethod.GET)
  public ModelAndView showForm(ServletRequest request, @PathVariable String nick) throws Exception {

    if (!isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    if(!getNick().equals(nick)) {
      throw new AccessViolationException("Not authorized");
    }

    ModelAndView result = new ModelAndView("edit-profile");
    result.addObject("styleList", DefaultProfile.getStyleList());
    result.addObject("avatarList", DefaultProfile.getAvatars());

    return result;
  }

  @RequestMapping(method=RequestMethod.POST)
  public ModelAndView editProfile(
          ServletRequest request,
          @RequestParam("tags") int tags,
          @RequestParam("topics") int topics,
          @RequestParam("messages") int messages,
          @PathVariable String nick
  ) throws Exception {

    if (!isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    if(!getNick().equals(nick)) {
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

    getProf().setTopics(topics);
    getProf().setMessages(messages);
    getProf().setTags(tags);
    getProf().setShowNewFirst("on".equals(request.getParameter("newfirst")));
    getProf().setShowPhotos("on".equals(request.getParameter("photos")));
    getProf().setHideAdsense("on".equals(request.getParameter("hideAdsense")));
    getProf().setShowGalleryOnMain("on".equals(request.getParameter("mainGallery")));
    getProf().setFormatMode(request.getParameter("format_mode"));
    getProf().setStyle(request.getParameter("style")); // TODO убрать как только
    userDao.setStyle(getCurrentUser(), request.getParameter("style"));
    
    getProf().setShowSocial("on".equals(request.getParameter("showSocial")));

    String avatar = request.getParameter("avatar");

    if (!DefaultProfile.getAvatars().contains(avatar)) {
      throw new BadInputException("invalid avatar value");
    }

    getProf().setAvatarMode(avatar);

    getProf().setThreeColumnsOnMain("on".equals(request.getParameter("3column")));

    getProf().setShowAnonymous("on".equals(request.getParameter("showanonymous")));
    getProf().setUseHover("on".equals(request.getParameter("hover")));

    new FileProfileReader(configuration).writeProfile(nick, getCurrentProfile());

    return new ModelAndView(new RedirectView("/people/" + nick + "/profile"));
  }
}
