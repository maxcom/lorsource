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
package ru.org.linux.group

import com.typesafe.scalalogging.StrictLogging
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AuthUtil.ModeratorOnly

import scala.jdk.CollectionConverters.MapHasAsJava

@Controller
class GroupModificationController(groupService: GroupService, prepareService: GroupInfoPrepareService)
    extends StrictLogging:
  @RequestMapping(value = Array("/groupmod.jsp"), method = Array(RequestMethod.GET))
  def showForm(
      @RequestParam("group")
      id: Int): ModelAndView =
    ModeratorOnly { _ =>
      val group = groupService.getGroup(id)

      val mv = new ModelAndView("groupmod", "group", group)

      mv.getModel.put("groupInfo", prepareService.prepareGroupInfo(group))

      mv
    }

  @RequestMapping(value = Array("/groupmod.jsp"), method = Array(RequestMethod.POST))
  def modifyGroup(
      @RequestParam("group")
      id: Int,
      @RequestParam("title")
      title: String,
      @RequestParam("info")
      info: String,
      @RequestParam("urlName")
      urlName: String,
      @RequestParam("longinfo")
      longInfo: String,
      @RequestParam(value = "preview", required = false)
      preview: String,
      @RequestParam(value = "resolvable", required = false)
      resolvable: String): ModelAndView =
    ModeratorOnly { currentUser =>
      val existingGroup = groupService.getGroup(id)

      // title и urlName может менять только администратор; модератору игнорируем отправленные значения
      val isAdmin = currentUser.administrator
      val effectiveTitle =
        if isAdmin then
          title
        else
          existingGroup.title
      val effectiveUrlName =
        if isAdmin then
          urlName
        else
          existingGroup.urlName

      // сохраняем введённые значения в копии группы, чтобы вернуть их в форму
      val group = existingGroup.copy(
        title = effectiveTitle,
        info = info,
        longInfo = longInfo,
        urlName = effectiveUrlName,
        resolvable = resolvable != null)

      if preview != null then
        new ModelAndView(
          "groupmod",
          Map[String, Any]("group" -> group, "groupInfo" -> prepareService.prepareGroupInfo(group), "preview" -> true)
            .asJava)
      else
        val validationError = GroupModificationController.validateUrlName(effectiveUrlName)

        if validationError.isDefined then
          new ModelAndView(
            "groupmod",
            Map[String, Any](
              "group" -> group,
              "groupInfo" -> prepareService.prepareGroupInfo(group),
              "error" -> validationError.get).asJava)
        else
          groupService.setParams(group, effectiveTitle, info, longInfo, resolvable != null, effectiveUrlName)

          logger.info("Настройки группы {} изменены {}", existingGroup.urlName, currentUser.user.nick)

          new ModelAndView("action-done", "message", "Параметры изменены")
    }

object GroupModificationController:
  def validateUrlName(urlName: String): Option[String] =
    if urlName == null || urlName.isEmpty then
      Some("Имя для URL не может быть пустым")
    else if urlName.contains('/') then
      Some("Имя для URL не может содержать символ '/'")
    else if !urlName.forall(isUrlNameChar) then
      Some("Имя для URL может содержать только латинские буквы, цифры, дефис и подчёркивание")
    else
      None

  private def isUrlNameChar(c: Char): Boolean =
    (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_'
