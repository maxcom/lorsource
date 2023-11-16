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

package ru.org.linux.topic

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.util.UriComponentsBuilder
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.{AuthorizedOnly, AuthorizedOpt}
import ru.org.linux.section.{SectionNotFoundException, SectionService}
import ru.org.linux.site.Template
import ru.org.linux.user.*

import scala.jdk.CollectionConverters.*

@Controller
@RequestMapping(path = Array("/people/{nick}"))
class UserTopicListController(topicListService: TopicListService, userDao: UserDao, userService: UserService,
                              sectionService: SectionService, prepareService: TopicPrepareService,
                              topicPermissionService: TopicPermissionService) {
  @RequestMapping(value = Array("favs"), params = Array("!output"))
  def showUserFavs(
    @PathVariable nick: String,
    @RequestParam(value = "offset", defaultValue = "0") rawOffset: Int
  ): ModelAndView = AuthorizedOpt { currentUser =>
    val (modelAndView, user) = mkModel(nick)

    modelAndView.addObject("url",
      UriComponentsBuilder.fromUriString("/people/{nick}/favs").buildAndExpand(nick).encode.toUriString)

    modelAndView.addObject("ptitle", s"Избранные сообщения ${user.getNick}")
    modelAndView.addObject("navtitle", s"Избранные сообщения")

    val offset = TopicListService.fixOffset(rawOffset)
    modelAndView.addObject("offset", offset)
    val messages = topicListService.getUserTopicsFeed(user, offset, isFavorite = true, watches = false)
    prepareTopicsForPlainOrRss(modelAndView, rss = false, messages, currentUser.map(_.user))
    modelAndView.setViewName("user-topics")

    modelAndView
  }

  @RequestMapping(value = Array("drafts"))
  def showUserDrafts(
    @PathVariable nick: String,
    @RequestParam(value = "offset", defaultValue = "0") rawOffset: Int
  ): ModelAndView = AuthorizedOnly { currentUser =>
    val (modelAndView, user) = mkModel(nick)

    if (!currentUser.moderator && !(user == currentUser.user)) {
      throw new AccessViolationException("Вы не можете смотреть черновики другого пользователя")
    }

    modelAndView.addObject("url",
      UriComponentsBuilder.fromUriString("/people/{nick}/drafts").buildAndExpand(nick).encode.toUriString)

    modelAndView.addObject("ptitle", s"Черновики ${user.getNick}")
    modelAndView.addObject("navtitle", s"Черновики")
    val offset = TopicListService.fixOffset(rawOffset)
    modelAndView.addObject("offset", offset)
    val messages = topicListService.getDrafts(user, offset)
    prepareTopicsForPlainOrRss(modelAndView, rss = false, messages, Some(currentUser.user))
    modelAndView.setViewName("user-topics")

    modelAndView
  }

  @RequestMapping
  def showUserTopics(
    @PathVariable nick: String,
    @RequestParam(value = "offset", defaultValue = "0") rawOffset: Int,
    @RequestParam(value = "section", defaultValue = "0") sectionId: Int,
    @RequestParam(value = "output", required = false) output: String
  ): ModelAndView = AuthorizedOpt { currentUser =>
    val (modelAndView, user) = mkModel(nick)

    if (user.getId == User.ANONYMOUS_ID && !currentUser.exists(_.moderator)) {
      throw new UserErrorException("Лента для пользователя anonymous не доступна")
    }

    val section = if (sectionId != 0) {
      Some(sectionService.getSection(sectionId))
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

    val offset = TopicListService.fixOffset(rawOffset)
    modelAndView.addObject("offset", offset)
    val messages = topicListService.getUserTopicsFeed(user, section, None, offset, favorites = false, watches = false)

    if (messages.nonEmpty) {
      val rss = "rss" == output
      if (!rss) {
        section.foreach { section => modelAndView.addObject("section", section) }

        val sections = topicListService.getUserSections(user)

        modelAndView.addObject("sectionList", sections.asJava)
      }

      modelAndView.addObject("params", section.map(s => s"section=${s.getId}").getOrElse(""))

      prepareTopicsForPlainOrRss(modelAndView, rss, messages, currentUser.map(_.user))

      if (!rss) {
        modelAndView.setViewName("user-topics")
      }

      modelAndView.addObject("showSearch", true)

      modelAndView
    } else {
      new ModelAndView("errors/code404")
    }
  }

  @RequestMapping(value = Array("deleted-topics"), method = Array(RequestMethod.GET))
  def showDeletedTopics(@PathVariable nick: String): ModelAndView = AuthorizedOnly { currentUser =>
    val tmpl = Template.getTemplate

    val user = userService.getUserCached(nick)

    if (!currentUser.moderator && !(user == currentUser.user)) {
      throw new AccessViolationException("Вы не можете смотреть удаленные темы другого пользователя")
    }

    val topics = topicListService.getDeletedUserTopics(user, tmpl.getProf.getTopics)

    val params = Map(
      "topics" -> topics.asJava,
      "user" -> user
    )

    new ModelAndView("deleted-topics", params.asJava)
  }

  @RequestMapping(value = Array("tracked"), params = Array("!output"))
  def showUserWatches(
    @PathVariable nick: String,
    @RequestParam(value = "offset", defaultValue = "0") rawOffset: Int
  ): ModelAndView = AuthorizedOnly { currentUser =>
    val (modelAndView, user) = mkModel(nick)

    if (!currentUser.moderator && !(user == currentUser.user)) {
      throw new AccessViolationException("Вы не можете смотреть отслеживаемые темы другого пользователя")
    }

    modelAndView.addObject("url",
      UriComponentsBuilder.fromUriString("/people/{nick}/tracked").buildAndExpand(nick).encode.toUriString)

    modelAndView.addObject("ptitle", s"Отслеживаемые сообщения ${user.getNick}")
    modelAndView.addObject("navtitle", s"Отслеживаемые сообщения")

    val offset = TopicListService.fixOffset(rawOffset)
    modelAndView.addObject("offset", offset)

    val messages = topicListService.getUserTopicsFeed(user, offset, isFavorite = true, watches = true)
    prepareTopicsForPlainOrRss(modelAndView, rss = false, messages, Some(currentUser.user))
    modelAndView.setViewName("user-topics")

    modelAndView
  }

  private def prepareTopicsForPlainOrRss(modelAndView: ModelAndView, rss: Boolean, messages: collection.Seq[Topic],
                                         currentUser: Option[User]): Unit = {
    if (rss) {
      modelAndView.addObject("messages", prepareService.prepareTopics(messages).asJava)
      modelAndView.setViewName("section-rss")
    } else {
      val tmpl = Template.getTemplate
      modelAndView.addObject("messages",
        prepareService.prepareTopicsForUser(messages, currentUser, tmpl.getProf, loadUserpics = false))
    }
  }

  private def mkModel(nick: String): (ModelAndView, User) = {
    val modelAndView = new ModelAndView()

    val user = userService.getUserCached(nick)

    modelAndView.addObject("user", user)
    modelAndView.addObject("whoisLink",
      UriComponentsBuilder.fromUriString("/people/{nick}/profile").buildAndExpand(nick).encode.toUriString)

    (modelAndView, user)
  }

  @ExceptionHandler(Array(classOf[UserNotFoundException], classOf[SectionNotFoundException]))
  @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleNotFoundException = new ModelAndView("errors/code404")
}
