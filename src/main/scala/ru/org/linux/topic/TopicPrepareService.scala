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

import org.springframework.stereotype.Service
import ru.org.linux.edithistory.EditInfoSummary
import ru.org.linux.gallery.{Image, ImageService}
import ru.org.linux.group.{GroupDao, GroupPermissionService}
import ru.org.linux.markup.MessageTextService
import ru.org.linux.poll.{Poll, PollNotFoundException, PollPrepareService, PreparedPoll}
import ru.org.linux.section.SectionService
import ru.org.linux.spring.SiteConfig
import ru.org.linux.spring.dao.{DeleteInfoDao, MessageText, MsgbaseDao, UserAgentDao}
import ru.org.linux.tag.TagRef
import ru.org.linux.user.*

import javax.annotation.Nullable
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.RichOptional

@Service
class TopicPrepareService(sectionService: SectionService, groupDao: GroupDao, deleteInfoDao: DeleteInfoDao,
                          pollPrepareService: PollPrepareService, remarkDao: RemarkDao, textService: MessageTextService,
                          siteConfig: SiteConfig, userService: UserService,
                          topicPermissionService: TopicPermissionService,
                          groupPermissionService: GroupPermissionService, topicTagService: TopicTagService,
                          msgbaseDao: MsgbaseDao, imageService: ImageService, userAgentDao: UserAgentDao) {
  def prepareTopic(message: Topic, user: User): PreparedTopic =
    prepareTopic(message, topicTagService.getTagRefs(message).asScala, minimizeCut = false, None, user,
      msgbaseDao.getMessageText(message.getId), None)

  def prepareTopic(message: Topic, tags: java.util.List[TagRef], user: User, text: MessageText): PreparedTopic =
    prepareTopic(message, tags.asScala, minimizeCut = false, None, user, text, None)

  def prepareTopicPreview(message: Topic, tags: java.util.List[TagRef], @Nullable newPoll: Poll, text: MessageText,
                          @Nullable image: Image): PreparedTopic =
    prepareTopic(message, tags.asScala, minimizeCut = false, Option(newPoll).map(pollPrepareService.preparePollPreview),
      null, text, Option(image))

  def prepareEditInfo(editInfo: EditInfoSummary): PreparedEditInfoSummary = {
    val lastEditor = userService.getUserCached(editInfo.editor).getNick
    val editCount = editInfo.editCount
    val lastEditDate = editInfo.editdate
    PreparedEditInfoSummary.apply(lastEditor, editCount, lastEditDate)
  }

  /**
   * Функция подготовки топика
   *
   * @param topic       топик
   * @param tags        список тэгов
   * @param minimizeCut сворачивать ли cut
   * @param poll        опрос к топику
   * @param currentUser        пользователь
   * @return подготовленный топик
   */
  private def prepareTopic(topic: Topic, tags: collection.Seq[TagRef], minimizeCut: Boolean, poll: Option[PreparedPoll],
                           @Nullable currentUser: User, text: MessageText, image: Option[Image]): PreparedTopic = try {
    val group = groupDao.getGroup(topic.getGroupId)
    val author = userService.getUserCached(topic.getAuthorUserId)
    val section = sectionService.getSection(topic.getSectionId)

    val deleteInfo = if (topic.isDeleted) {
      deleteInfoDao.getDeleteInfo(topic.getId).toScala
    } else {
      None
    }

    val deleteUser = deleteInfo.map(_.userid).map(userService.getUserCached)

    val preparedPoll = if (section.isPollPostAllowed) {
      Some(poll.getOrElse(pollPrepareService.preparePoll(topic, currentUser)))
    } else {
      None
    }

    val commiter = if (topic.getCommitby != 0) {
      Some(userService.getUserCached(topic.getCommitby))
    } else {
      None
    }

    val url = s"${siteConfig.getSecureUrlWithoutSlash}${topic.getLink}"
    val processedMessage = textService.renderTopic(text, minimizeCut, !topicPermissionService.followInTopic(topic, author), url)

    val preparedImage = if (section.isImagepost || section.isImageAllowed) {
      val loadedImage = if (topic.getId != 0) {
        imageService.imageForTopic(topic).toScala
      } else {
        image
      }

      loadedImage.flatMap(imageService.prepareImage)
    } else {
      None
    }

    val remark = if (currentUser != null) {
      remarkDao.getRemark(currentUser, author)
    } else {
      None
    }

    val postscore = topicPermissionService.getPostscore(group, topic)

    val showRegisterInvite = currentUser==null &&
      postscore <= 45 &&
      postscore != TopicPermissionService.POSTSCORE_UNRESTRICTED

    val userAgent = if (currentUser != null && currentUser.isModerator) {
      userAgentDao.getUserAgentById(topic.getUserAgentId).toScala
    } else {
      None
    }

    PreparedTopic(topic, author, deleteInfo.orNull, deleteUser.orNull, processedMessage, preparedPoll.orNull,
      commiter.orNull, tags.asJava, group, section, text.markup, preparedImage.orNull,
      TopicPermissionService.getPostScoreInfo(postscore), remark.orNull, showRegisterInvite, userAgent.orNull)
  } catch {
    case e: PollNotFoundException =>
      throw new RuntimeException(e)
  }

  /**
   * Подготовка ленты топиков для пользователя
   * сообщения рендерятся со свернутым cut
   *
   * @param messages     список топиков
   * @param user         пользователь
   * @param profile      профиль пользователя
   * @param loadUserpics флаг загрузки аватар
   * @return список подготовленных топиков
   */
  def prepareTopicsForUser(messages: java.util.List[Topic], @Nullable user: User, profile: Profile, loadUserpics: Boolean): java.util.List[PersonalizedPreparedTopic] = {
    val textMap = loadTexts(messages.asScala)
    val tags = topicTagService.getTagRefs(messages.asScala.toSeq)

    messages.asScala.map { message =>
      val preparedMessage = prepareTopic(message, tags.get(message.getId).asScala, minimizeCut = true, None,
        user, textMap(message.getId), None)

      val topicMenu = getTopicMenu(preparedMessage, user, profile, loadUserpics)
      new PersonalizedPreparedTopic(preparedMessage, topicMenu)
    }.asJava
  }

  private def loadTexts(messages: collection.Seq[Topic]) =
    msgbaseDao.getMessageText(messages.map(_.getId).map(Integer.valueOf).asJava).asScala

  /**
   * Подготовка ленты топиков, используется в TopicListController например
   * сообщения рендерятся со свернутым cut
   *
   * @param messages список топиков
   * @return список подготовленных топиков
   */
  def prepareTopics(messages: Seq[Topic]): Seq[PreparedTopic] = {
    val textMap = loadTexts(messages)
    val tags = topicTagService.getTagRefs(messages)

    messages.map { message =>
      prepareTopic(message, tags.get(message.getId).asScala, minimizeCut = true, None, null,
        textMap(message.getId), None)
    }
  }

  def getTopicMenu(topic: PreparedTopic, @Nullable currentUser: User, profile: Profile,
                   loadUserpics: Boolean): TopicMenu = {
    val topicEditable = groupPermissionService.isEditable(topic, currentUser)
    val tagsEditable = groupPermissionService.isTagsEditable(topic, currentUser)

    val (resolvable, deletable, undeletable) = if (currentUser != null) {
      val resolvable = (currentUser.isModerator || (topic.author.getId == currentUser.getId)) &&
        topic.group.isResolvable

      val deletable = groupPermissionService.isDeletable(topic.message, currentUser)
      val undeletable = groupPermissionService.isUndeletable(topic.message, currentUser)

      (resolvable, deletable, undeletable)
    } else {
      (false, false, false)
    }

    val userpic = if (loadUserpics && profile.isShowPhotos) {
      Some(userService.getUserpic(topic.author, profile.getAvatarMode, misteryMan = true))
    } else {
      None
    }

    val postscore = topicPermissionService.getPostscore(topic.group, topic.message)
    val showComments = postscore != TopicPermissionService.POSTSCORE_HIDE_COMMENTS

    new TopicMenu(topicEditable, tagsEditable, resolvable,
      topicPermissionService.isCommentsAllowed(topic.group, topic.message, currentUser, false), deletable,
      undeletable, groupPermissionService.canCommit(currentUser, topic.message), userpic.orNull, showComments)
  }
}