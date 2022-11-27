package ru.org.linux.reaction

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestAttribute, RequestMapping, RequestParam}
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.AuthorizedOpt
import ru.org.linux.comment.{CommentDao, CommentPrepareService}
import ru.org.linux.group.GroupDao
import ru.org.linux.site.Template
import ru.org.linux.topic.{TopicDao, TopicPermissionService, TopicPrepareService}
import ru.org.linux.user.{IgnoreListDao, UserService}

import scala.jdk.CollectionConverters.*

@Controller
@RequestMapping(Array("/reactions"))
class ReactionController(topicDao: TopicDao, commentDao: CommentDao, permissionService: TopicPermissionService,
                         groupDao: GroupDao, userService: UserService, commentPrepareService: CommentPrepareService,
                         ignoreListDao: IgnoreListDao, topicPrepareService: TopicPrepareService) {
  @RequestMapping(params = Array("comment"))
  def commentReaction(@RequestAttribute reactionsEnabled: Boolean, @RequestParam("topic") topicId: Int,
                      @RequestParam("comment") commentId: Int): ModelAndView = AuthorizedOpt { currentUserOpt =>
    val topic = topicDao.getById(topicId)
    val comment = commentDao.getById(commentId)

    currentUserOpt.filter(_ => reactionsEnabled) match {
      case None =>
        new ModelAndView(new RedirectView(topic.getLink + "?cid=" + comment.id))
      case Some(currentUser) =>
        if (topic.isDeleted || comment.isDeleted || topic.postscore == TopicPermissionService.POSTSCORE_HIDE_COMMENTS) {
          throw new AccessViolationException("Сообщение не доступно")
        }

        val ignoreList = ignoreListDao.getJava(currentUser.user)

        val tmpl = Template.getTemplate

        new ModelAndView("reaction-comment", Map[String, Any](
          "topic" -> topic,
          "preparedComment" ->
            commentPrepareService.prepareCommentOnly(comment, currentUser.user, tmpl.getProf, topic, ignoreList)
        ).asJava)
    }
  }

  @RequestMapping(params = Array("!comment"))
  def topicReaction(@RequestAttribute reactionsEnabled: Boolean,
                    @RequestParam("topic") topicId: Int): ModelAndView = AuthorizedOpt { currentUserOpt =>
    val topic = topicDao.getById(topicId)

    currentUserOpt.filter(_ => reactionsEnabled) match {
      case None =>
        new ModelAndView(new RedirectView(topic.getLink))
      case Some(currentUser) =>
        val group = groupDao.getGroup(topic.groupId)
        val topicAuthor = userService.getUserCached(topic.authorUserId)

        permissionService.checkView(group, topic, currentUser.user, topicAuthor, false)

        if (topic.isDeleted) {
          throw new AccessViolationException("Сообщение не доступно")
        }

        new ModelAndView("reaction-topic", Map(
          "topic" -> topic,
          "preparedTopic" -> topicPrepareService.prepareTopic(topic, currentUser.user)
        ).asJava)
    }
  }
}
