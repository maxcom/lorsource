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
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.util.UriComponentsBuilder
import ru.org.linux.auth.*
import ru.org.linux.auth.AuthUtil.AuthorizedOpt
import ru.org.linux.csrf.CSRFNoAuto
import ru.org.linux.csrf.CSRFProtectionService
import ru.org.linux.gallery.Image
import ru.org.linux.gallery.ImageService
import ru.org.linux.gallery.UploadedImagePreview
import ru.org.linux.group.Group
import ru.org.linux.group.GroupDao
import ru.org.linux.group.GroupPermissionService
import ru.org.linux.markup.MarkupPermissions
import ru.org.linux.markup.MessageTextService
import ru.org.linux.poll.Poll
import ru.org.linux.poll.PollVariant
import ru.org.linux.realtime.RealtimeEventHub
import ru.org.linux.search.SearchQueueSender
import ru.org.linux.section.Section
import ru.org.linux.section.SectionService
import ru.org.linux.site.Template
import ru.org.linux.markup.MarkupType
import ru.org.linux.spring.dao.MessageText
import ru.org.linux.tag.TagName
import ru.org.linux.tag.TagService
import ru.org.linux.user.User
import ru.org.linux.user.UserPropertyEditor
import ru.org.linux.user.UserService
import ru.org.linux.util.ExceptionBindingErrorProcessor
import ru.org.linux.util.markdown.MarkdownFormatter

import javax.annotation.Nullable
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import java.beans.PropertyEditorSupport
import java.nio.charset.StandardCharsets
import javax.servlet.ServletContext
import scala.collection.mutable
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsJava, SeqHasAsJava}

@Controller
object AddTopicController {
  private val MAX_MESSAGE_LENGTH_ANONYMOUS = 8196
  private val MAX_MESSAGE_LENGTH = 65536

  private def preparePollPreview(form: AddTopicRequest,currentUserId: Int): Poll = {
    val variants = form.getPoll.filterNot(Strings.isNullOrEmpty).map(item => PollVariant(0, item,currentUserId))

    Poll(0, 0, form.isMultiSelect, variants.toVector)
  }

  def getAddUrl(section: Section, tag: String): String = {
    val builder = UriComponentsBuilder.fromPath("/add-section.jsp")
    builder.queryParam("section", section.getId)
    builder.queryParam("tag", tag)
    builder.build.toUriString
  }

  def getAddUrl(section: Section): String = {
    val builder = UriComponentsBuilder.fromPath("/add-section.jsp")
    builder.queryParam("section", section.getId)
    builder.build.toUriString
  }

  def getAddUrl(group: Group): String = getAddUrl(group, null)

  def getAddUrl(group: Group, @Nullable tag: String): String = {
    val builder = UriComponentsBuilder.fromPath("/add.jsp")
    builder.queryParam("group", group.id)
    if (tag != null) builder.queryParam("tags", tag)
    builder.build.toUriString
  }
}

