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

import org.apache.pekko.actor.typed.ActorRef
import com.google.common.base.Strings
import jakarta.servlet.http.HttpServletRequest
import org.apache.commons.text.StringEscapeUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.*
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.edithistory.{EditHistoryObjectTypeEnum, EditHistoryService}
import ru.org.linux.group.{GroupDao, GroupPermissionService}
import ru.org.linux.poll.{Poll, PollDao, PollVariant}
import ru.org.linux.realtime.RealtimeEventHub
import ru.org.linux.search.SearchQueueSender
import ru.org.linux.section.Section
import ru.org.linux.site.BadInputException
import ru.org.linux.spring.dao.{MessageText, MsgbaseDao}
import ru.org.linux.tag.{TagName, TagRef, TagService}
import ru.org.linux.user.{User, UserErrorException, UserPermissionService, UserPropertyEditor, UserService}
import ru.org.linux.util.ExceptionBindingErrorProcessor

import java.beans.PropertyEditorSupport
import javax.validation.Valid
import scala.collection.mutable
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsJava, MapHasAsScala, SeqHasAsJava, SetHasAsJava}

@Controller
class EditTopicController(searchQueueSender: SearchQueueSender, topicService: TopicService,
                          prepareService: TopicPrepareService, groupDao: GroupDao, pollDao: PollDao,
                          permissionService: GroupPermissionService, captcha: CaptchaService, msgbaseDao: MsgbaseDao,
                          editHistoryService: EditHistoryService, editTopicRequestValidator: EditTopicRequestValidator,
                          ipBlockDao: IPBlockDao, tagService: TagService, userService: UserService,
                          @Qualifier("realtimeHubWS") realtimeHubWS: ActorRef[RealtimeEventHub.Protocol]) {
  @RequestMapping(value = Array("/commit.jsp"), method = Array(RequestMethod.GET))
  def showCommitForm(@RequestParam("msgid") msgid: Int,
                     @ModelAttribute("form") form: EditTopicRequest): ModelAndView = AuthorizedOnly { implicit currentUser =>
    val topic = topicService.getById(msgid)

    if (!permissionService.canCommit(topic)) {
      throw new AccessViolationException("Not authorized")
    }

    if (topic.commited) {
      throw new UserErrorException("Сообщение уже подтверждено")
    }

    val preparedTopic = prepareService.prepareTopic(topic)

    if (!preparedTopic.section.isPremoderated) {
      throw new UserErrorException("Раздел не премодерируемый")
    }

    initForm(preparedTopic, form)
    val mv = new ModelAndView("edit", prepareModel(preparedTopic).asJava)

    mv.getModel.put("commit", true)

    mv
  }

  @RequestMapping(value = Array("/edit.jsp"), method = Array(RequestMethod.GET))
  def showEditForm(@RequestParam("msgid") msgid: Int,
                   @ModelAttribute("form") form: EditTopicRequest): ModelAndView = AuthorizedOnly { implicit currentUser =>
    val message = topicService.getById(msgid)
    val preparedTopic = prepareService.prepareTopic(message)

    if (!permissionService.isEditable(preparedTopic) && !permissionService.isTagsEditable(preparedTopic)) {
      throw new AccessViolationException("это сообщение нельзя править")
    }

    initForm(preparedTopic, form)
    new ModelAndView("edit", prepareModel(preparedTopic).asJava)
  }

  private def prepareModel(preparedTopic: PreparedTopic)
                          (implicit currentUser: AuthorizedSession): mutable.HashMap[String, AnyRef] = {
    val params = mutable.HashMap[String, AnyRef]()

    val message = preparedTopic.message

    params.put("message", message)
    params.put("preparedMessage", preparedTopic)

    val group = preparedTopic.group

    params.put("group", group)
    params.put("groups", groupDao.getGroups(preparedTopic.section))
    params.put("newMsg", message)

    val topicMenu = prepareService.getTopicMenu(preparedTopic, loadUserpics = true)

    params.put("topicMenu", topicMenu)

    val editInfoList = editHistoryService.getEditInfo(message.id, EditHistoryObjectTypeEnum.TOPIC)

    if (editInfoList.nonEmpty) {
      params.put("lastEdit", Long.box(editInfoList.head.editdate.toEpochMilli))
      val editors = editHistoryService.getEditorUsers(message, editInfoList)

      params.put("editors", editors.asJava)
    }

    params.put("commit", Boolean.box(false))

    val messageText = msgbaseDao.getMessageText(message.id)

    params.put("imagepost", Boolean.box(permissionService.isImagePostingAllowed(preparedTopic.section)))
    params.put("mode", messageText.markup.title)

    params
  }

  private def initForm(preparedTopic: PreparedTopic, form: EditTopicRequest)
                      (implicit session: AuthorizedSession): Unit = {
    val message = preparedTopic.message

    val editInfoList = editHistoryService.getEditInfo(message.id, EditHistoryObjectTypeEnum.TOPIC)

    if (editInfoList.nonEmpty) {
      val editors = editHistoryService.getEditorUsers(message, editInfoList)

      form.editorBonus=editors.view.map(u => u -> Integer.valueOf(0)).toMap.asJava
    }

    if (preparedTopic.group.linksAllowed) {
      form.linktext=message.linktext
      form.url=message.url
    }

    form.title=StringEscapeUtils.unescapeHtml4(message.title)

    val messageText = msgbaseDao.getMessageText(message.id)

    if (form.fromHistory != null) {
      form.msg=editHistoryService.getEditHistoryRecord(message, form.fromHistory).oldmessage.orNull
    } else {
      form.msg=messageText.text
    }

    if (message.sectionId == Section.SECTION_NEWS || message.sectionId == Section.SECTION_ARTICLES) {
      form.minor=message.minor
    }

    if (!preparedTopic.tags.isEmpty) {
      form.tags=TagRef.names(preparedTopic.tags)
    }

    if (preparedTopic.section.isPollPostAllowed) {
      val poll = pollDao.getPollByTopicId(message.id)

      form.poll=PollVariant.toMap(poll.getVariants)
      form.multiselect=poll.multiSelect
    }

    form.additionalUploadedImages=new Array[String](Math.max(0, permissionService.additionalImageLimit(preparedTopic.section) -
      preparedTopic.additionalImages.size()))
  }

  @ModelAttribute("ipBlockInfo")
  def loadIPBlock(request: HttpServletRequest): IPBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr)

  @RequestMapping(value = Array("/edit.jsp"), method = Array(RequestMethod.POST))
  @throws[Exception]
  def edit(request: HttpServletRequest,
           @RequestParam(value = "chgrp", required = false) changeGroupId: Integer,
           @Valid @ModelAttribute("form") form: EditTopicRequest, errors: Errors,
           @ModelAttribute("ipBlockInfo") ipBlockInfo: IPBlockInfo): ModelAndView = AuthorizedOnly { implicit currentUser =>
    import form.topic

    val preparedTopic = prepareService.prepareTopic(topic)

    val params = prepareModel(preparedTopic)

    val group = preparedTopic.group
    val user = currentUser.user

    UserPermissionService.checkBlockIP(ipBlockInfo, errors, user)

    val tagsEditable = permissionService.isTagsEditable(preparedTopic)
    val editable = permissionService.isEditable(preparedTopic)

    if (!editable && !tagsEditable) {
      throw new AccessViolationException("это сообщение нельзя править")
    }

    if (editable) {
      val title = request.getParameter("title")
      if (title == null || title.trim.isEmpty) {
        throw new BadInputException("заголовок сообщения не может быть пустым")
      }
    }

    val preview = request.getParameter("preview") != null
    if (preview) {
      params.put("info", "Предпросмотр")
    }

    val publish = request.getParameter("publish") != null

    val commit = request.getParameter("commit") != null

    if (commit) {
      if (!permissionService.canCommit(topic)) {
        throw new AccessViolationException("Not authorized")
      }

      if (topic.commited) {
        errors.reject(null, "Сообщение уже подтверждено")
      }
    }

    val canCommit = !topic.commited && preparedTopic.section.isPremoderated && permissionService.canCommit(topic)

    params.put("commit", Boolean.box(canCommit))

    val newMsg = Topic.fromEditRequest(group, topic, form, publish)

    var modified = false

    if (!(topic.title == newMsg.title)) {
      modified = true
    }

    val oldText = msgbaseDao.getMessageText(topic.id)

    if (form.msg != null) {
      if (!(oldText.text == form.msg)) {
        modified = true
      }
    }

    if (topic.linktext == null) {
      if (newMsg.linktext != null) {
        modified = true
      } else {
        if (!(topic.linktext == newMsg.linktext)) modified = true
      }
    }

    if (group.linksAllowed) {
      if (topic.url == null) {
        if (newMsg.url != null) {
          modified = true
        } else if (!(topic.url == newMsg.url)) {
          modified = true
        }
      }
    }

    val (imagePreview, additionalImagePreviews) =
      topicService.processUploads(form, group, errors, preparedTopic.additionalImages.size(),
        hasImage = preparedTopic.image != null)

    if (imagePreview.isDefined || additionalImagePreviews.nonEmpty) {
      modified = true
    }

    if (!editable && modified) {
      throw new AccessViolationException("нельзя править это сообщение, только теги")
    }

    if (form.minor != topic.minor && !permissionService.canCommit(topic)) {
      errors.reject(null, "вы не можете менять статус новости")
    }

    var newTags: Option[Seq[String]] = None

    if (form.tags != null) {
      newTags = Some(TagName.parseAndSanitizeTags(form.tags)).filter(_.nonEmpty)

      newTags.foreach { newTags =>
        if (!permissionService.canCreateTag(preparedTopic.section)) {
          val nonExistingTags = tagService.getNewTags(newTags)

          if (nonExistingTags.nonEmpty) {
            errors.rejectValue("tags", null, "Вы не можете создавать новые теги (" + TagService.tagsToString(nonExistingTags) + ")")
          }
        }
      }
    }

    if (changeGroupId != null) {
      if (topic.groupId != changeGroupId) {
        val changeGroup = groupDao.getGroup(changeGroupId)
        if (changeGroup.sectionId != topic.sectionId) {
          throw new AccessViolationException("Can't move topics between sections")
        }
      }
    }

    val newPoll: Option[Poll] = if (preparedTopic.section.isPollPostAllowed && form.poll != null) {
      Some(buildNewPoll(topic, form))
    } else {
      None
    }

    val newText: MessageText = if (form.msg != null) {
      MessageText.apply(form.msg, oldText.markup)
    } else {
      oldText
    }

    if (!preview && !errors.hasErrors && ipBlockInfo.captchaRequired) {
      captcha.checkCaptcha(request, errors)
    }

    if (!preview && !errors.hasErrors) {
      val editorBonus = if (form.editorBonus!=null) {
        form.editorBonus.asScala.view.mapValues(_.toInt).toMap
      } else {
        Map.empty[User, Int]
      }

      val (changed, users) = topicService.updateAndCommit(newMsg, topic, user, newTags, newText, commit,
        Option[Integer](changeGroupId).map(_.toInt), form.bonus, newPoll.map(_.variants), form.multiselect,
        editorBonus, imagePreview, additionalImagePreviews)

      if (changed || commit || publish) {
        if (!newMsg.draft) {
          searchQueueSender.updateMessage(newMsg.id, true)
          RealtimeEventHub.notifyEvents(realtimeHubWS, users)
        }

        if (!publish || !preparedTopic.section.isPremoderated) {
          return new ModelAndView(new RedirectView(TopicLinkBuilder.baseLink(topic).forceLastmod.build))
        } else {
          params.put("url", TopicLinkBuilder.baseLink(topic).forceLastmod.build)
          return new ModelAndView("add-done-moderated", params.asJava)
        }
      } else {
        errors.reject(null, "Нет изменений")
      }
    }

    params.put("newMsg", newMsg)

    params.put("newPreparedMessage",
      prepareService.prepareTopicPreview(newMsg, newTags.map(t => TagService.namesToRefs(t.asJava).asScala.toSeq).getOrElse(Seq.empty),
        newPoll, newText, imagePreview, additionalImagePreviews))

    new ModelAndView("edit", params.asJava)
  }

  private def buildNewPoll(message: Topic, form: EditTopicRequest) = {
    val poll = pollDao.getPollByTopicId(message.id)

    val changed = poll.variants.flatMap { v =>
      val label = form.poll.get(v.id)

      if (!Strings.isNullOrEmpty(label)) {
        Some(PollVariant(v.id, label))
      } else {
        None
      }
    }

    val added = form.newPoll.flatMap { label =>
      if (!Strings.isNullOrEmpty(label)) {
        Some(PollVariant(0, label))
      } else {
        None
      }
    }

    poll.copy(variants = changed ++ added)
  }

  @InitBinder(Array("form"))
  def requestValidator(binder: WebDataBinder): Unit = {
    binder.setValidator(editTopicRequestValidator)
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor)
    binder.registerCustomEditor(classOf[User], new UserPropertyEditor(userService))
    binder.registerCustomEditor(classOf[Topic], new PropertyEditorSupport() {
      override def getAsText: String = if (getValue != null) getValue.asInstanceOf[Topic].id.toString else null

      override def setAsText(text: String): Unit = setValue(topicService.getById(text.toInt))
    })
  }
}