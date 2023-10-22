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

import akka.actor.ActorRef
import com.google.common.base.Strings
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
import ru.org.linux.edithistory.EditHistoryObjectTypeEnum
import ru.org.linux.edithistory.EditHistoryService
import ru.org.linux.gallery.Image
import ru.org.linux.gallery.ImageService
import ru.org.linux.gallery.UploadedImagePreview
import ru.org.linux.group.GroupDao
import ru.org.linux.group.GroupPermissionService
import ru.org.linux.poll.Poll
import ru.org.linux.poll.PollDao
import ru.org.linux.poll.PollVariant
import ru.org.linux.realtime.RealtimeEventHub
import ru.org.linux.search.SearchQueueSender
import ru.org.linux.section.Section
import ru.org.linux.site.BadInputException
import ru.org.linux.site.Template
import ru.org.linux.spring.dao.MessageText
import ru.org.linux.spring.dao.MsgbaseDao
import ru.org.linux.tag.TagName
import ru.org.linux.tag.TagRef
import ru.org.linux.tag.TagService
import ru.org.linux.user.Profile
import ru.org.linux.user.User
import ru.org.linux.user.UserErrorException
import ru.org.linux.util.ExceptionBindingErrorProcessor

import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import scala.collection.mutable
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsJava, MapHasAsScala, SeqHasAsJava, SetHasAsJava, SetHasAsScala}

