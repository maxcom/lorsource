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

import org.apache.pekko.actor.typed.ActorRef
import com.google.common.base.Strings
import jakarta.servlet.ServletContext
import jakarta.servlet.http.HttpServletRequest
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
import ru.org.linux.auth.AuthUtil.MaybeAuthorized
import ru.org.linux.csrf.{CSRFNoAuto, CSRFProtectionService}
import ru.org.linux.gallery.UploadedImagePreview
import ru.org.linux.group.{Group, GroupDao, GroupPermissionService}
import ru.org.linux.markup.MarkupType.ofFormId
import ru.org.linux.markup.MessageTextService
import ru.org.linux.poll.{Poll, PollVariant}
import ru.org.linux.realtime.RealtimeEventHub
import ru.org.linux.search.SearchQueueSender
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.spring.dao.MessageText
import ru.org.linux.tag.TagService.tagRef
import ru.org.linux.tag.{TagName, TagService}
import ru.org.linux.user.{User, UserPropertyEditor, UserService}
import ru.org.linux.util.ExceptionBindingErrorProcessor
import ru.org.linux.util.markdown.MarkdownFormatter

import java.beans.PropertyEditorSupport
import java.nio.charset.StandardCharsets
import javax.annotation.Nullable
import javax.validation.Valid
import scala.collection.mutable
import scala.jdk.CollectionConverters.MapHasAsJava

@Controller
object AddTopicController {
  private val MaxMessageLengthAnonymous = 8196
  private val MaxMessageLength = 65536

  private def preparePollPreview(form: AddTopicRequest): Poll = {
    val variants = form.poll.filterNot(Strings.isNullOrEmpty).map(item => PollVariant(0, item))

    Poll(0, 0, form.multiSelect, variants.toVector)
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
                         permissionService: GroupPermissionService, addTopicRequestValidator: AddTopicRequestValidator,
                         topicService: TopicService,
                         @Qualifier("realtimeHubWS") realtimeHubWS: ActorRef[RealtimeEventHub.Protocol],
                         renderService: MarkdownFormatter, groupDao: GroupDao, dupeProtector: FloodProtector,
                         ipBlockDao: IPBlockDao, servletContext: ServletContext) {
  @ModelAttribute("ipBlockInfo")
  def loadIPBlock(request: HttpServletRequest): IPBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr)

  @RequestMapping(value = Array("/add.jsp"), method = Array(RequestMethod.GET))
  def add(@Valid @ModelAttribute("form") form: AddTopicRequest): ModelAndView = MaybeAuthorized { implicit currentUser =>
    val group = form.group

    if (currentUser.authorized && !permissionService.isTopicPostingAllowed(group)) {
      val errorView = new ModelAndView("errors/good-penguin")

      errorView.addObject("msgHeader", "Недостаточно прав для постинга тем в эту группу")
      errorView.addObject("msgMessage", permissionService.getPostScoreInfo(group))

      errorView
    } else {
      val section = sectionService.getSection(form.group.sectionId)

      form.additionalUploadedImages=new Array[String](permissionService.additionalImageLimit(section))

      val params = prepareModel(Some(form.group), section)

      new ModelAndView("add", params.asJava)
    }
  }

  private def prepareModel(group: Option[Group], section: Section)
                          (implicit currentUser: AnySession): Map[String, AnyRef] = {
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
      params.addOne("postscoreInfo" -> permissionService.getPostScoreInfo(group))
      params.addOne("showAllowAnonymous" -> Boolean.box(permissionService.enableAllowAnonymousCheckbox(group)))
      params.addOne("imagepost" -> Boolean.box(permissionService.isImagePostingAllowed(section)))
    }

