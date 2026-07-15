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
package ru.org.linux.topic

import com.google.common.base.Strings
import jakarta.servlet.http.HttpServletRequest
import org.apache.commons.text.StringEscapeUtils
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.*
import ru.org.linux.auth.AuthUtil.{AuthorizedOnly, CorrectorOrModerator}
import ru.org.linux.edithistory.{EditHistoryObjectTypeEnum, EditHistoryService}
import ru.org.linux.gallery.UploadedImagePreview
import ru.org.linux.group.{GroupPermissionService, GroupService}
import ru.org.linux.msgbase.{MessageText, MsgbaseDao}
import ru.org.linux.poll.{Poll, PollDao, PollVariant}
import ru.org.linux.rights.{EditTopicChecker, Permission, TopicPublishChecker}
import ru.org.linux.section.Section
import ru.org.linux.site.BadInputException
import ru.org.linux.tag.{TagName, TagRef, TagService}
import ru.org.linux.user.{User, UserErrorException, UserPropertyEditor, UserService}
import ru.org.linux.util.ExceptionBindingErrorProcessor

import java.beans.PropertyEditorSupport
import javax.validation.Valid
import scala.collection.mutable
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsJava, MapHasAsScala, SeqHasAsJava, SetHasAsJava}

@Controller
class EditTopicController(
    topicService: TopicService,
    prepareService: TopicPrepareService,
    groupService: GroupService,
    pollDao: PollDao,
    permissionService: GroupPermissionService,
    msgbaseDao: MsgbaseDao,
    editHistoryService: EditHistoryService,
    editTopicRequestValidator: EditTopicRequestValidator,
    tagService: TagService,
    userService: UserService,
    topicPublishChecker: TopicPublishChecker):
  @RequestMapping(value = Array("/commit.jsp"), method = Array(RequestMethod.GET))
  def showCommitForm(
      @RequestParam("msgid")
      msgid: Int,
      @ModelAttribute("form")
      form: EditTopicRequest): ModelAndView =
    CorrectorOrModerator { implicit currentUser =>
      val topic = topicService.getById(msgid)
      val preparedTopic = prepareService.prepareTopic(topic)

      if topic.commited then
        throw new UserErrorException("Топик уже подтвержден")

      if !preparedTopic.committable then
        throw new UserErrorException("Этот топик нельзя подтвердить")

      EditTopicChecker.checkCommit(topic).checkOrThrow()

      initForm(preparedTopic, form)

      val mv = new ModelAndView("edit", {
        val params = prepareModel(preparedTopic)
        publishParams(topic, preparedTopic, params)
        params.asJava
      })

      mv.getModel.put("commit", true)

      mv
    }

  @RequestMapping(value = Array("/edit.jsp"), method = Array(RequestMethod.GET))
  def showEditForm(
      @RequestParam("msgid")
      msgid: Int,
      @ModelAttribute("form")
      form: EditTopicRequest): ModelAndView =
    AuthorizedOnly { implicit currentUser =>
      val message = topicService.getById(msgid)
      val preparedTopic = prepareService.prepareTopic(message)

      EditTopicChecker.checkAnythingEdit(preparedTopic).checkOrThrow()

      initForm(preparedTopic, form)

      val params = prepareModel(preparedTopic)
      publishParams(message, preparedTopic, params)
      new ModelAndView("edit", params.asJava)
    }

  private def publishCheck(preparedTopic: PreparedTopic)(using session: AuthorizedSession): Permission =
    val topicLimitInfo = permissionService.topicLimitInfo(preparedTopic.section)
    topicPublishChecker.checkPublish(preparedTopic.group, topicLimitInfo)

  private def publishParams(topic: Topic, preparedTopic: PreparedTopic, params: mutable.HashMap[String, AnyRef])(using
      session: AuthorizedSession): Unit =
    if topic.draft then
      val pc = publishCheck(preparedTopic)
      params.put("topicPublishingAllowed", Boolean.box(pc.permitted))
      params.put("topicPublishingReason", pc.reason)

  private def prepareModel(preparedTopic: PreparedTopic)(using
      currentUser: AuthorizedSession): mutable.HashMap[String, AnyRef] =
    val params = mutable.HashMap[String, AnyRef]()

    val message = preparedTopic.message

    params.put("message", message)
    params.put("preparedMessage", preparedTopic)

    val group = preparedTopic.group

    params.put("group", group)
    params.put("groups", groupService.getGroups(preparedTopic.section).asJava)
    params.put("newMsg", message)

    val topicMenu = prepareService.getTopicMenu(preparedTopic, loadUserpics = true)

    params.put("topicMenu", topicMenu)

    val editInfoList = editHistoryService.getEditInfo(message.id, EditHistoryObjectTypeEnum.TOPIC)

    if editInfoList.nonEmpty then
      params.put("lastEdit", Long.box(editInfoList.head.editdate.toEpochMilli))
      val editors = editHistoryService.getEditorUsers(message, editInfoList)

      params.put("editors", editors.asJava)

    params.put("commit", Boolean.box(false))

    val messageText = msgbaseDao.getMessageText(message.id)

    params.put("imagepost", Boolean.box(permissionService.isImagePostingAllowed(preparedTopic.section)))
    params.put("mode", messageText.markup.title)
    params.put("modeFormId", messageText.markup.formId)

    params

  private def initForm(preparedTopic: PreparedTopic, form: EditTopicRequest)(using session: AuthorizedSession): Unit =
    val message = preparedTopic.message

    val editInfoList = editHistoryService.getEditInfo(message.id, EditHistoryObjectTypeEnum.TOPIC)

    if editInfoList.nonEmpty then
      val editors = editHistoryService.getEditorUsers(message, editInfoList)

      form.editorBonus = editors.view.map(u => u -> Integer.valueOf(0)).toMap.asJava

    if preparedTopic.group.linksAllowed then
      form.linktext = message.linktext
      form.url = message.url

    form.title = StringEscapeUtils.unescapeHtml4(message.title)

    val messageText = msgbaseDao.getMessageText(message.id)

    if form.fromHistory != null then
      form.msg = editHistoryService.getEditHistoryRecord(message, form.fromHistory).oldmessage.orNull
    else
      form.msg = messageText.text

    if message.sectionId == Section.News || message.sectionId == Section.Articles then
      form.minor = message.minor

    if !preparedTopic.tags.isEmpty then
      form.tags = TagRef.names(preparedTopic.tags.asScala.toSeq)

    if preparedTopic.section.isPollPostAllowed then
      val poll = pollDao.getPollByTopicId(message.id)

      form.poll = PollVariant.toMap(poll.getVariants)
      form.multiselect = poll.multiSelect

    form.additionalUploadedImages =
      new Array[String](
        Math.max(
          0,
          permissionService.additionalImageLimit(preparedTopic.section) - preparedTopic.additionalImages.size()))

  private def checkModified(
      preparedTopic: PreparedTopic,
      newMsg: Topic,
      form: EditTopicRequest,
      oldText: MessageText,
      imagePreview: Option[UploadedImagePreview],
      additionalImagePreviews: Seq[UploadedImagePreview]): Boolean =
    var modified = false
    val topic = preparedTopic.message

    if !(topic.title == newMsg.title) then
      modified = true

    if form.msg != null then
      if !(oldText.text == form.msg) then
        modified = true

    if topic.linktext == null then
      if newMsg.linktext != null then
        modified = true
      else if !(topic.linktext == newMsg.linktext) then
        modified = true

    if preparedTopic.group.linksAllowed then
      if topic.url == null then
        if newMsg.url != null then
          modified = true
        else if !(topic.url == newMsg.url) then
          modified = true

    if imagePreview.isDefined || additionalImagePreviews.nonEmpty then
      modified = true

    modified
  end checkModified

  @RequestMapping(value = Array("/edit.jsp"), method = Array(RequestMethod.POST))
  def edit(
      request: HttpServletRequest,
      @RequestParam(value = "chgrp", required = false)
      changeGroupId: Integer,
      @Valid @ModelAttribute("form")
      form: EditTopicRequest,
      errors: Errors): ModelAndView =
    AuthorizedOnly { implicit currentUser =>
      import form.topic

      val preparedTopic = prepareService.prepareTopic(topic)

      val params = prepareModel(preparedTopic)

      publishParams(topic, preparedTopic, params)

      val editCheck = EditTopicChecker.checkContentEdit(preparedTopic)
      val editable = editCheck.permitted

      editCheck.or(EditTopicChecker.checkTagsEdit(preparedTopic)).checkOrThrow()

      if editable then
        val title = request.getParameter("title")
        if title == null || title.trim.isEmpty then
          throw new BadInputException("заголовок сообщения не может быть пустым")

      val preview = request.getParameter("preview") != null
      if preview then
        params.put("info", "Предпросмотр")

      val commit = request.getParameter("commit") != null

      val commitCheck = EditTopicChecker.checkCommit(topic)

      if commit then
        if topic.commited then
          errors.reject(null, "Топик уже подтвержден")
        else if !preparedTopic.committable then
          errors.reject(null, "Этот топик нельзя подтвердить")

        if !errors.hasErrors then
          commitCheck.checkOrError(errors)

      params.put("commit", Boolean.box(preparedTopic.committable && commitCheck.permitted))

      val newMsg = Topic.fromEditRequest(preparedTopic.group, topic, form, request.getParameter("publish") != null)
      val oldText = msgbaseDao.getMessageText(topic.id)

      val (imagePreview, additionalImagePreviews) = topicService.processUploads(
        form,
        preparedTopic.group,
        errors,
        preparedTopic.additionalImages.size(),
        hasImage = preparedTopic.image != null)

      val modified = checkModified(preparedTopic, newMsg, form, oldText, imagePreview, additionalImagePreviews)

      if !editable && modified then
        throw new AccessViolationException("нельзя править это сообщение, только теги")

      if form.minor != topic.minor && (!preparedTopic.canBeMini || commitCheck.restricted) then
        errors.reject(null, "вы не можете менять статус новости")

      val newTags: Option[Seq[String]] = Option(form.tags).map(TagName.parseAndSanitizeTags).filter(_.nonEmpty)

      newTags.foreach { newTags =>
        if !permissionService.canCreateTag(preparedTopic.section) then
          val nonExistingTags = tagService.getNewTags(newTags)

          if nonExistingTags.nonEmpty then
            errors.rejectValue(
              "tags",
              null,
              "Вы не можете создавать новые теги (" + TagService.tagsToString(nonExistingTags) + ")")
      }

      if changeGroupId != null then
        if topic.groupId != changeGroupId then
          val changeGroup = groupService.getGroup(changeGroupId)
          if changeGroup.sectionId != topic.sectionId then
            throw new AccessViolationException("Can't move topics between sections")

      val newPoll: Option[Poll] =
        if preparedTopic.section.isPollPostAllowed && form.poll != null then
          Some(buildNewPoll(topic, form))
        else
          None

      val newText: MessageText =
        if form.msg != null then
          MessageText.apply(form.msg, oldText.markup)
        else
          oldText

      val doPublish = topic.draft && !newMsg.draft

      if doPublish && !preview && !errors.hasErrors then
        publishCheck(preparedTopic).checkOrError(errors)

      val performed = if !preview && !errors.hasErrors then
        val changed = topicService.updateAndCommit(
          newMsg = newMsg,
          oldMsg = topic,
          user = currentUser.user,
          newTags = newTags,
          newText = newText,
          commit = commit,
          publish = doPublish,
          changeGroupId = Option[Integer](changeGroupId).map(_.toInt),
          bonus = form.bonus,
          pollVariants = newPoll.map(_.variants),
          multiselect = form.multiselect,
          editorBonus = form.editorBonusScala,
          imagePreview = imagePreview,
          additionalImages = additionalImagePreviews
        )

        if changed || commit || doPublish then
          true
        else
          errors.reject(null, "Нет изменений")
          false
      else
        false

      if performed then
        if doPublish && preparedTopic.section.premoderated then
          params.put("url", TopicLinkBuilder.baseLink(topic).forceLastmod.build)
          new ModelAndView("add-done-moderated", params.asJava)
        else
          new ModelAndView(new RedirectView(TopicLinkBuilder.baseLink(topic).forceLastmod.build))
      else
        params.put("newMsg", newMsg)

        params.put(
          "newPreparedMessage",
          prepareService.prepareTopicPreview(
            newMsg,
            newTags.map(t => TagService.namesToRefs(t.asJava).asScala.toSeq).getOrElse(Seq.empty),
            newPoll,
            newText,
            imagePreview,
            additionalImagePreviews)
        )

        new ModelAndView("edit", params.asJava)
    }

  private def buildNewPoll(message: Topic, form: EditTopicRequest) =
    val poll = pollDao.getPollByTopicId(message.id)

    val changed = poll
      .variants
      .flatMap { v =>
        val label = form.poll.get(v.id)

        if !Strings.isNullOrEmpty(label) then
          Some(PollVariant(v.id, label))
        else
          None
      }

    val added = form
      .newPoll
      .flatMap { label =>
        if !Strings.isNullOrEmpty(label) then
          Some(PollVariant(0, label))
        else
          None
      }

    poll.copy(variants = changed ++ added)

  @InitBinder(Array("form"))
  def requestValidator(binder: WebDataBinder): Unit =
    binder.setValidator(editTopicRequestValidator)
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor)
    binder.registerCustomEditor(classOf[User], new UserPropertyEditor(userService))
    binder.registerCustomEditor(
      classOf[Topic],
      new PropertyEditorSupport():
        override def getAsText: String =
          if getValue != null then
            getValue.asInstanceOf[Topic].id.toString
          else
            null

        override def setAsText(text: String): Unit = setValue(topicService.getById(text.toInt))
    )
