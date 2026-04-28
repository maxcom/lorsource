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

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import org.springframework.validation.{Errors, Validator}
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.{AccessViolationException, AuthorizedSession}
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.util.ExceptionBindingErrorProcessor

import javax.validation.Valid
import scala.beans.{BeanProperty, BooleanBeanProperty}

@Controller
class DeregisterController(userService: UserService) {
  private def checkUser(currentUser: AuthorizedSession): Unit = {
    if (currentUser.user.getScore < 100) {
      throw new AccessViolationException("Удаление аккаунта недоступно для пользователей со score < 100")
    }

    if (currentUser.user.isAdministrator || currentUser.moderator) {
      throw new AccessViolationException("Нельзя удалить модераторский аккаунт")
    }

    if (currentUser.user.isFrozen) {
      throw new AccessViolationException("Нельзя удалить замороженный аккаунт")
    }
  }

  @RequestMapping(value = Array("/deregister.jsp"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def show(@ModelAttribute("form") form: DeregisterRequest): ModelAndView = AuthorizedOnly { currentUser =>
    checkUser(currentUser)

    new ModelAndView("deregister")
  }

  @RequestMapping(value = Array("/deregister.jsp"), method = Array(RequestMethod.POST))
  def deregister(@Valid @ModelAttribute("form") form: DeregisterRequest, errors: Errors,
                 request: HttpServletRequest): ModelAndView = AuthorizedOnly { currentUser =>
    checkUser(currentUser)

    import currentUser.user

    if (!UserService.matchPassword(user, form.password)) {
      errors.rejectValue("password", null, "Неверный пароль")
    }

    if (errors.hasErrors) {
      new ModelAndView("deregister")
    } else {
      userService.deregister(user)

      new ModelAndView("action-done", "message", "Удаление пользователя прошло успешно.")
    }
  }

  @InitBinder(Array("form"))
  def requestValidator(binder: WebDataBinder): Unit = {
    binder.setValidator(new DeregisterRequestValidator)
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor)
  }
}

class DeregisterRequest(@BeanProperty var password: String, @BooleanBeanProperty var acceptBlock: Boolean,
                        @BooleanBeanProperty var acceptOneway: Boolean) {
  def this() = this(password = null, acceptBlock = false, acceptOneway = false)
}

class DeregisterRequestValidator extends Validator {
  override def supports(aClass: Class[?]): Boolean = classOf[DeregisterRequest] == aClass

  override def validate(o: AnyRef, errors: Errors): Unit = {
    val form = o.asInstanceOf[DeregisterRequest]

    if (!form.acceptBlock) {
      errors.reject("acceptBlock", null, "Вы не согласились с блокировкой аккаунта")
    }

    if (!form.acceptOneway) {
      errors.reject("acceptOneway", null, "Вы не согласились с невозможностью восстановления аккаунта")
    }
  }
}
