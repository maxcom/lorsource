/*
 * Copyright 1998-2024 Linux.org.ru
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
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, RequestMethod, RequestParam}
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.auth.{AccessViolationException, AuthorizedSession}
import ru.org.linux.comment.DeleteService
import ru.org.linux.common.DeleteReasons
import ru.org.linux.group.GroupPermissionService
import ru.org.linux.search.SearchQueueSender
import ru.org.linux.section.SectionService
import ru.org.linux.site.BadParameterException
import ru.org.linux.user.{UserErrorException, UserService}

import java.util
import scala.jdk.CollectionConverters.*

@Controller
class DeleteTopicController(searchQueueSender: SearchQueueSender, sectionService: SectionService,
                            topicDao: TopicDao, deleteService: DeleteService,
                            prepareService: TopicPrepareService,
                            permissionService: GroupPermissionService,
                            userService: UserService) extends StrictLogging {
  private def checkUndeletable(topic: Topic)(implicit currentUser: AuthorizedSession): Unit = {
    if (!permissionService.isUndeletable(topic)) {
      throw new AccessViolationException("это сообщение нельзя восстановить")
    }
  }

  @ModelAttribute("deleteReasons")
  def deleteReasons: util.List[String] = DeleteReasons.DeleteReasons.asJava

  @RequestMapping(value = Array("/delete.jsp"), method = Array(RequestMethod.GET))
  def showForm(@RequestParam("msgid") msgid: Int): ModelAndView = AuthorizedOnly { implicit currentUser =>
    val topic = topicDao.getById(msgid)

    if (topic.deleted) {
      throw new UserErrorException("Сообщение уже удалено")
    }

    if (!permissionService.isDeletable(topic)) {
      throw new AccessViolationException("Вы не можете удалить это сообщение")
    }

    val section = sectionService.getSection(topic.sectionId)

    new ModelAndView("delete", Map[String, Any](
      "bonus" -> (!section.isPremoderated && !topic.draft && !topic.expired),
      "author" -> userService.getUserCached(topic.authorUserId),
      "msgid" -> msgid,
      "draft" -> topic.draft,
      "uncommited" -> (section.isPremoderated && !topic.commited)
    ).asJava)
  }

  @RequestMapping(value = Array("/delete.jsp"), method = Array(RequestMethod.POST))
  def deleteMessage(@RequestParam("msgid") msgid: Int, @RequestParam("reason") reason: String,
                    @RequestParam(value = "bonus", defaultValue = "0") bonus: Int): ModelAndView = AuthorizedOnly { implicit currentUser =>
    if (bonus < 0 || bonus > 20) {
      throw new BadParameterException("неправильный размер штрафа")
    }

    val topic = topicDao.getById(msgid)
    if (topic.deleted) {
      throw new UserErrorException("Сообщение уже удалено")
    }

    if (!permissionService.isDeletable(topic)) {
      throw new AccessViolationException("Вы не можете удалить это сообщение")
    }

    val effectiveBonus = if (currentUser.moderator && currentUser.user.getId != topic.authorUserId && !topic.draft) {
      bonus
    } else {
      0
    }

    deleteService.deleteTopic(topic, reason, effectiveBonus)

    logger.info(s"Удалено сообщение $msgid пользователем ${currentUser.user.getNick} по причине `$reason'")

    searchQueueSender.updateMessage(msgid, true)

    new ModelAndView("action-done", "message", "Сообщение удалено")
  }

  @RequestMapping(value = Array("/undelete"), method = Array(RequestMethod.GET))
  def undeleteForm(@RequestParam msgid: Int): ModelAndView = AuthorizedOnly { implicit currentUser =>
    val topic = topicDao.getById(msgid)
    checkUndeletable(topic)

    new ModelAndView("undelete", Map(
      "message" -> topic,
      "preparedMessage" -> prepareService.prepareTopic(topic)
    ).asJava)
  }

  @RequestMapping(value = Array("/undelete"), method = Array(RequestMethod.POST))
  def undelete(@RequestParam msgid: Int): ModelAndView = AuthorizedOnly { implicit currentUser =>
    val topic = topicDao.getById(msgid)
    checkUndeletable(topic)

    if (topic.deleted) {
      deleteService.undeleteTopic(topic)

      logger.info(s"Восстановлено сообщение $msgid пользователем ${currentUser.user.getNick}")

      searchQueueSender.updateMessage(msgid, true)
    }

    new ModelAndView("action-done",
      Map("message" -> "Сообщение восстановлено", "link" -> topic.getLink).asJava)
  }
}
