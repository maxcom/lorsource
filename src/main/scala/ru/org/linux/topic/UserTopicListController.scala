/*
 * Copyright 1998-2022 Linux.org.ru
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

package ru.org.linux.topic

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.util.UriComponentsBuilder
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.section.SectionService
import ru.org.linux.site.Template
import ru.org.linux.user._

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import scala.jdk.CollectionConverters._

@Controller
@RequestMapping(Array("/people/{nick}"))
class UserTopicListController(topicListService: TopicListService, userDao: UserDao, userService: UserService,
                              sectionService: SectionService, prepareService: TopicPrepareService,
                              topicPermissionService: TopicPermissionService) {
  @RequestMapping(value = Array("favs"), params = Array("!output"))
  def showUserFavs(
    request: HttpServletRequest,
    @PathVariable nick: String,
    @RequestParam(value = "offset", defaultValue = "0") rawOffset: Int
  ): ModelAndView = {
    val (modelAndView, user) = mkModel(nick)

    modelAndView.addObject("url",
      UriComponentsBuilder.fromUriString("/people/{nick}/favs").buildAndExpand(nick).encode.toUriString)

    modelAndView.addObject("ptitle", s"Избранные сообщения ${user.getNick}")
    modelAndView.addObject("navtitle", s"Избранные сообщения")

    val offset = topicListService.fixOffset(rawOffset)
    modelAndView.addObject("offset", offset)
    val messages = topicListService.getUserTopicsFeed(user, offset, true, false)
    prepareTopicsForPlainOrRss(request, modelAndView, rss = false, messages)
    modelAndView.setViewName("user-topics")

    modelAndView
  }

  @RequestMapping(value = Array("drafts"))
  def showUserDrafts(
    request: HttpServletRequest,
    @PathVariable nick: String,
    @RequestParam(value = "offset", defaultValue = "0") rawOffset: Int
  ): ModelAndView = {
    val tmpl = Template.getTemplate(request)
    val (modelAndView, user) = mkModel(nick)

    if (!tmpl.isModeratorSession && !(user == tmpl.getCurrentUser)) {
      throw new AccessViolationException("Вы не можете смотреть черновики другого пользователя")
    }

    modelAndView.addObject("url",
      UriComponentsBuilder.fromUriString("/people/{nick}/drafts").buildAndExpand(nick).encode.toUriString)

    modelAndView.addObject("ptitle", s"Черновики ${user.getNick}")
    modelAndView.addObject("navtitle", s"Черновики")
    val offset = topicListService.fixOffset(rawOffset)
    modelAndView.addObject("offset", offset)
    val messages = topicListService.getDrafts(user, offset)
    prepareTopicsForPlainOrRss(request, modelAndView, rss = false, messages)
    modelAndView.setViewName("user-topics")

    modelAndView
  }

  @RequestMapping
  def showUserTopics(
    request: HttpServletRequest,
    @PathVariable nick: String,
    response: HttpServletResponse,
    @RequestParam(value = "offset", defaultValue = "0") rawOffset: Int,
    @RequestParam(value = "section", defaultValue = "0") sectionId: Int,
    @RequestParam(value = "output", required = false) output: String
  ): ModelAndView = {
    TopicListController.setExpireHeaders(response, null, null)

    val (modelAndView, user) = mkModel(nick)

    val section = if (sectionId != 0) {
      sectionService.idToSection.get(sectionId)
    } else {
      None
    }

    val userInfo = userDao.getUserInfoClass(user)

    if (topicPermissionService.followAuthorLinks(user)) {
      modelAndView.addObject("meLink", userInfo.getUrl)
    }

    modelAndView.addObject("nick", user.getNick)
    modelAndView.addObject("url",
      UriComponentsBuilder.fromUriString("/people/{nick}/").buildAndExpand(nick).encode.toUriString)
    modelAndView.addObject("ptitle", s"Сообщения ${user.getNick}")
    modelAndView.addObject("navtitle", s"Сообщения")
    modelAndView.addObject("rssLink",
      UriComponentsBuilder.fromUriString("/people/{nick}/?output=rss").buildAndExpand(nick).encode.toUriString)

    val offset = topicListService.fixOffset(rawOffset)
    modelAndView.addObject("offset", offset)
    val messages = topicListService.getUserTopicsFeed(user, section.orNull, null, offset, false, false)

    val rss = "rss" == output
    if (!rss) {
      section.foreach { section => modelAndView.addObject("section", section)}
      modelAndView.addObject("sectionList", sectionService.getSectionList)
    }

    modelAndView.addObject("params", section.map(s => s"section=${s.getId}").getOrElse(""))

    prepareTopicsForPlainOrRss(request, modelAndView, rss, messages)

    if (!rss) {
      modelAndView.setViewName("user-topics")
    }

    modelAndView.addObject("showSearch", true)

    modelAndView
  }

  @RequestMapping(value = Array("deleted-topics"), method = Array(RequestMethod.GET))
  def showDeletedTopics(request: HttpServletRequest, @PathVariable nick: String): ModelAndView = {
    val tmpl = Template.getTemplate(request)

    val user = userService.getUserCached(nick)

    if (!tmpl.isModeratorSession && !(user == tmpl.getCurrentUser)) {
      throw new AccessViolationException("Вы не можете смотреть удаленные темы другого пользователя")
    }

    val topics = topicListService.getDeletedUserTopics(user, tmpl.getProf.getTopics)

    val params = Map(
      "topics" -> topics,
      "user" -> user
    )

    new ModelAndView("deleted-topics", params.asJava)
  }

  @RequestMapping(value = Array("tracked"), params = Array("!output"))
  def showUserWatches(
    request: HttpServletRequest,
    @PathVariable nick: String,
    @RequestParam(value = "offset", defaultValue = "0") rawOffset: Int
  ): ModelAndView = {
    val tmpl = Template.getTemplate(request)

    val (modelAndView, user) = mkModel(nick)

    if (!tmpl.isModeratorSession && !(user == tmpl.getCurrentUser)) {
      throw new AccessViolationException("Вы не можете смотреть отслеживаемые темы другого пользователя")
    }

    modelAndView.addObject("url",
      UriComponentsBuilder.fromUriString("/people/{nick}/tracked").buildAndExpand(nick).encode.toUriString)

    modelAndView.addObject("ptitle", s"Отслеживаемые сообщения ${user.getNick}")
    modelAndView.addObject("navtitle", s"Отслеживаемые сообщения")

    val offset = topicListService.fixOffset(rawOffset)
    modelAndView.addObject("offset", offset)

    val messages = topicListService.getUserTopicsFeed(user, offset, true, true)
    prepareTopicsForPlainOrRss(request, modelAndView, rss = false, messages)
    modelAndView.setViewName("user-topics")

    modelAndView
  }

  private def prepareTopicsForPlainOrRss(
    request: HttpServletRequest,
    modelAndView: ModelAndView,
    rss: Boolean,
    messages: java.util.List[Topic]
  ): Unit = {
    if (rss) {
      modelAndView.addObject("messages", prepareService.prepareTopics(messages.asScala.toSeq).asJava)
      modelAndView.setViewName("section-rss")
    } else {
      val tmpl = Template.getTemplate(request)
      modelAndView.addObject("messages",
        prepareService.prepareTopicsForUser(messages, tmpl.getCurrentUser, tmpl.getProf, loadUserpics = false))
    }
  }

  private def mkModel(nick: String): (ModelAndView, User) = {
    val modelAndView = new ModelAndView()

    val user = userService.getUserCached(nick)

    if (user.getId == User.ANONYMOUS_ID) {
      throw new UserErrorException("Лента для пользователя anonymous не доступна")
    }

    modelAndView.addObject("user", user)
    modelAndView.addObject("whoisLink",
      UriComponentsBuilder.fromUriString("/people/{nick}/profile").buildAndExpand(nick).encode.toUriString)

    (modelAndView, user)
  }

  @ExceptionHandler(Array(classOf[UserNotFoundException]))
  @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleNotFoundException = new ModelAndView("errors/code404")
}