    params.result()
  }

  @RequestMapping(value = Array("/add.jsp"), method = Array(RequestMethod.POST))
  @CSRFNoAuto
  def doAdd(request: HttpServletRequest, @Valid @ModelAttribute("form") form: AddTopicRequest, errors: BindingResult,
            @ModelAttribute("ipBlockInfo") ipBlockInfo: IPBlockInfo): ModelAndView = MaybeAuthorized { sessionUserOpt =>
    // не используем implicit, так как есть sessionUser и postingUser
    val group = form.group
    val section = sectionService.getSection(group.sectionId)

    val params = prepareModel(Some(group), section)(sessionUserOpt).to(mutable.HashMap)

    val postingUser = AuthUtil.postingUser(sessionUserOpt, Option(form.nick), Option(form.password), errors)
    val user = postingUser.userOpt.getOrElse(userService.getAnonymous)

    user.checkFrozen(errors)

    IPBlockDao.checkBlockIP(ipBlockInfo, errors, postingUser.userOpt.orNull)

    if (!permissionService.isTopicPostingAllowed(group)(postingUser)) {
      errors.reject(null, "Недостаточно прав для постинга тем в эту группу")
    }

    if (!permissionService.enableAllowAnonymousCheckbox(group)(postingUser)) {
      form.allowAnonymous=true
    }

    val message = MessageText(Strings.nullToEmpty(form.msg), sessionUserOpt.profile.formatMode)

    if (!postingUser.authorized) {
      if (message.text.length > AddTopicController.MaxMessageLengthAnonymous) {
        errors.rejectValue("msg", null, "Слишком большое сообщение")
      }
    } else {
      if (message.text.length > AddTopicController.MaxMessageLength) {
        errors.rejectValue("msg", null, "Слишком большое сообщение")
      }
    }

    val (imagePreview, additionalImagePreviews) = postingUser.opt match {
      case Some(authorized) =>
        topicService.processUploads(form, group, errors)(authorized)
      case None =>
        (None, Seq.empty)
    }

    val poll: Option[Poll] = if (section.isPollPostAllowed) {
      Some(AddTopicController.preparePollPreview(form))
    } else {
      None
    }

    val previewMsg: Topic = Topic.fromAddRequest(form, user, request.getRemoteAddr)

    val tagNames = TagName.parseAndSanitizeTags(form.tags)

    if (!permissionService.canCreateTag(section)(postingUser)) {
      val newTags = tagService.getNewTags(tagNames)

      if (newTags.nonEmpty) {
        errors.rejectValue("tags", null, "Вы не можете создавать новые теги (" + TagService.tagsToString(newTags) + ")")
      }
    }

    val preparedTopic = prepareService.prepareTopicPreview(previewMsg, tagNames.map(tagRef), poll, message, imagePreview,
      additionalImagePreviews)

    params.put("message", preparedTopic)

    val topicMenu = prepareService.getTopicMenu(preparedTopic, loadUserpics = true)(sessionUserOpt)
    params.put("topicMenu", topicMenu)

    if (!form.isPreviewMode && !errors.hasErrors) {
      CSRFProtectionService.checkCSRF(request, errors)
    }

    if (!form.isPreviewMode && !errors.hasErrors && !sessionUserOpt.authorized || ipBlockInfo.isCaptchaRequired) {
      captcha.checkCaptcha(request, errors)
    }

    if (!form.isPreviewMode && !errors.hasErrors) {
      dupeProtector.checkRateLimit(FloodProtector.AddTopic, request.getRemoteAddr, postingUser.userOpt.orNull, errors)
    }

    if (!form.isPreviewMode && !errors.hasErrors) {
      createNewTopic(request, form, group, params, section, user, message, imagePreview, additionalImagePreviews, previewMsg)
    } else {
      new ModelAndView("add", params.asJava)
    }

  }

  private def createNewTopic(request: HttpServletRequest, form: AddTopicRequest, group: Group,
                             params: mutable.Map[String, AnyRef], section: Section, user: User, message: MessageText,
                             image: Option[UploadedImagePreview], additionalImages: Seq[UploadedImagePreview],
                             previewMsg: Topic) = {
    val (msgid, notifyUsers) = topicService.addMessage(request, form, message, group, user, image, additionalImages, previewMsg)

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
               @RequestParam(value = "tag", required = false) tag: String): ModelAndView = MaybeAuthorized { implicit currentUser =>
    val section = sectionService.getSection(sectionId)

    if (tag != null) {
      TagName.checkTag(tag)
    }

    val groups = groupDao.getGroups(section)

    if (groups.size == 1) {
      new ModelAndView(new RedirectView(AddTopicController.getAddUrl(groups.get(0), tag)))
    } else {
      val params = prepareModel(None, section).to(mutable.HashMap)

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
          getValue.asInstanceOf[Group].id.toString
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
}