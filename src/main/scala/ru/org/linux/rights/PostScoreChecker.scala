package ru.org.linux.rights

import org.springframework.stereotype.Service
import ru.org.linux.group.{Group, GroupService}
import ru.org.linux.msgbase.DeleteInfoDao
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.topic.{Topic, TopicPermissionService}
import ru.org.linux.topic.TopicPermissionService.*
import ru.org.linux.user.User
import ru.org.linux.warning.WarningService.TopicMaxWarnings

@Service
class PostScoreChecker(sectionService: SectionService, groupService: GroupService, deleteInfoDao: DeleteInfoDao):
  def postScoreCheckerChain(user: User, postscore: Int, byAuthor: Boolean = false): RestrictionChain =
    val byAuthorNotAnonymous = byAuthor && !user.anonymous

    postscore match
      case POSTSCORE_UNRESTRICTED =>
        Unrestricted
      case POSTSCORE_MODERATORS_ONLY =>
        Unrestricted.restrict(!user.isModerator, "только для модераторов")
      case POSTSCORE_REGISTERED_ONLY =>
        Unrestricted.restrict(user.anonymous, "только для зарегистрированных")
      case POSTSCORE_NO_COMMENTS | POSTSCORE_HIDE_COMMENTS =>
        Restricted("постинг запрещен")
      case POSTSCORE_MOD_AUTHOR =>
        Unrestricted.restrict(!byAuthorNotAnonymous && !user.isModerator, "только для модераторов и автора")
      case 100 | 200 | 300 | 400 =>
        Unrestricted.restrict(
          !user.isModerator && !byAuthorNotAnonymous && (user.anonymous || user.score < postscore),
          s"только для зарегистрированных, минимум ${User.getStars(postscore, postscore, false)}"
        )
      case 500 =>
        Unrestricted.restrict(
          !user.isModerator && !byAuthorNotAnonymous && (user.anonymous || user.score < postscore),
          s"только для зарегистрированных, ${User.getStars(postscore, postscore, false)}"
        )
      case _ =>
        Unrestricted.restrict(
          !user.isModerator && !byAuthorNotAnonymous && (user.anonymous || (user.score < postscore)),
          s"только для зарегистрированных, score>=$postscore")

  def topicRestrictionScore(section: Section): Int = section.topicsRestriction

  def topicRestrictionScore(group: Group): Int =
    val section = sectionService.getSection(group.sectionId)
    Math.max(group.topicRestriction, section.topicsRestriction)

  def topicRestrictionAnywhere: Int = sectionService.sections.view.map(_.topicsRestriction).min

  def commentRestrictionScore(group: Group, topic: Topic): Int =
    Seq(
      topic.postscore,
      group.commentsRestriction,
      Section.getCommentPostscore(topic.sectionId),
      getCommentCountRestriction(topic),
      getAllowAnonymousPostscore(topic),
      getScoreLossPostscore(topic),
      getOpenWarningsPostscore(topic)
    ).max

  def commentRestrictionScore(topic: Topic): Int =
    val group = groupService.getGroup(topic.groupId)

    commentRestrictionScore(group, topic)

  private def getCommentCountRestriction(topic: Topic) =
    if !topic.sticky then
      val commentCount = topic.commentCount

      if commentCount > 3000 then
        200
      else if commentCount > 2000 then
        100
      else if commentCount > 1000 then
        50
      else
        POSTSCORE_UNRESTRICTED
    else
      POSTSCORE_UNRESTRICTED

  private def getAllowAnonymousPostscore(topic: Topic) =
    if topic.allowAnonymous then
      TopicPermissionService.POSTSCORE_UNRESTRICTED
    else
      TopicPermissionService.POSTSCORE_REGISTERED_ONLY

  private def getScoreLossPostscore(topic: Topic): Int =
    if !topic.sticky && !topic.expired then
      val scoreLoss = deleteInfoDao.scoreLoss(topic.id)

      if scoreLoss >= 150 then
        100
      else if scoreLoss >= 100 then
        50
      else
        POSTSCORE_UNRESTRICTED
    else
      POSTSCORE_UNRESTRICTED

  private def getOpenWarningsPostscore(topic: Topic): Int =
    if topic.openWarnings > TopicMaxWarnings then
      100
    else
      POSTSCORE_UNRESTRICTED

object PostScoreChecker:
  def getPostScoreInfo(postscore: Int): String =
    postscore match
      case POSTSCORE_UNRESTRICTED =>
        ""
      case 50 =>
        "Закрыто добавление комментариев для недавно зарегистрированных пользователей (со score < 50)"
      case 100 | 200 | 300 | 400 | 500 =>
        "<b>Ограничение на отправку комментариев</b>: " + User.getStars(postscore, postscore, true)
      case POSTSCORE_MOD_AUTHOR =>
        "<b>Ограничение на отправку комментариев</b>: только для модераторов и автора"
      case POSTSCORE_MODERATORS_ONLY =>
        "<b>Ограничение на отправку комментариев</b>: только для модераторов"
      case POSTSCORE_NO_COMMENTS =>
        "<b>Ограничение на отправку комментариев</b>: комментарии запрещены"
      case POSTSCORE_HIDE_COMMENTS =>
        "<b>Ограничение на отправку комментариев</b>: без комментариев"
      case POSTSCORE_REGISTERED_ONLY =>
        "<b>Ограничение на отправку комментариев</b>: только для зарегистрированных пользователей"
      case _ =>
        "<b>Ограничение на отправку комментариев</b>: только для зарегистрированных пользователей, score>=" + postscore
