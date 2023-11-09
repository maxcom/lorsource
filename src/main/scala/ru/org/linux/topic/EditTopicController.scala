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

    if (topic.commited) {
      throw new UserErrorException("Сообщение уже подтверждено")
    }

    val preparedTopic = prepareService.prepareTopic(topic, currentUser.user)

    if (!preparedTopic.section.isPremoderated) {
      throw new UserErrorException("Раздел не премодерируемый")
    }

    initForm(preparedTopic, form,currentUser.user)
    val mv = new ModelAndView("edit", prepareModel(preparedTopic, currentUser.user, tmpl.getProf).asJava)

    mv.getModel.put("commit", true)

    mv
  }

  @RequestMapping(value = Array("/edit.jsp"), method = Array(RequestMethod.GET))
  def showEditForm(@RequestParam("msgid") msgid: Int,
                   @ModelAttribute("form") form: EditTopicRequest): ModelAndView = AuthorizedOnly { currentUser =>
    val message = messageDao.getById(msgid)
    val user = currentUser.user
    val preparedTopic = prepareService.prepareTopic(message, user)

    if (!permissionService.isEditable(preparedTopic, user) && !permissionService.isTagsEditable(preparedTopic, user)) {
      throw new AccessViolationException("это сообщение нельзя править")
    }

    val tmpl = Template.getTemplate

    initForm(preparedTopic, form,currentUser.user)
    new ModelAndView("edit", prepareModel(preparedTopic, user, tmpl.getProf).asJava)
  }

  private def prepareModel(preparedTopic: PreparedTopic, currentUser: User, profile: Profile): mutable.HashMap[String, AnyRef] = {
    val params = mutable.HashMap[String, AnyRef]()

    val message = preparedTopic.message

    params.put("message", message)
    params.put("preparedMessage", preparedTopic)

    val group = preparedTopic.group

    params.put("group", group)
    params.put("groups", groupDao.getGroups(preparedTopic.section))
    params.put("newMsg", message)

    val topicMenu = prepareService.getTopicMenu(preparedTopic, currentUser, profile, loadUserpics = true)

    params.put("topicMenu", topicMenu)

    val editInfoList = editHistoryService.getEditInfo(message.id, EditHistoryObjectTypeEnum.TOPIC)

    if (!editInfoList.isEmpty) {
      params.put("editInfo", editInfoList.get(0))
      val editors = editHistoryService.getEditorUsers(message, editInfoList).asScala

      params.put("editors", editors.asJava)
    }

    params.put("commit", Boolean.box(false))

    val messageText = msgbaseDao.getMessageText(message.id)

    params.put("imagepost", Boolean.box(permissionService.isImagePostingAllowed(preparedTopic.section, currentUser)))
    params.put("mode", messageText.markup.title)

    params
  }

  private def initForm(preparedTopic: PreparedTopic, form: EditTopicRequest,user: User): Unit = {
    val message = preparedTopic.message

    val editInfoList = editHistoryService.getEditInfo(message.id, EditHistoryObjectTypeEnum.TOPIC)

    if (!editInfoList.isEmpty) {
      val editors = editHistoryService.getEditorUsers(message, editInfoList).asScala

      form.setEditorBonus(editors.view.map(u => Integer.valueOf(u.getId) -> Integer.valueOf(0)).toMap.asJava)
    }

    if (preparedTopic.group.linksAllowed) {
      form.setLinktext(message.linktext)
      form.setUrl(message.url)
    }

    form.setTitle(StringEscapeUtils.unescapeHtml4(message.title))

    val messageText = msgbaseDao.getMessageText(message.id)

    if (form.getFromHistory != null) {
      form.setMsg(editHistoryService.getEditHistoryRecord(message, form.getFromHistory).getOldmessage)
    } else {
      form.setMsg(messageText.text)
    }

    if (message.sectionId == Section.SECTION_NEWS || message.sectionId == Section.SECTION_ARTICLES) {
      form.setMinor(message.minor)
    }

    if (!preparedTopic.tags.isEmpty) {
      form.setTags(TagRef.names(preparedTopic.tags))
    }

    if (preparedTopic.section.isPollPostAllowed) {
      val poll = pollDao.getPollByTopicId(message.id,user.getId)

      form.setPoll(PollVariant.toMap(poll.getVariants))
      form.setMultiselect(poll.multiSelect)
    }
  }

  @ModelAttribute("ipBlockInfo")
  def loadIPBlock(request: HttpServletRequest): IPBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr)

  @RequestMapping(value = Array("/edit.jsp"), method = Array(RequestMethod.POST))
  @throws[Exception]
  def edit(request: HttpServletRequest, @RequestParam("msgid") msgid: Int,
           @RequestParam(value = "lastEdit", required = false) lastEdit: String,
           @RequestParam(value = "chgrp", required = false) changeGroupId: Integer,
           @Valid @ModelAttribute("form") form: EditTopicRequest, errors: Errors,
           @ModelAttribute("ipBlockInfo") ipBlockInfo: IPBlockInfo): ModelAndView = AuthorizedOnly { currentUser =>
    val tmpl = Template.getTemplate

    val topic = messageDao.getById(msgid)
    val preparedTopic = prepareService.prepareTopic(topic, currentUser.user)

    val params = prepareModel(preparedTopic, currentUser.user, tmpl.getProf)

    val group = preparedTopic.group
    val user = currentUser.user

    IPBlockDao.checkBlockIP(ipBlockInfo, errors, user)

    val tagsEditable = permissionService.isTagsEditable(preparedTopic, user)
    val editable = permissionService.isEditable(preparedTopic, user)

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
    val editInfoList = editHistoryService.getEditInfo(topic.id, EditHistoryObjectTypeEnum.TOPIC).asScala

    if (editInfoList.nonEmpty) {
      val editHistoryRecord = editInfoList.head

      if (lastEdit == null || editHistoryRecord.getEditdate.getTime.toString != lastEdit) {
        errors.reject(null, "Сообщение было отредактировано независимо")
      }
    }

    val commit = request.getParameter("commit") != null

    if (commit) {
      if (!permissionService.canCommit(user, topic)) {
        throw new AccessViolationException("Not authorized")
      }

      if (topic.commited) {
        throw new BadInputException("сообщение уже подтверждено")
      }
    }

    params.put("commit", Boolean.box(!topic.commited && preparedTopic.section.isPremoderated && permissionService.canCommit(user, topic)))

    val newMsg = Topic.fromEditRequest(group, topic, form, publish)

    var modified = false

    if (!(topic.title == newMsg.title)) {
      modified = true
    }

    val oldText = msgbaseDao.getMessageText(topic.id)

    if (form.getMsg != null) {
      if (!(oldText.text == form.getMsg)) {
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

    val imagePreview: Option[UploadedImagePreview] =
      if (permissionService.isImagePostingAllowed(preparedTopic.section, user) && permissionService.isTopicPostingAllowed(group, user)) {
        val image = imageService.processUploadImage(request)
        val preview = imageService.processUpload(user, Option(form.getUploadedImage), image, errors)

        preview.foreach { img =>
          modified = true
          form.setUploadedImage(img.mainFile.getName)
        }

        preview
    } else {
      None
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
        if (!permissionService.canCreateTag(preparedTopic.section, user)) {
          val nonExistingTags = tagService.getNewTags(newTags)

          if (nonExistingTags.nonEmpty) {
            errors.rejectValue("tags", null, "Вы не можете создавать новые теги (" + TagService.tagsToString(nonExistingTags.asJava) + ")")
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

    val newPoll: Option[Poll] = if (preparedTopic.section.isPollPostAllowed && form.getPoll != null) {
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

    val imageObject: Option[Image] = imagePreview.map { i =>
      new Image(0, 0, "gallery/preview/" + i.mainFile.getName)
    }

    params.put("newPreparedMessage",
      prepareService.prepareTopicPreview(newMsg, newTags.map(t => TagService.namesToRefs(t.asJava).asScala.toSeq).getOrElse(Seq.empty),
        newPoll, newText, imageObject))

    new ModelAndView("edit", params.asJava)
  }

  private def buildNewPoll(message: Topic, form: EditTopicRequest) = {
    val poll = pollDao.getPollByTopicId(message.id,0)

    val changed = poll.variants.flatMap { v =>
      val label = form.getPoll.get(v.id)

      if (!Strings.isNullOrEmpty(label)) {
        Some(PollVariant(v.id, label,0))
      } else {
        None
      }
    }

    val added = form.getNewPoll.flatMap { label =>
      if (!Strings.isNullOrEmpty(label)) {
        Some(PollVariant(0, label,0))
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