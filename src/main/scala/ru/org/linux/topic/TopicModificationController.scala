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

import com.typesafe.scalalogging.StrictLogging
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam}
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.ModeratorOnly
import ru.org.linux.group.GroupDao
import ru.org.linux.markup.MessageTextService
import ru.org.linux.search.SearchQueueSender
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.spring.dao.MsgbaseDao
import ru.org.linux.user.{UserDao, UserErrorException}

import scala.compat.java8.OptionConverters.*
import scala.jdk.CollectionConverters.*

@Controller
class TopicModificationController(prepareService: TopicPrepareService, messageDao: TopicDao,
                                  sectionService: SectionService, groupDao: GroupDao,
                                  userDao: UserDao, searchQueueSender: SearchQueueSender,
                                  msgbaseDao: MsgbaseDao, textService: MessageTextService) extends StrictLogging {

  @RequestMapping(value = Array("/setpostscore.jsp"), method = Array(RequestMethod.GET))
  def showForm(@RequestParam msgid: Int): ModelAndView = ModeratorOnly { _ =>
    val message = messageDao.getById(msgid)

    new ModelAndView("setpostscore", Map(
      "message" -> message,
      "group" -> groupDao.getGroup(message.groupId)
    ).asJava)
  }

  @RequestMapping(value = Array("/setpostscore.jsp"), method = Array(RequestMethod.POST))
  def modifyTopic(@RequestParam msgid: Int,
                  @RequestParam postscore: Int,
                  @RequestParam(defaultValue = "false") sticky: Boolean,
                  @RequestParam(defaultValue = "false") notop: Boolean): ModelAndView = ModeratorOnly { currentUser =>
    if (postscore < TopicPermissionService.POSTSCORE_UNRESTRICTED) {
      throw new UserErrorException(s"invalid postscore $postscore")
    }

    if (postscore > TopicPermissionService.POSTSCORE_UNRESTRICTED &&
      postscore < TopicPermissionService.POSTSCORE_REGISTERED_ONLY) {
      throw new UserErrorException(s"invalid postscore $postscore")
    }

    if (postscore > TopicPermissionService.POSTSCORE_HIDE_COMMENTS) {
      throw new UserErrorException(s"invalid postscore $postscore")
    }

    val topic = messageDao.getById(msgid)

    messageDao.setTopicOptions(topic, postscore, sticky, notop)

    val out = new StringBuilder
    if (topic.postscore != postscore) {
      out.append("Установлен новый уровень записи: ").append(postScoreInfoFull(postscore)).append("<br>")
      logger.info(s"Установлен новый уровень записи $postscore для $msgid пользователем ${currentUser.user.getNick}")

      searchQueueSender.updateMessage(topic.id, true)
    }

    if (topic.sticky != sticky) {
      out.append("Новое значение sticky: ").append(sticky).append("<br>")
      logger.info(s"Новое значение sticky: $sticky")
    }

    if (topic.notop != notop) {
      out.append("Новое значение notop: ").append(notop).append("<br>")
      logger.info(s"Новое значение notop: $notop")
    }

    new ModelAndView("action-done", Map (
      "message" -> "Данные изменены",
      "bigMessage" -> out.toString,
      "link" -> topic.getLink
    ).asJava)
  }

  @RequestMapping(value = Array("/mt.jsp"), method = Array(RequestMethod.POST))
  def moveTopic(@RequestParam msgid: Int,
                @RequestParam("moveto") newgr: Int): RedirectView = ModeratorOnly { currentUser =>
    val msg = messageDao.getById(msgid)
    if (msg.deleted) {
      throw new AccessViolationException("Сообщение удалено")
    }

    val newGrp = groupDao.getGroup(newgr)

    if (msg.groupId != newGrp.id) {
      val moveInfo = if (!newGrp.linksAllowed) {
        val moveFrom = msg.groupUrl
        val linktext = msg.linktext
        val url = msg.url

        val markup = msgbaseDao.getMessageText(msg.id).markup

        Some(textService.moveInfo(markup, url, linktext, currentUser.user, moveFrom))
      } else {
        None
      }

      messageDao.moveTopic(msg, newGrp, moveInfo.asJava)
      logger.info(s"topic ${msg.id} moved by ${currentUser.user.getNick} from news/forum ${msg.groupUrl} to forum ${newGrp.getTitle}")
    }

    searchQueueSender.updateMessage(msg.id, true)

    new RedirectView(TopicLinkBuilder.baseLink(msg).forceLastmod.build)
  }

  @RequestMapping(value = Array("/mt.jsp"), method = Array(RequestMethod.GET))
  def moveTopicFormForum(@RequestParam msgid: Int): ModelAndView = ModeratorOnly { _ =>
    val topic = messageDao.getById(msgid)
    val section = sectionService.getSection(Section.SECTION_FORUM)

    new ModelAndView("mtn", Map (
      "message" -> topic,
      "groups" -> groupDao.getGroups(section),
      "author" -> userDao.getUserCached(topic.authorUserId)
    ).asJava)
  }

  @RequestMapping(value = Array("/mtn.jsp"), method = Array(RequestMethod.GET))
  @throws[Exception]
  def moveTopicForm(@RequestParam msgid: Int): ModelAndView = ModeratorOnly { _ =>
    val topic = messageDao.getById(msgid)
    val section = sectionService.getSection(topic.sectionId)

    new ModelAndView("mtn", Map(
      "message" -> topic,
      "groups" -> groupDao.getGroups(section),
      "author" -> userDao.getUserCached(topic.authorUserId)
    ).asJava)
  }

  @RequestMapping(value = Array("/uncommit.jsp"), method = Array(RequestMethod.GET))
  def uncommitForm(@RequestParam msgid: Int): ModelAndView = ModeratorOnly { currentUser =>
    val message = messageDao.getById(msgid)

    checkUncommitable(message)

    new ModelAndView("uncommit", Map(
      "message" -> message,
      "preparedMessage" -> prepareService.prepareTopic(message, currentUser.user)
    ).asJava)
  }

  @RequestMapping(value = Array("/uncommit.jsp"), method = Array(RequestMethod.POST))
  def uncommit(@RequestParam msgid: Int): ModelAndView = ModeratorOnly { currentUser =>
    val topic = messageDao.getById(msgid)

    checkUncommitable(topic)

    messageDao.uncommit(topic)

    searchQueueSender.updateMessage(topic.id, true)

    logger.info(s"Отменено подтверждение сообщения $msgid пользователем ${currentUser.user.getNick}")

    new ModelAndView("action-done",
      Map("message" -> "Подтверждение отменено", "link" -> topic.getLink).asJava)
  }

  private def checkUncommitable(message: Topic): Unit = {
    if (message.expired) {
      throw new AccessViolationException("нельзя восстанавливать устаревшие сообщения")
    }
    if (message.deleted) {
      throw new AccessViolationException("сообщение удалено")
    }
    if (!message.commited) {
      throw new AccessViolationException("сообщение не подтверждено")
    }
  }

  private def postScoreInfoFull(postscore: Int): String = {
    val info = TopicPermissionService.getPostScoreInfo(postscore)

    if (info.isEmpty) {
      "без ограничений"
    } else {
      info
    }
  }
}