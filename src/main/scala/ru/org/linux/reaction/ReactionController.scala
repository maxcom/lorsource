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
package ru.org.linux.reaction

import io.circe.Json
import io.circe.syntax.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AuthUtil.{AuthorizedOnly, AuthorizedOpt}
import ru.org.linux.auth.{AccessViolationException, CurrentUser}
import ru.org.linux.comment.{Comment, CommentDao, CommentPrepareService}
import ru.org.linux.group.GroupDao
import ru.org.linux.reaction.ReactionController.ReactionsLimit
import ru.org.linux.site.Template
import ru.org.linux.topic.{Topic, TopicDao, TopicPermissionService, TopicPrepareService}
import ru.org.linux.user.{IgnoreListDao, UserService}

import scala.jdk.CollectionConverters.*

object ReactionController {
  def ReactionsLimit = 5 // per 10 minutes
}

@Controller
@RequestMapping(path = Array("/reactions"))
class ReactionController(topicDao: TopicDao, commentDao: CommentDao, permissionService: TopicPermissionService,
                         groupDao: GroupDao, userService: UserService, commentPrepareService: CommentPrepareService,
                         ignoreListDao: IgnoreListDao, topicPrepareService: TopicPrepareService,
                         reactionService: ReactionService, reactionsDao: ReactionDao) {
  @RequestMapping(params = Array("comment"), method = Array(RequestMethod.GET))
  def commentReaction(@RequestParam("comment") commentId: Int): ModelAndView = AuthorizedOpt { currentUserOpt =>
    val comment = commentDao.getById(commentId)
    val topic = topicDao.getById(comment.topicId)

    currentUserOpt match {
      case None =>
        new ModelAndView(new RedirectView(topic.getLink + "?cid=" + comment.id))
      case Some(currentUser) =>
        if (comment.deleted || topic.deleted || topic.postscore == TopicPermissionService.POSTSCORE_HIDE_COMMENTS) {
          throw new AccessViolationException("Сообщение не доступно")
        }

        val ignoreList = ignoreListDao.get(currentUser.user.getId)
        val reactionLog = reactionsDao.getLogByComment(comment)

        val tmpl = Template.getTemplate

        new ModelAndView("reaction-comment", Map[String, Any](
          "topic" -> topic,
          "preparedComment" ->
            commentPrepareService.prepareCommentOnly(comment, currentUser.user, tmpl.getProf, topic,
              ignoreList.map(Integer.valueOf).asJava),
          "reactionList" -> reactionService.prepareReactionList(comment.reactions, reactionLog, ignoreList)
        ).asJava)
    }
  }

  private def doSetCommentReaction(topic: Topic, comment: Comment, reactionAction: String,
                                   currentUser: CurrentUser): Int = {
    val Array(reaction, action) = reactionAction.split("-", 2)

    if (!reactionService.allowInteract(Some(currentUser.user), topic, Some(comment))) {
      throw new AccessViolationException("Сообщение не доступно")
    }

    if (reactionsDao.recentReactionCount(currentUser.user) >= ReactionsLimit) {
      throw new ReactionRateLimitException
    }

    if (!ReactionService.AllowedReactions.contains(reaction)) {
      throw new AccessViolationException("unsupported reaction")
    }

    reactionService.setCommentReaction(topic, comment, currentUser.user, reaction, action == "true")
  }

  @RequestMapping(params = Array("comment"), method = Array(RequestMethod.POST))
  def setCommentReaction(@RequestParam("comment") commentId: Int,
                         @RequestParam("reaction") reactionAction: String): ModelAndView = AuthorizedOnly { currentUser =>
    val comment = commentDao.getById(commentId)
    val topic = topicDao.getById(comment.topicId)

    doSetCommentReaction(topic, comment, reactionAction, currentUser)

    new ModelAndView(new RedirectView(topic.getLink + "?cid=" + comment.id))
  }

  @RequestMapping(value=Array("/ajax"), params = Array("comment"), method = Array(RequestMethod.POST))
  @ResponseBody
  def setCommentReactionAjax(@RequestParam("comment") commentId: Int,
                             @RequestParam("reaction") reactionAction: String): Json = AuthorizedOnly { currentUser =>
    val comment = commentDao.getById(commentId)
    val topic = topicDao.getById(comment.topicId)

    val count = doSetCommentReaction(topic, comment, reactionAction, currentUser)

    Map("count" -> Integer.valueOf(count)).asJson
  }


  @RequestMapping(params = Array("!comment"), method = Array(RequestMethod.GET))
  def topicReaction(@RequestParam("topic") topicId: Int): ModelAndView = AuthorizedOpt { currentUserOpt =>
    val topic = topicDao.getById(topicId)

    currentUserOpt match {
      case None =>
        new ModelAndView(new RedirectView(topic.getLink))
      case Some(currentUser) =>
        val group = groupDao.getGroup(topic.groupId)
        val topicAuthor = userService.getUserCached(topic.authorUserId)

        permissionService.checkView(group, topic, currentUser.user, topicAuthor, false)

        if (topic.deleted) {
          throw new AccessViolationException("Сообщение не доступно")
        }

        val ignoreList = ignoreListDao.get(currentUser.user.getId)
        val reactionLog = reactionsDao.getLogByTopic(topic)

        new ModelAndView("reaction-topic", Map(
          "topic" -> topic,
          "preparedTopic" -> topicPrepareService.prepareTopic(topic, currentUser.user),
          "reactionList" -> reactionService.prepareReactionList(topic.reactions, reactionLog, ignoreList)
        ).asJava)
    }
  }

  private def doSetTopicReaction(topic: Topic, reactionAction: String, currentUser: CurrentUser): Int = {
    val Array(reaction, action) = reactionAction.split("-", 2)

    if (!reactionService.allowInteract(Some(currentUser.user), topic, None)) {
      throw new AccessViolationException("Сообщение не доступно")
    }

    if (reactionsDao.recentReactionCount(currentUser.user) >= ReactionsLimit) {
      throw new ReactionRateLimitException
    }

    if (!ReactionService.AllowedReactions.contains(reaction)) {
      throw new AccessViolationException("unsupported reaction")
    }

    reactionService.setTopicReaction(topic, currentUser.user, reaction, action == "true")
  }

  @RequestMapping(params = Array("!comment"), method = Array(RequestMethod.POST))
  def setTopicReaction(@RequestParam("topic") topicId: Int,
                       @RequestParam("reaction") reactionAction: String): ModelAndView = AuthorizedOnly { currentUser =>
    val topic = topicDao.getById(topicId)

    doSetTopicReaction(topic, reactionAction, currentUser)

    new ModelAndView(new RedirectView(topic.getLink))
  }

  @RequestMapping(value = Array("/ajax"), params = Array("!comment"), method = Array(RequestMethod.POST))
  @ResponseBody
  def setTopicReactionAjax(@RequestParam("topic") topicId: Int,
                           @RequestParam("reaction") reactionAction: String): Json = AuthorizedOnly { currentUser =>
    val topic = topicDao.getById(topicId)

    val count = doSetTopicReaction(topic, reactionAction, currentUser)

    Map("count" -> Integer.valueOf(count)).asJson
  }

  @ExceptionHandler(Array(classOf[ReactionRateLimitException]))
  @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
  def handleRateLimit = new ModelAndView(
    "errors/good-penguin",
    Map("msgHeader" -> "Попробуйте позже").asJava)
}

class ReactionRateLimitException extends RuntimeException


