/*
 * Copyright 1998-2023 Linux.org.ru
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

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestMethod, RequestParam}
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.markup.MarkupPermissions
import ru.org.linux.site.{BadInputException, DefaultProfile, Template, Theme}
import ru.org.linux.tracker.TrackerFilterEnum

import java.util
import javax.servlet.ServletRequest
import scala.jdk.CollectionConverters.*

@Controller
@RequestMapping (path = Array ("/people/{nick}/settings") )
class EditSettingsController(userDao: UserDao, profileDao: ProfileDao, userService: UserService) {
  @RequestMapping(method = Array(RequestMethod.GET))
  def showForm(@PathVariable nick: String): ModelAndView = AuthorizedOnly { currentUser =>
    val tmpl = Template.getTemplate
    if (!(currentUser.user.getNick == nick)) {
      throw new AccessViolationException("Not authorized")
    }

    val params = new util.HashMap[String, AnyRef]

    val nonDeprecatedThemes = Theme.THEMES.asScala.toVector.filterNot(_.isDeprecated).map(_.getId)

    if (DefaultProfile.getTheme(currentUser.user.getStyle).isDeprecated) {
      params.put("stylesList", (nonDeprecatedThemes :+ currentUser.user.getStyle).asJava)
    } else {
      params.put("stylesList", nonDeprecatedThemes.asJava)
    }

    params.put("trackerModes", TrackerFilterEnum.values.filter(_.isCanBeDefault))

    params.put("topicsValues", (DefaultProfile.TOPICS_VALUES.asScala + tmpl.getProf.getTopics).toSeq.sorted.asJava)
    params.put("messagesValues", (DefaultProfile.COMMENTS_VALUES.asScala + tmpl.getProf.getMessages).toSeq.sorted.asJava)

    params.put("format_mode", tmpl.getFormatMode)

    params.put("formatModes",
      MarkupPermissions.allowedFormats(currentUser.user).map(m => m.formId -> m.title).toMap.asJava)

    params.put("avatarsList", DefaultProfile.getAvatars)

    params.put("canLoadUserpic", Boolean.box(userService.canLoadUserpic(currentUser.user)))

    new ModelAndView("edit-profile", params)
  }

  @RequestMapping(method = Array(RequestMethod.POST))
  def updateSettings(request: ServletRequest, @RequestParam("topics") topics: Int,
                     @RequestParam("messages") messages: Int,
                     @RequestParam("format_mode") formatMode: String,
                     @PathVariable nick: String
                 ): ModelAndView = AuthorizedOnly { currentUser =>
    val tmpl = Template.getTemplate
    if (!(currentUser.user.getNick == nick)) {
      throw new AccessViolationException("Not authorized")
    }
    if (!(DefaultProfile.TOPICS_VALUES.contains(topics) || topics == tmpl.getProf.getTopics)) {
      throw new BadInputException("некорректное число тем")
    }
    if (!(DefaultProfile.COMMENTS_VALUES.contains(messages) || messages == tmpl.getProf.getMessages)) {
      throw new BadInputException("некорректное число комментариев")
    }
    if (!DefaultProfile.isStyle(request.getParameter("style"))) {
      throw new BadInputException("неправльное название темы")
    }

    if (!MarkupPermissions.allowedFormats(currentUser.user).map(_.formId).contains(formatMode)) {
      throw new BadInputException("некорректный режим форматирования")
    }

    tmpl.getProf.setTopics(topics)
    tmpl.getProf.setMessages(messages)
    tmpl.getProf.setShowPhotos("on" == request.getParameter("photos"))
    tmpl.getProf.setHideAdsense("on" == request.getParameter("hideAdsense"))
    tmpl.getProf.setShowGalleryOnMain("on" == request.getParameter("mainGallery"))
    tmpl.getProf.setFormatMode(formatMode)
    tmpl.getProf.setStyle(request.getParameter("style"))
    userDao.setStyle(currentUser.user, request.getParameter("style"))
    tmpl.getProf.setOldTracker("on" == request.getParameter("oldTracker"))
    tmpl.getProf.setTrackerMode(TrackerFilterEnum.getByValue(request.getParameter("trackerMode"), tmpl.isModeratorSession).orElse(DefaultProfile.DEFAULT_TRACKER_MODE))

    val avatar = request.getParameter("avatar")
    if (!DefaultProfile.getAvatars.contains(avatar)) {
      throw new BadInputException("invalid avatar value")
    }

    tmpl.getProf.setAvatarMode(avatar)
    tmpl.getProf.setReactionNotification("on" == request.getParameter("reactionNotification"))

    profileDao.writeProfile(currentUser.user, tmpl.getProf)

    new ModelAndView(new RedirectView("/people/" + nick + "/profile"))
  }
}
