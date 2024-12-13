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
package ru.org.linux.user

import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.{BindingResult, FieldError, ValidationUtils}
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.site.DefaultProfile

import scala.beans.BeanProperty
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.{ListHasAsScala, MutableSeqHasAsJava}

class EditBoxesRequest(@BeanProperty var position: Integer = null, @BeanProperty var boxName: String = null)

@Controller
class AddRemoveBoxesController(profileDao: ProfileDao) {
  @RequestMapping(value = Array("/remove-box.jsp", "/add-box.jsp"), method = Array(RequestMethod.GET))
  @throws[AccessViolationException]
  def showRemove(@RequestParam(required = false) pos: Integer): ModelMap = AuthorizedOnly { _ =>
    val result = new ModelMap

    val form = new EditBoxesRequest()
    form.position = pos

    result.addAttribute("form", form)

    result
  }

  @RequestMapping(value = Array("/remove-box.jsp"), method = Array(RequestMethod.POST))
  def doRemove(@ModelAttribute("form") form: EditBoxesRequest, result: BindingResult): String = AuthorizedOnly { currentUser =>
    ValidationUtils.rejectIfEmptyOrWhitespace(result, "position", "position.empty", "Не указанa позиция бокслета")

    if (result.hasErrors) {
      "remove-box"
    } else {
      val boxlets = currentUser.profile.getBoxlets.asScala.to(ArrayBuffer)

      if (boxlets.size > form.position) {
        boxlets.remove(form.position.intValue)

        val builder = new ProfileBuilder(currentUser.profile)
        builder.setBoxlets(boxlets.asJava)

        profileDao.writeProfile(currentUser.user, builder)
      }

      "redirect:/edit-boxes.jsp"
    }
  }

  @ModelAttribute("allboxes")
  def getAllBoxes: java.util.Map[String, String] = DefaultProfile.getAllBoxes

  @RequestMapping(value = Array("/add-box.jsp"), method = Array(RequestMethod.POST))
  def doAdd(@ModelAttribute("form") form: EditBoxesRequest, result: BindingResult): String = AuthorizedOnly { currentUser =>
    ValidationUtils.rejectIfEmptyOrWhitespace(result, "boxName", "boxName.empty", "Не выбран бокслет")

    if (StringUtils.isNotEmpty(form.boxName) && !DefaultProfile.isBox(form.boxName)) {
      result.addError(new FieldError("boxName", "boxName.invalid", "Неверный бокслет"))
    }

    if (result.hasErrors) {
      "add-box"
    } else {
      if (form.position == null) {
        form.position = 0
      }

      val boxlets = currentUser.profile.getBoxlets.asScala.view.filter(DefaultProfile.isBox).to(ArrayBuffer)

      if (boxlets.size > form.position) {
        boxlets.insert(form.position, form.boxName)
      } else {
        boxlets.addOne(form.boxName)
      }

      val builder = new ProfileBuilder(currentUser.profile)

      builder.setBoxlets(boxlets.asJava)

      profileDao.writeProfile(currentUser.user, builder)

      "redirect:/edit-boxes.jsp"
    }
  }

  @RequestMapping(Array("/edit-boxes.jsp"))
  def view = new ModelAndView("edit-boxes")
}