@Controller
class AddTopicController(searchQueueSender: SearchQueueSender, captcha: CaptchaService, sectionService: SectionService,
                         tagService: TagService, userService: UserService, prepareService: TopicPrepareService,
                         groupPermissionService: GroupPermissionService,
                         addTopicRequestValidator: AddTopicRequestValidator, imageService: ImageService,
                         topicService: TopicService, @Qualifier("realtimeHubWS") realtimeHubWS: ActorRef,
                         renderService: MarkdownFormatter, groupDao: GroupDao, dupeProtector: FloodProtector,
                         ipBlockDao: IPBlockDao, servletContext: ServletContext) {
  @ModelAttribute("ipBlockInfo")
  def loadIPBlock(request: HttpServletRequest): IPBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr)

  @RequestMapping(value = Array("/add.jsp"), method = Array(RequestMethod.GET))
  def add(@Valid @ModelAttribute("form") form: AddTopicRequest): ModelAndView = AuthorizedOpt { currentUser =>
    val tmpl = Template.getTemplate

    if (form.getMode == null) {
      form.setMode(tmpl.getFormatMode)
    }

    val group = form.getGroup

    if (currentUser.isDefined && !groupPermissionService.isTopicPostingAllowed(group, currentUser.map(_.user).orNull)) {
      val errorView = new ModelAndView("errors/good-penguin")

      errorView.addObject("msgHeader", "Недостаточно прав для постинга тем в эту группу")
      errorView.addObject("msgMessage", groupPermissionService.getPostScoreInfo(group))

      errorView
    } else {
      val section = sectionService.getSection(form.getGroup.sectionId)

      val params = prepareModel(Some(form.getGroup), currentUser.map(_.user), section)

      new ModelAndView("add", params.asJava)
    }
  }

  private def prepareModel(group: Option[Group], currentUser: Option[User], section: Section): Map[String, AnyRef] = {
    val params = Map.newBuilder[String, AnyRef]

    val helpResource = servletContext.getResource("/help/new-topic-" + Section.getUrlName(section.getId) + ".md")
    if (helpResource != null) {
      val helpRawText = IOUtils.toString(helpResource, StandardCharsets.UTF_8)
      val addInfo = renderService.renderToHtml(helpRawText, nofollow = false)
      params.addOne("addportal" -> addInfo)
    }

    params.addOne("sectionId" -> Integer.valueOf(section.getId))
    params.addOne("section" -> section)

    group.foreach { group =>
      params.addOne("group" -> group)
      params.addOne("postscoreInfo" -> groupPermissionService.getPostScoreInfo(group))
      params.addOne("showAllowAnonymous" -> Boolean.box(groupPermissionService.enableAllowAnonymousCheckbox(group, currentUser.orNull)))
      params.addOne("imagepost" -> Boolean.box(groupPermissionService.isImagePostingAllowed(section, currentUser.orNull)))
    }

    params.result()
  }

  private def postingUser(sessionUserOpt: Option[CurrentUser], form: AddTopicRequest) = {
    sessionUserOpt match {
      case Some(currentUser) =>
        currentUser.user
      case None if form.getNick != null =>
        form.getNick
      case _ =>
        userService.getAnonymous
    }
  }

  @RequestMapping(value = Array("/add.jsp"), method = Array(RequestMethod.POST))
  @CSRFNoAuto
  def doAdd(request: HttpServletRequest, @Valid @ModelAttribute("form") form: AddTopicRequest, errors: BindingResult,
            @ModelAttribute("ipBlockInfo") ipBlockInfo: IPBlockInfo): ModelAndView = AuthorizedOpt { sessionUserOpt =>
    val group = form.getGroup
    val section = sectionService.getSection(group.sectionId)

    val params = prepareModel(Some(group), sessionUserOpt.map(_.user), section).to(mutable.HashMap)

    val user = postingUser(sessionUserOpt, form)

    user.checkBlocked(errors)
    user.checkFrozen(errors)

    IPBlockDao.checkBlockIP(ipBlockInfo, errors, user)

    if (!groupPermissionService.isTopicPostingAllowed(group, user)) {
      errors.reject(null, "Недостаточно прав для постинга тем в эту группу")
    }

    val tmpl = Template.getTemplate

    if (form.getMode == null) {
      form.setMode(tmpl.getFormatMode)
    }

    if (!groupPermissionService.enableAllowAnonymousCheckbox(group, user)) {
      form.setAllowAnonymous(true)
    }

    if (!MarkupPermissions.allowedFormats(sessionUserOpt.map(_.user).orNull).map(_.formId).contains(form.getMode)) {
      errors.rejectValue("mode", null, "Некорректный режим разметки")
      form.setMode(MarkupType.Lorcode.formId)
    }

    val message = MessageTextService.processPostingText(Strings.nullToEmpty(form.getMsg), form.getMode)

    if (user.isAnonymous) {
      if (message.text.length > AddTopicController.MAX_MESSAGE_LENGTH_ANONYMOUS) {
        errors.rejectValue("msg", null, "Слишком большое сообщение")
      }
    } else {
      if (message.text.length > AddTopicController.MAX_MESSAGE_LENGTH) {
        errors.rejectValue("msg", null, "Слишком большое сообщение")
      }
    }

    val session = request.getSession

    var imagePreview: Option[UploadedImagePreview] = None

    if (groupPermissionService.isImagePostingAllowed(section, user)) {
      if (groupPermissionService.isTopicPostingAllowed(group, user)) {
        val image = imageService.processUploadImage(request)
        imagePreview = imageService.processUpload(user, Option(form.getUploadedImage), image, errors)

        imagePreview.foreach { img =>
          form.setUploadedImage(img.mainFile.getName)
        }
      }

      if (section.isImagepost && imagePreview.isEmpty) {
        errors.reject(null, "Изображение отсутствует")
      }
    }

    val poll: Option[Poll] = if (section.isPollPostAllowed) {
      Some(AddTopicController.preparePollPreview(form,user.getId))
    } else {
      None
    }

    val previewMsg: Topic = Topic.fromAddRequest(form, user, request.getRemoteAddr)

    val imageObject = imagePreview.map(i => Image(0, 0, "gallery/preview/" + i.mainFile.getName))

    val tagNames = TagName.parseAndSanitizeTags(form.getTags)

    if (!groupPermissionService.canCreateTag(section, user)) {
      val newTags = tagService.getNewTags(tagNames)

      if (newTags.nonEmpty) {
        errors.rejectValue("tags", null, "Вы не можете создавать новые теги (" + TagService.tagsToString(newTags.asJava) + ")")
      }
    }

    val preparedTopic =
      prepareService.prepareTopicPreview(previewMsg, TagService.namesToRefs(tagNames.asJava).asScala.toSeq, poll, message, imageObject)

    params.put("message", preparedTopic)

    val topicMenu = prepareService.getTopicMenu(preparedTopic, sessionUserOpt.map(_.user).orNull, tmpl.getProf, loadUserpics = true)
    params.put("topicMenu", topicMenu)

    if (!form.isPreviewMode && !errors.hasErrors) {
      CSRFProtectionService.checkCSRF(request, errors)
    }

    if (!form.isPreviewMode && !errors.hasErrors && sessionUserOpt.isEmpty || ipBlockInfo.isCaptchaRequired) {
      captcha.checkCaptcha(request, errors)
    }

    if (!form.isPreviewMode && !errors.hasErrors) {
      dupeProtector.checkDuplication(FloodProtector.Action.ADD_TOPIC, request.getRemoteAddr, user, errors)
    }

    if (!form.isPreviewMode && !errors.hasErrors) {
      session.removeAttribute("image")

      createNewTopic(request, form, group, params, section, user, message, imagePreview, previewMsg)
    } else {
      new ModelAndView("add", params.asJava)
    }
  }

  private def createNewTopic(request: HttpServletRequest, form: AddTopicRequest, group: Group,
                             params: mutable.Map[String, AnyRef], section: Section, user: User, message: MessageText,
                             scrn: Option[UploadedImagePreview], previewMsg: Topic) = {
    val (msgid, notifyUsers) = topicService.addMessage(request, form, message, group, user, scrn.orNull, previewMsg)

    if (!previewMsg.draft) {
      searchQueueSender.updateMessageOnly(msgid)
      RealtimeEventHub.notifyEvents(realtimeHubWS, notifyUsers)
    }

    val topicUrl = previewMsg.withId(msgid).getLink

    if (!section.isPremoderated || previewMsg.draft) {
      new ModelAndView(new RedirectView(topicUrl, false, false))
    } else {
      params.put("url", topicUrl)

      new ModelAndView("add-done-moderated", params.asJava)
    }
  }

  @RequestMapping(path = Array("/add-section.jsp"))
  def showForm(@RequestParam("section") sectionId: Int,
               @RequestParam(value = "tag", required = false) tag: String): ModelAndView = AuthorizedOpt { currentUser =>
    val section = sectionService.getSection(sectionId)

    if (tag != null) {
      TagName.checkTag(tag)
    }

    val groups = groupDao.getGroups(section)

    if (groups.size == 1) {
      new ModelAndView(new RedirectView(AddTopicController.getAddUrl(groups.get(0), tag)))
    } else {
      val params = prepareModel(None, currentUser.map(_.user), section).to(mutable.HashMap)

      params.put("groups", groups)

      if (tag != null) {
        params.put("tag", tag)
      }

      new ModelAndView("add-section", params.asJava)
    }
  }

  @InitBinder
  def initBinder(binder: WebDataBinder): Unit = {
    binder.registerCustomEditor(classOf[Group], new PropertyEditorSupport() {
      override def setAsText(text: String): Unit = {
        setValue(groupDao.getGroup(text.toInt))
      }

      override def getAsText: String = {
        if (getValue == null) {
          null
        } else {
          Integer.toString(getValue.asInstanceOf[Group].id)
        }
      }
    })

    binder.registerCustomEditor(classOf[User], new UserPropertyEditor(userService))
  }

  @InitBinder(Array("form"))
  def requestValidator(binder: WebDataBinder): Unit = {
    binder.setValidator(addTopicRequestValidator)
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor)
  }

  @ModelAttribute("modes")
  def getModes: java.util.Map[String, String] = AuthorizedOpt { currentUser =>
    val tmpl = Template.getTemplate
    MessageTextService.postingModeSelector(currentUser, tmpl.getFormatMode).asJava
  }
}