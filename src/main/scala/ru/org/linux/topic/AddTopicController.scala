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
import jakarta.servlet.ServletContext
import jakarta.servlet.http.HttpServletRequest
import org.apache.commons.io.IOUtils
import org.apache.pekko.actor.typed.ActorRef
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.util.UriComponentsBuilder
import ru.org.linux.auth.*
import ru.org.linux.auth.AuthUtil.{MaybeAuthorized, MaybeAuthorizedCtx}
import ru.org.linux.csrf.{CSRFNoAuto, CSRFProtectionService}
import ru.org.linux.gallery.UploadedImagePreview
import ru.org.linux.group.GroupPermissionService.TopicLimitInfo
import ru.org.linux.group.{Group, GroupPermissionService, GroupService}
import ru.org.linux.msgbase.MessageText
import ru.org.linux.poll.{Poll, PollVariant}
import ru.org.linux.realtime.RealtimeEventHub
import ru.org.linux.rights.{AddTopicChecker, Permission, TopicPublishChecker}
import ru.org.linux.search.SearchQueueSender
import ru.org.linux.section.SectionController.NonTech
import ru.org.linux.section.{Section, SectionNotFoundException, SectionService}
import ru.org.linux.tag.TagService.tagRef
import ru.org.linux.tag.{TagName, TagService}
import ru.org.linux.user.{User, UserPropertyEditor, UserService}
import ru.org.linux.util.ExceptionBindingErrorProcessor
import ru.org.linux.util.markdown.MarkdownFormatter

import java.beans.PropertyEditorSupport
import java.nio.charset.StandardCharsets
import javax.annotation.Nullable
import javax.validation.Valid
import scala.beans.{BeanProperty, BooleanBeanProperty}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

@Controller
object AddTopicController:
  case class SectionChoice(
      @BeanProperty
      section: Section,
      @BeanProperty
      url: String,
      @BooleanBeanProperty
      postable: Boolean,
      @BeanProperty
      postScoreInfo: String)
  case class GroupChoice(
      @BeanProperty
      group: Group,
      @BeanProperty
      addUrl: String,
      @BooleanBeanProperty
      postable: Boolean,
      @BeanProperty
      postScoreInfo: String)

  private val MaxMessageLengthAnonymous = 8196
  private val MaxMessageLength = 65536

  private def preparePollPreview(form: AddTopicRequest): Poll =
    val variants = form.poll.filterNot(Strings.isNullOrEmpty).map(item => PollVariant(0, item))

    Poll(0, 0, form.multiSelect, variants.toVector)

  def getAddUrl(section: Section, tag: String): String =
    val builder = UriComponentsBuilder.fromPath("/add-section.jsp")
    builder.queryParam("section", section.id)
    builder.queryParam("tag", tag)
    builder.build.toUriString

  def getAddUrl(section: Section): String =
    val builder = UriComponentsBuilder.fromPath("/add-section.jsp")
    builder.queryParam("section", section.id)
    builder.build.toUriString

  def getAddUrl(group: Group): String = getAddUrl(group, null)

  def getAddUrl(
      group: Group,
      @Nullable
      tag: String,
      noinfo: Boolean = false): String =
    val builder = UriComponentsBuilder.fromPath("/add.jsp")
    builder.queryParam("group", group.id)
    if tag != null then
      builder.queryParam("tags", tag)
    if noinfo then
      builder.queryParam("noinfo", 1)
    builder.build.toUriString

