/*
 * Copyright 1998-201 Linux.org.ru
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

package ru.org.linux.user

import java.util
import javax.servlet.ServletRequest

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestMethod, RequestParam}
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.site.{BadInputException, DefaultProfile, Template}
import ru.org.linux.tracker.TrackerFilterEnum
import ru.org.linux.util.Theme

import scala.collection.JavaConverters._

@Controller
@RequestMapping (Array ("/people/{nick}/settings") )
class EditProfileController(
  userDao:UserDao,
  profileDao:ProfileDao
) {
  @RequestMapping(method = Array(RequestMethod.GET))
  def showForm(request: ServletRequest, @PathVariable nick: String): ModelAndView = {
    val tmpl = Template.getTemplate(request)
    if (!tmpl.isSessionAuthorized) {
      throw new AccessViolationException("Not authorized")
    }

    if (!(tmpl.getNick == nick)) {
      throw new AccessViolationException("Not authorized")
    }

    val params = new util.HashMap[String, AnyRef]

    val nonDeprecatedThemes = Theme.THEMES.asScala.toVector.filterNot(_.isDeprecated).map(_.getId)

    if (DefaultProfile.getTheme(tmpl.getCurrentUser.getStyle).isDeprecated) {
      params.put("stylesList", (nonDeprecatedThemes :+ tmpl.getCurrentUser.getStyle).asJava)
    } else {
      params.put("stylesList", nonDeprecatedThemes.asJava)
    }

    params.put("trackerModes", TrackerFilterEnum.values)

    params.put("avatarsList", DefaultProfile.getAvatars)

    new ModelAndView("edit-profile", params)
  }

  @RequestMapping(method = Array(RequestMethod.POST))
  def editProfile(
    request: ServletRequest, @RequestParam("topics") topics: Int,
    @RequestParam("messages") messages: Int,
    @PathVariable nick: String
  ): ModelAndView = {
    val tmpl: Template = Template.getTemplate(request)
    
    if (!tmpl.isSessionAuthorized) {
      throw new AccessViolationException("Not authorized")
    }
    if (!(tmpl.getNick == nick)) {
      throw new AccessViolationException("Not authorized")
    }
    if (topics < 10 || topics > 500) {
      throw new BadInputException("некорректное число тем")
    }
    if (messages < 10 || messages > 500) {
      throw new BadInputException("некорректное число сообщений")
    }
    if (!DefaultProfile.isStyle(request.getParameter("style"))) {
      throw new BadInputException("неправльное название темы")
    }
    
    tmpl.getProf.setTopics(topics)
    tmpl.getProf.setMessages(messages)
    tmpl.getProf.setShowNewFirst("on" == request.getParameter("newfirst"))
    tmpl.getProf.setShowPhotos("on" == request.getParameter("photos"))
    tmpl.getProf.setHideAdsense("on" == request.getParameter("hideAdsense"))
    tmpl.getProf.setShowGalleryOnMain("on" == request.getParameter("mainGallery"))
    tmpl.getProf.setFormatMode(request.getParameter("format_mode"))
    tmpl.getProf.setStyle(request.getParameter("style"))
    userDao.setStyle(tmpl.getCurrentUser, request.getParameter("style"))
    tmpl.getProf.setShowSocial("on" == request.getParameter("showSocial"))
    tmpl.getProf.setTrackerMode(TrackerFilterEnum.getByValue(request.getParameter("trackerMode")).or(DefaultProfile.DEFAULT_TRACKER_MODE))

    val avatar = request.getParameter("avatar")
    if (!DefaultProfile.getAvatars.contains(avatar)) {
      throw new BadInputException("invalid avatar value")
    }

    tmpl.getProf.setAvatarMode(avatar)
    tmpl.getProf.setShowAnonymous("on" == request.getParameter("showanonymous"))
    tmpl.getProf.setUseHover("on" == request.getParameter("hover"))
    profileDao.writeProfile(tmpl.getCurrentUser, tmpl.getProf)

    new ModelAndView(new RedirectView("/people/" + nick + "/profile"))
  }
}
