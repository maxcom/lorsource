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
package ru.org.linux.user

import com.google.common.base.Strings
import com.google.common.net.InternetDomainName
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import ru.org.linux.util.StringUtil

import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress

class RegisterRequestValidator(emailDomainsBlockDao: EmailDomainsBlockDao) extends Validator {
  protected val TOWN_LENGTH = 100
  protected val MIN_PASSWORD_LEN = 4

  protected def checkEmail(email: InternetAddress, errors: Errors): Unit = {
    if (!isGoodDomainEmail(email)) {
      errors.reject("email", "некорректный email домен")
    }
  }

  def isGoodDomainEmail(email: InternetAddress): Boolean = {
    val domain = email.getAddress.replaceFirst("^[^@]+@", "").toLowerCase

    try {
      val topDomain = InternetDomainName.from(domain).topPrivateDomain.toString
      !emailDomainsBlockDao.isBlocked(domain) && !emailDomainsBlockDao.isBlocked(topDomain)
    } catch {
      case _: IllegalStateException => false
    }
  }

  override def supports(clazz: Class[_]): Boolean = classOf[RegisterRequest] == clazz

  override def validate(target: AnyRef, errors: Errors): Unit = {
    val form = target.asInstanceOf[RegisterRequest]

    /*
    Nick validate
     */

    val nick = form.getNick

    if (Strings.isNullOrEmpty(nick)) {
      errors.rejectValue("nick", null, "не задан nick")
    }

    if (nick != null && !StringUtil.checkLoginName(nick)) {
      errors.rejectValue("nick", null, "некорректное имя пользователя")
    }

    if (nick != null && nick.length > UserConstants.MAX_NICK_LENGTH) {
      errors.rejectValue("nick", null, "слишком длинное имя пользователя")
    }

    /*
    Password validate
     */

    val password = Strings.emptyToNull(form.getPassword)
    val password2 = Strings.emptyToNull(form.getPassword2)

    if (Strings.isNullOrEmpty(password)) {
      errors.reject("password", null, "пароль не может быть пустым")
    }
    if (Strings.isNullOrEmpty(password2)) {
      errors.reject("password2", null, "пароль не может быть пустым")
    }

    if (password != null && password.equalsIgnoreCase(nick)) {
      errors.reject(password, null, "пароль не может совпадать с логином")
    }

    if (form.getPassword2 != null &&
      form.getPassword != null &&
      !form.getPassword.equals(form.getPassword2)) {
      errors.reject(null, "введенные пароли не совпадают")
    }

    if (!Strings.isNullOrEmpty(form.getPassword) && form.getPassword.length < MIN_PASSWORD_LEN) {
      errors.reject("password", null, "слишком короткий пароль, минимальная длина: " + MIN_PASSWORD_LEN)
    }

    /*
    Email validate
     */

    if (Strings.isNullOrEmpty(form.getEmail)) {
      errors.rejectValue("email", null, "Не указан e-mail")
    } else {
      try {
        val mail = new InternetAddress(form.getEmail)
        checkEmail(mail, errors)
      } catch {
        case e: AddressException =>
          errors.rejectValue("email", null, "Некорректный e-mail: " + e.getMessage)
      }
    }

    /*
    Rules validate
     */

    if (Strings.isNullOrEmpty(form.getRules) || !"okay".equals(form.getRules)) {
      errors.reject("rules", null, "Вы не согласились с правилами")
    }
  }
}