@Controller
class AddTopicController(
                          searchQueueSender: SearchQueueSender,
                          captcha: CaptchaService,
                          sectionService: SectionService,
                          tagService: TagService,
                          userService: UserService,
                          prepareService: TopicPrepareService,
                          permissionService: GroupPermissionService,
                          addTopicRequestValidator: AddTopicRequestValidator,
                          topicService: TopicService,
                          @Qualifier("realtimeHubWS")
                          realtimeHubWS: ActorRef[RealtimeEventHub.Protocol],
                          renderService: MarkdownFormatter,
                          groupService: GroupService,
                          dupeProtector: FloodProtector,
                          servletContext: ServletContext,
                          addTopicChecker: AddTopicChecker,
                          topicPublishChecker: TopicPublishChecker,
                          passwordEncoder: PasswordEncoder):
  @RequestMapping(value = Array("/add.jsp"), method = Array(RequestMethod.GET))
  def add(
      @Valid @ModelAttribute("form")
      form: AddTopicRequest): ModelAndView =
    MaybeAuthorizedCtx {
      val group = form.group

      addTopicChecker.checkTopicPosting(group).checkOrThrow("Недостаточно прав для создания топика")

      val section = sectionService.getSection(form.group.sectionId)

      form.uploadedImages = new Array[String](permissionService.imageLimit(section))

      val topicLimitInfo = permissionService.topicLimitInfo(section)
      val topicPostingCheck = topicPublishChecker.checkPublish(group, topicLimitInfo)

      val params = prepareModelForm(Some(form.group), section, topicLimitInfo, topicPostingCheck)

      new ModelAndView("add", params.asJava)
    }

  private def prepareModelForm(group: Option[Group], section: Section, topicLimitInfo: TopicLimitInfo,
                               topicPostingCheck: Permission)
                              (using AnySession): Map[String, AnyRef] =
    prepareModel(group, section) +
      ("topicLimitInfo" -> topicLimitInfo) +
      ("topicPostingAllowed" -> Boolean.box(topicPostingCheck.permitted)) +
      ("topicPostingReason" -> topicPostingCheck.reason)

  private def prepareModel(group: Option[Group], section: Section)(using AnySession): Map[String, AnyRef] =
    val params = Map.newBuilder[String, AnyRef]

    val helpResource = servletContext.getResource("/help/new-topic-" + Section.getUrlName(section.id) + ".md")
    if helpResource != null then
      val helpRawText = IOUtils.toString(helpResource, StandardCharsets.UTF_8)
      val addInfo = renderService.renderToHtml(helpRawText, nofollow = false)
      params.addOne("addportal" -> addInfo)

    params.addOne("sectionId" -> Integer.valueOf(section.id))
    params.addOne("section" -> section)

    group.foreach { group =>
      params.addOne("group" -> group)
      params.addOne("showAllowAnonymous" -> Boolean.box(permissionService.enableAllowAnonymousCheckbox(group)))
      params.addOne("imagepost" -> Boolean.box(permissionService.isImagePostingAllowed(section)))
    }

    params.result()

  @RequestMapping(value = Array("/add.jsp"), method = Array(RequestMethod.POST)) @CSRFNoAuto
  def doAdd(
      request: HttpServletRequest,
      @Valid @ModelAttribute("form")
      form: AddTopicRequest,
      errors: BindingResult,
      @RequestAttribute("captchaRequired")
      captchaRequired: Boolean): ModelAndView =
    MaybeAuthorized { sessionUserOpt =>
      // не используем implicit, так как есть sessionUser и postingUser
      val group = form.group
      val section = sectionService.getSection(group.sectionId)


      if !form.isPreviewMode && !errors.hasErrors && captchaRequired then
        captcha.checkCaptcha(request, errors)

      val postingUser = AuthUtil.postingUser(sessionUserOpt, Option(form.nick), Option(form.password), errors, passwordEncoder, request)
      val user = postingUser.user

      val postingCheck = addTopicChecker.checkTopicPosting(group)(using postingUser)

      postingCheck.checkOrError(errors, "Недостаточно прав для создания топика")

      val topicLimitInfo = permissionService.topicLimitInfo(section)(using postingUser)
      val topicPostingCheck = topicPublishChecker.checkPublish(group, topicLimitInfo)(using postingUser)

      val params = prepareModelForm(Some(group), section, topicLimitInfo, topicPostingCheck)
        (using sessionUserOpt).to(mutable.HashMap)

      if !permissionService.enableAllowAnonymousCheckbox(group)(using postingUser) then
        form.allowAnonymous = true

      val message = MessageText(Strings.nullToEmpty(form.msg), sessionUserOpt.profile.formatMode)

      if !postingUser.authorized then
        if message.text.length > AddTopicController.MaxMessageLengthAnonymous then
          errors.rejectValue("msg", null, "Слишком большое сообщение")
      else if message.text.length > AddTopicController.MaxMessageLength then
        errors.rejectValue("msg", null, "Слишком большое сообщение")

      val imagePreviews =
        postingUser.opt match
          case Some(authorized) =>
            topicService.processUploads(form, group, errors)(using authorized)
          case None =>
            Seq.empty

      val poll: Option[Poll] =
        if section.isPollPostAllowed then
          Some(AddTopicController.preparePollPreview(form))
        else
          None

      val previewMsg: Topic = Topic.fromAddRequest(form, user, request.getRemoteAddr)

      val tagNames = TagName.parseAndSanitizeTags(form.tags)

      if !permissionService.canCreateTag(section)(using postingUser) then
        val newTags = tagService.getNewTags(tagNames)

        if newTags.nonEmpty then
          errors.rejectValue(
            "tags",
            null,
            "Вы не можете создавать новые теги (" + TagService.tagsToString(newTags) + ")")

      val preparedTopic = prepareService.prepareTopicPreview(
        previewMsg,
        tagNames.map(tagRef),
        poll,
        message,
        imagePreviews)(using postingUser)

      params.put("message", preparedTopic)

      val topicMenu = prepareService.getTopicMenu(preparedTopic, loadUserpics = true)(using sessionUserOpt)
      params.put("topicMenu", topicMenu)

      if !form.isPreviewMode && !errors.hasErrors then
        CSRFProtectionService.checkCSRF(request, errors)

      if !form.isPreviewMode && !errors.hasErrors then
        dupeProtector.checkRateLimit(FloodProtector.AddTopic, request.getRemoteAddr, postingUser.userOpt.orNull, errors)

      if !errors.hasErrors && !form.isDraftMode && !form.isPreviewMode then
        topicPostingCheck.checkOrError(errors)

      if !form.isPreviewMode && !errors.hasErrors then
        createNewTopic(
          request,
          form,
          group,
          params,
          section,
          user,
          message,
          imagePreviews,
          previewMsg)
      else
        new ModelAndView("add", params.asJava)
    }

  private def createNewTopic(
      request: HttpServletRequest,
      form: AddTopicRequest,
      group: Group,
      params: mutable.Map[String, AnyRef],
      section: Section,
      user: User,
      message: MessageText,
      images: Seq[UploadedImagePreview],
      previewMsg: Topic) =
    val (msgid, notifyUsers) = topicService.addMessage(
      request,
      form,
      message,
      group,
      user,
      images,
      previewMsg)

    if !previewMsg.draft then
      searchQueueSender.updateMessageOnly(msgid)
      RealtimeEventHub.notifyEvents(realtimeHubWS, notifyUsers)

    val topicUrl = previewMsg.withId(msgid).getLink

    if !section.premoderated || previewMsg.draft then
      new ModelAndView(new RedirectView(topicUrl, false, false))
    else
      params.put("url", topicUrl)

      new ModelAndView("add-done-moderated", params.asJava)

  @RequestMapping(path = Array("/add-section.jsp"), params = Array("section"))
  def showFormWithSection(
      @RequestParam("section")
      sectionId: Int,
      @RequestParam(value = "tag", required = false)
      tag: String): ModelAndView =
    MaybeAuthorizedCtx {
      val section = sectionService.getSection(sectionId)

      if tag != null then
        TagName.checkTag(tag)

      val groups = groupService.getGroups(section)

      if groups.size == 1 then
        val group = groups.head

        val postable = addTopicChecker.checkTopicPosting(group)

        if postable.permitted then
          new ModelAndView(new RedirectView(AddTopicController.getAddUrl(group, tag)))
        else
          val params = prepareModel(None, section).to(mutable.HashMap)

          params.put(
            "groups",
            Seq(
              AddTopicController.GroupChoice(
                group,
                AddTopicController.getAddUrl(group, tag, noinfo = true),
                false,
                postable.reason)).asJava)

          if tag != null then
            params.put("tag", tag)

          new ModelAndView("add-section", params.asJava)
      else
        val params = prepareModel(None, section).to(mutable.HashMap)

        val groupChoices = groups.map { group =>
          val postable = addTopicChecker.checkTopicPosting(group)

          AddTopicController.GroupChoice(
            group,
            AddTopicController.getAddUrl(group, tag, noinfo = true),
            postable.permitted,
            postable.reason)
        }

        if section.id == Section.Forum then
          val (other, tech) = groupChoices.partition(g => NonTech.contains(g.group.id))

          params.put("tech", tech.asJava)
          params.put("other", other.asJava)
        else
          params.put("groups", groupChoices.asJava)


        if tag != null then
          params.put("tag", tag)

        new ModelAndView("add-section", params.asJava)
    }

  @RequestMapping(path = Array("/add-section.jsp"), params = Array("!section"))
  def showFormAllSections(
      @RequestParam(value = "tag", required = false)
      tag: String): ModelAndView =
    MaybeAuthorizedCtx {
      val sectionList = sectionService
        .sections
        .map { section =>
          val groups = groupService.getGroups(section)

          val postable =
            if groups.size == 1 then
              addTopicChecker.checkTopicPosting(groups.head)
            else
              addTopicChecker.checkTopicPosting(section)

          val url =
            if groups.size == 1 then
              AddTopicController.getAddUrl(groups.head, tag)
            else
              val builder = UriComponentsBuilder.fromPath("/add-section.jsp")
              builder.queryParam("section", section.id)
              if tag != null then
                builder.queryParam("tag", tag)
              builder.build.toUriString

          AddTopicController.SectionChoice(section, url, postable.permitted, postable.reason)
        }

      val params = mutable.HashMap[String, AnyRef]("sectionList" -> sectionList.asJava)

      if tag != null then
        params.put("tag", tag)

      new ModelAndView("add-section", params.asJava)
    }

  @InitBinder
  def initBinder(binder: WebDataBinder): Unit =
    binder.registerCustomEditor(
      classOf[Group],
      new PropertyEditorSupport():
        override def setAsText(text: String): Unit = setValue(groupService.getGroup(text.toInt))

        override def getAsText: String =
          if getValue == null then
            null
          else
            getValue.asInstanceOf[Group].id.toString
    )

    binder.registerCustomEditor(classOf[User], new UserPropertyEditor(userService))

  @ExceptionHandler(Array(classOf[SectionNotFoundException])) @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleNotFoundException: ModelAndView = new ModelAndView("errors/code404")

  @InitBinder(Array("form"))
  def requestValidator(binder: WebDataBinder): Unit =
    binder.setValidator(addTopicRequestValidator)
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor)
