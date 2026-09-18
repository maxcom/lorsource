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
package ru.org.linux.exception

import jakarta.servlet.http.HttpServletRequest
import munit.FunSuite
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.email.EmailService
import ru.org.linux.site.ScriptErrorException
import ru.org.linux.user.UserErrorException

/** Юнит-тесты анти-раскрытия информации на странице ошибки ([[ExceptionResolver]] / errors/common.jsp).
  *
  * Для непредвиденных исключений (ExceptionType.OTHER) пользователь должен получать generic-сообщение без internals
  * (CWE-209): без текста исключения (имена таблиц/констрейнтов БД, пути файловой системы) и без имени класса
  * исключения. Сообщения UserErrorException/ScriptErrorException предназначены пользователю и должны отображаться как
  * есть (с HTML-экранированием).
  */
class ExceptionResolverTest extends FunSuite:
  private val emailServiceStub =
    new EmailService(null, null, null):
      override def sendExceptionReport(
          request: HttpServletRequest,
          exception: Exception,
          currentUser: ru.org.linux.user.User): String = "Администраторы получили об этом сигнал."

  private def resolve(exception: Exception): (ModelAndView, MockHttpServletResponse) =
    val resolver = new ExceptionResolver
    val emailServiceField = classOf[ExceptionResolver].getDeclaredField("emailService")
    emailServiceField.setAccessible(true)
    emailServiceField.set(resolver, emailServiceStub)

    val request = MockHttpServletRequest("GET", "/ExceptionResolver")
    val response = MockHttpServletResponse()
    val modelAndView = resolver.doResolveException(request, response, null, exception)
    (modelAndView, response)

  test("непредвиденное исключение: generic-сообщение без деталей исключения") {
    val (modelAndView, response) = resolve(
      new RuntimeException("ERROR: duplicate key value violates unique constraint \"users_pkey\" /var/lib/tomcat"))

    assertEquals(modelAndView.getViewName, "errors/common")
    assertEquals(modelAndView.getModel.get("exceptionType"), "OTHER")
    assertEquals(modelAndView.getModel.get("errorMessage"), "Произошла внутренняя ошибка сервера")
    assertEquals(modelAndView.getModel.get("headTitle"), "внутренняя ошибка")
    assert(modelAndView.getModel.containsKey("infoMessage"))
    assertEquals(response.getStatus, 500)
  }

  test("непредвиденное исключение: имя класса не попадает в модель") {
    val (modelAndView, _) = resolve(new NullPointerException("secret detail"))

    val errorMessage = modelAndView.getModel.get("errorMessage").toString
    val headTitle = modelAndView.getModel.get("headTitle").toString
    assert(!errorMessage.contains("NullPointerException"))
    assert(!errorMessage.contains("secret"))
    assert(!headTitle.contains("NullPointerException"))
  }

  test("UserErrorException: пользовательское сообщение отображается") {
    val (modelAndView, response) = resolve(UserErrorException("Сообщение уже удалено"))

    assertEquals(modelAndView.getModel.get("exceptionType"), "IGNORED")
    assertEquals(modelAndView.getModel.get("errorMessage"), "Сообщение уже удалено")
    assertEquals(modelAndView.getModel.get("headTitle"), "неверный запрос")
    assert(!modelAndView.getModel.containsKey("infoMessage"))
    assertEquals(response.getStatus, 500)
  }

  test("ScriptErrorException: пользовательское сообщение отображается") {
    val (modelAndView, response) = resolve(ScriptErrorException("Нельзя удалить комментарий с ответами"))

    assertEquals(modelAndView.getModel.get("exceptionType"), "SCRIPT_ERROR")
    assertEquals(modelAndView.getModel.get("errorMessage"), "Нельзя удалить комментарий с ответами")
    assertEquals(modelAndView.getModel.get("headTitle"), "неверный запрос")
    assert(!modelAndView.getModel.containsKey("infoMessage"))
    assertEquals(response.getStatus, 500)
  }

  test("пользовательское сообщение HTML-экранируется") {
    val (modelAndView, _) = resolve(UserErrorException("Некорректный <тег> & \"значение\""))

    val errorMessage = modelAndView.getModel.get("errorMessage").toString
    assert(!errorMessage.contains("<тег>"))
    assert(errorMessage.contains("&lt;"))
  }
