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

package ru.org.linux.user

import jakarta.servlet.ServletRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestMethod, RequestParam}
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.site.{BadInputException, DefaultProfile, Theme}
import ru.org.linux.tracker.TrackerFilterEnum

import java.util
import scala.jdk.CollectionConverters.*

@Controller
@RequestMapping (path = Array ("/people/{nick}/settings") )
class EditSettingsController(userDao: UserDao, profileDao: ProfileDao, userPermissionService: UserPermissionService) {
  @RequestMapping(method = Array(RequestMethod.GET))
  def showForm(@PathVariable nick: String): ModelAndView = AuthorizedOnly { implicit currentUser =>
    if (!(currentUser.user.nick == nick)) {
      throw new AccessViolationException("Not authorized")
    }

    val params = new util.HashMap[String, AnyRef]

    val nonDeprecatedThemes = Theme.THEMES.asScala.view.filterNot(_.isDeprecated).map(_.getId).toVector

    if (currentUser.user.score >= 500) {
      params.put("stylesList", Theme.THEMES.asScala.map(_.getId).asJava)
    } else if (DefaultProfile.getTheme(currentUser.user.style).isDeprecated) {
      params.put("stylesList", (nonDeprecatedThemes :+ currentUser.user.style).asJava)
    } else {
      params.put("stylesList", nonDeprecatedThemes.asJava)
    }

    params.put("trackerModes", TrackerFilterEnum.values.filter(_.isCanBeDefault))

    params.put("topicsValues", (DefaultProfile.TOPICS_VALUES.asScala + currentUser.profile.topics).toSeq.sorted.asJava)
    params.put("messagesValues", (DefaultProfile.COMMENTS_VALUES.asScala + currentUser.profile.messages).toSeq.sorted.asJava)

    params.put("format_mode", currentUser.profile.formatMode.formId)

    params.put("formatModes",
      UserPermissionService.allowedFormats(currentUser.user).map(m => m.formId -> m.title).toMap.asJava)

    params.put("avatarsList", DefaultProfile.getAvatars)

    params.put("canLoadUserpic", Boolean.box(userPermissionService.canLoadUserpic))

    new ModelAndView("edit-profile", params)
  }

  @RequestMapping(method = Array(RequestMethod.POST))
  def updateSettings(request: ServletRequest, @RequestParam("topics") topics: Int,
                     @RequestParam("messages") messages: Int,
                     @RequestParam("format_mode") formatMode: String,
                     @PathVariable nick: String
                 ): ModelAndView = AuthorizedOnly { currentUser =>
    if (!(currentUser.user.nick == nick)) {
      throw new AccessViolationException("Not authorized")
    }
    if (!(DefaultProfile.TOPICS_VALUES.contains(topics) || topics == currentUser.profile.topics)) {
      throw new BadInputException("некорректное число тем")
    }
    if (!(DefaultProfile.COMMENTS_VALUES.contains(messages) || messages == currentUser.profile.messages)) {
      throw new BadInputException("некорректное число комментариев")
    }
    if (!DefaultProfile.isStyle(request.getParameter("style"))) {
      throw new BadInputException("неправльное название темы")
    }

    if (!UserPermissionService.allowedFormats(currentUser.user).map(_.formId).contains(formatMode)) {
      throw new BadInputException("некорректный режим форматирования")
    }

    val builder = new ProfileBuilder(currentUser.profile)

    builder.setTopics(topics)
    builder.setMessages(messages)
    builder.setShowPhotos("on" == request.getParameter("photos"))
    builder.setHideAdsense("on" == request.getParameter("hideAdsense"))
    builder.setShowGalleryOnMain("on" == request.getParameter("mainGallery"))
    builder.setFormatMode(formatMode)
    builder.setStyle(request.getParameter("style"))
    userDao.setStyle(currentUser.user, request.getParameter("style"))
    builder.setOldTracker("on" == request.getParameter("oldTracker"))
    builder.setTrackerMode(TrackerFilterEnum.getByValue(request.getParameter("trackerMode")).orElse(DefaultProfile.DEFAULT_TRACKER_MODE))

    val avatar = request.getParameter("avatar")
    if (!DefaultProfile.getAvatars.contains(avatar)) {
      throw new BadInputException("invalid avatar value")
    }

    builder.setAvatarMode(avatar)
    builder.setReactionNotification("on" == request.getParameter("reactionNotification"))

    profileDao.writeProfile(currentUser.user, builder)

    new ModelAndView(new RedirectView("/people/" + nick + "/profile"))
  }
}