@Controller
class EditTopicController(messageDao: TopicDao, searchQueueSender: SearchQueueSender, topicService: TopicService,
                          prepareService: TopicPrepareService, groupDao: GroupDao, pollDao: PollDao,
                          permissionService: GroupPermissionService, captcha: CaptchaService, msgbaseDao: MsgbaseDao,
                          editHistoryService: EditHistoryService, imageService: ImageService,
                          editTopicRequestValidator: EditTopicRequestValidator, ipBlockDao: IPBlockDao,
                          @Qualifier("realtimeHubWS") realtimeHubWS: ActorRef, tagService: TagService) {
  @RequestMapping(value = Array("/commit.jsp"), method = Array(RequestMethod.GET))
  def showCommitForm(@RequestParam("msgid") msgid: Int, @ModelAttribute("form") form: EditTopicRequest): ModelAndView = AuthorizedOnly { currentUser =>
    val tmpl = Template.getTemplate
    val topic = messageDao.getById(msgid)

    if (!permissionService.canCommit(currentUser.user, topic)) {
      throw new AccessViolationException("Not authorized")
    }

    if (topic.isCommited) {
      throw new UserErrorException("Сообщение уже подтверждено")
    }

    val preparedMessage = prepareService.prepareTopic(topic, currentUser.user)

    if (!preparedMessage.getSection.isPremoderated) {
      throw new UserErrorException("Раздел не премодерируемый")
    }

    val mv = prepareModel(preparedMessage, form, currentUser.user, tmpl.getProf)

    mv.getModel.put("commit", true)

    mv
  }

  @RequestMapping(value = Array("/edit.jsp"), method = Array(RequestMethod.GET))
  def showEditForm(@RequestParam("msgid") msgid: Int,
                   @ModelAttribute("form") form: EditTopicRequest): ModelAndView = AuthorizedOnly { currentUser =>
    val message = messageDao.getById(msgid)
    val user = currentUser.user
    val preparedMessage = prepareService.prepareTopic(message, user)

    if (!permissionService.isEditable(preparedMessage, user) && !permissionService.isTagsEditable(preparedMessage, user)) {
      throw new AccessViolationException("это сообщение нельзя править")
    }

    val tmpl = Template.getTemplate

    prepareModel(preparedMessage, form, user, tmpl.getProf)
  }

  private def prepareModel(preparedTopic: PreparedTopic, form: EditTopicRequest, currentUser: User, profile: Profile) = {
    val params = mutable.HashMap[String, AnyRef]()

    val message = preparedTopic.getMessage

    params.put("message", message)
    params.put("preparedMessage", preparedTopic)

    val group = preparedTopic.getGroup

    params.put("group", group)
    params.put("groups", groupDao.getGroups(preparedTopic.getSection))
    params.put("newMsg", message)

    val topicMenu = prepareService.getTopicMenu(preparedTopic, currentUser, profile, loadUserpics = true)

    params.put("topicMenu", topicMenu)

    val editInfoList = editHistoryService.getEditInfo(message.getId, EditHistoryObjectTypeEnum.TOPIC)

    if (!editInfoList.isEmpty) {
      params.put("editInfo", editInfoList.get(0))
      val editors = editHistoryService.getEditorUsers(message, editInfoList).asScala

      form.setEditorBonus(editors.view.map(u => Integer.valueOf(u.getId) -> Integer.valueOf(0)).toMap.asJava)

      params.put("editors", editors.asJava)
    }

    params.put("commit", Boolean.box(false))

    if (group.isLinksAllowed) {
      form.setLinktext(message.getLinktext)
      form.setUrl(message.getUrl)
    }

    form.setTitle(StringEscapeUtils.unescapeHtml4(message.getTitle))

    val messageText = msgbaseDao.getMessageText(message.getId)

    if (form.getFromHistory != null) {
      form.setMsg(editHistoryService.getEditHistoryRecord(message, form.getFromHistory).getOldmessage)
    } else {
      form.setMsg(messageText.text)
    }

    if (message.getSectionId == Section.SECTION_NEWS || message.getSectionId == Section.SECTION_ARTICLES) {
      form.setMinor(message.isMinor)
    }

    if (!preparedTopic.getTags.isEmpty) {
      form.setTags(TagRef.names(preparedTopic.getTags))
    }

    if (preparedTopic.getSection.isPollPostAllowed) {
      val poll = pollDao.getPollByTopicId(message.getId)

      form.setPoll(PollVariant.toMap(poll.getVariants))
      form.setMultiselect(poll.isMultiSelect)
    }

    params.put("imagepost", Boolean.box(permissionService.isImagePostingAllowed(preparedTopic.getSection, currentUser)))
    params.put("mode", messageText.markup.title)

    new ModelAndView("edit", params.asJava)
  }

  @ModelAttribute("ipBlockInfo") private def loadIPBlock(request: HttpServletRequest) = ipBlockDao.getBlockInfo(request.getRemoteAddr)

  @RequestMapping(value = Array("/edit.jsp"), method = Array(RequestMethod.POST))
  @throws[Exception]
  def edit(request: HttpServletRequest, @RequestParam("msgid") msgid: Int,
           @RequestParam(value = "lastEdit", required = false) lastEdit: String,
           @RequestParam(value = "chgrp", required = false) changeGroupId: Integer,
           @Valid @ModelAttribute("form") form: EditTopicRequest, errors: Errors,
           @ModelAttribute("ipBlockInfo") ipBlockInfo: IPBlockInfo): ModelAndView = AuthorizedOnly { currentUser =>
    val tmpl = Template.getTemplate

    val params = new mutable.HashMap[String, AnyRef]()

    val topic = messageDao.getById(msgid)
    val preparedTopic = prepareService.prepareTopic(topic, currentUser.user)
    val group = preparedTopic.getGroup
    val section = preparedTopic.section
    val user = currentUser.user

    IPBlockDao.checkBlockIP(ipBlockInfo, errors, user)

    val tagsEditable = permissionService.isTagsEditable(preparedTopic, user)
    val editable = permissionService.isEditable(preparedTopic, user)

    if (!editable && !tagsEditable) {
      throw new AccessViolationException("это сообщение нельзя править")
    }

    params.put("message", topic)
    params.put("preparedMessage", preparedTopic)
    params.put("group", group)
    params.put("topicMenu", prepareService.getTopicMenu(preparedTopic, user, tmpl.getProf, loadUserpics = true))
    params.put("groups", groupDao.getGroups(preparedTopic.getSection))

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
    val editInfoList = editHistoryService.getEditInfo(topic.getId, EditHistoryObjectTypeEnum.TOPIC).asScala

    if (editInfoList.nonEmpty) {
      val editHistoryRecord = editInfoList.head
      params.put("editInfo", editHistoryRecord)

      if (lastEdit == null || editHistoryRecord.getEditdate.getTime.toString != lastEdit) {
        errors.reject(null, "Сообщение было отредактировано независимо")
      }
    }

    val commit = request.getParameter("commit") != null

    if (commit) {
      if (!permissionService.canCommit(user, topic)) {
        throw new AccessViolationException("Not authorized")
      }

      if (topic.isCommited) {
        throw new BadInputException("сообщение уже подтверждено")
      }
    }

    params.put("commit", Boolean.box(!topic.isCommited && preparedTopic.getSection.isPremoderated && permissionService.canCommit(user, topic)))

    val newMsg = Topic.fromEditRequest(group, topic, form, publish)

    var modified = false

    if (!(topic.getTitle == newMsg.getTitle)) {
      modified = true
    }

    val oldText = msgbaseDao.getMessageText(topic.getId)

    if (form.getMsg != null) {
      if (!(oldText.text == form.getMsg)) {
        modified = true
      }
    }

    if (topic.getLinktext == null) {
      if (newMsg.getLinktext != null) {
        modified = true
      } else {
        if (!(topic.getLinktext == newMsg.getLinktext)) modified = true
      }
    }

    if (group.isLinksAllowed) {
      if (topic.getUrl == null) {
        if (newMsg.getUrl != null) {
          modified = true
        } else if (!(topic.getUrl == newMsg.getUrl)) {
          modified = true
        }
      }
    }

    var imagePreview: Option[UploadedImagePreview] = None
    if (permissionService.isImagePostingAllowed(preparedTopic.getSection, user)) {
      if (permissionService.isTopicPostingAllowed(group, user)) {
        val image = imageService.processUploadImage(request)
        imagePreview = Option(imageService.processUpload(user, request.getSession, image, errors))

        if (imagePreview.isDefined) {
          modified = true
        }
      }
    }

    if (!editable && modified) {
      throw new AccessViolationException("нельзя править это сообщение, только теги")
    }

    if (form.getMinor != null && !permissionService.canCommit(user, topic)) {
      throw new AccessViolationException("вы не можете менять статус новости")
    }

    var newTags: Option[Seq[String]] = None

    if (form.getTags != null) {
      newTags = Some(TagName.parseAndSanitizeTags(form.getTags)).filter(_.nonEmpty)

      newTags.foreach { newTags =>
        if (!permissionService.canCreateTag(section, user)) {
          val nonExistingTags = tagService.getNewTags(newTags)

          if (nonExistingTags.nonEmpty) {
            errors.rejectValue("tags", null, "Вы не можете создавать новые теги (" + TagService.tagsToString(nonExistingTags.asJava) + ")")
          }
        }
      }
    }

    if (changeGroupId != null) {
      if (topic.getGroupId != changeGroupId) {
        val changeGroup = groupDao.getGroup(changeGroupId)
        if (changeGroup.getSectionId != topic.getSectionId) {
          throw new AccessViolationException("Can't move topics between sections")
        }
      }
    }

    val newPoll: Option[Poll] = if (preparedTopic.getSection.isPollPostAllowed && form.getPoll != null) {
      Some(buildNewPoll(topic, form))
    } else {
      None
    }

    val newText: MessageText = if (form.getMsg != null) {
      MessageText.apply(form.getMsg, oldText.markup)
    } else {
      oldText
    }

    if (form.getEditorBonus != null) {
      val editors = editHistoryService.getEditors(topic, editInfoList.asJava)

      form.getEditorBonus.asScala.keySet.filter(userid => !editors.contains(userid)).foreach { _ =>
        errors.reject("editorBonus", "некорректный корректор?!")
      }
    }

    if (!preview && !errors.hasErrors && ipBlockInfo.isCaptchaRequired) {
      captcha.checkCaptcha(request, errors)
    }

    if (!preview && !errors.hasErrors) {
      val (changed, users) = topicService.updateAndCommit(newMsg, topic, user, newTags.map(_.asJava).orNull, newText, commit,
        changeGroupId, form.getBonus, newPoll.map(_.getVariants).orNull, form.isMultiselect,
        form.getEditorBonus, imagePreview.orNull)

      if (changed || commit || publish) {
        if (!newMsg.isDraft) {
          searchQueueSender.updateMessage(newMsg.getId, true)
          RealtimeEventHub.notifyEvents(realtimeHubWS, users)
        }

        if (!publish || !preparedTopic.getSection.isPremoderated) {
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

    val imageObject: Option[Image] = imagePreview.map { i =>
      new Image(0, 0, "gallery/preview/" + i.mainFile.getName)
    }

    params.put("newPreparedMessage",
      prepareService.prepareTopicPreview(newMsg, newTags.map(t => TagService.namesToRefs(t.asJava).asScala.toSeq).getOrElse(Seq.empty),
        newPoll, newText, imageObject))

    params.put("mode", oldText.markup.title)

    new ModelAndView("edit", params.asJava)
  }

  private def buildNewPoll(message: Topic, form: EditTopicRequest) = {
    val poll = pollDao.getPollByTopicId(message.getId)

    val changed = poll.variants.flatMap { v =>
      val label = form.getPoll.get(v.getId)

      if (!Strings.isNullOrEmpty(label)) {
        Some(PollVariant(v.getId, label))
      } else {
        None
      }
    }

    val added = form.getNewPoll.flatMap { label =>
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
  }
}