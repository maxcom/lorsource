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

import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpStatus
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.{ControllerAdvice, ExceptionHandler}
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.ModelAndView

/** Клиентские ошибки Spring MVC показываем дружелюбными страницами сайта вместо дефолтных ответов
  * DefaultHandlerExceptionResolver (sendError + WARN в лог + голая страница контейнера).
  *
  * Advice виден ExceptionHandlerExceptionResolver внутри composite-резолвера (order=0), создаваемого @EnableWebMvc,
  * поэтому срабатывает раньше DefaultHandlerExceptionResolver.
  */
@ControllerAdvice
class MvcClientErrorAdvice:
  /** Ошибка валидации @Validated-аргумента без Errors-параметра (например, GET формы по устаревшей теме). */
  @ExceptionHandler(Array(classOf[MethodArgumentNotValidException]))
  def methodArgumentNotValid(ex: MethodArgumentNotValidException, response: HttpServletResponse): ModelAndView =
    badParameter(ex, response)

  /** Не хватает обязательных параметров: @RequestParam/@RequestHeader/@RequestCookie без значения, несоответствие
    * условиям params=... в @RequestMapping (MissingServletRequestParameterException,
    * UnsatisfiedServletRequestParameterException, MissingServletRequestPartException и пр.).
    */
  @ExceptionHandler(Array(classOf[ServletRequestBindingException]))
  def requestBinding(ex: ServletRequestBindingException, response: HttpServletResponse): ModelAndView =
    badParameter(ex, response)

  /** Неконвертируемое значение параметра (в т.ч. MethodArgumentTypeMismatchException). */
  @ExceptionHandler(Array(classOf[TypeMismatchException]))
  def typeMismatch(ex: TypeMismatchException, response: HttpServletResponse): ModelAndView = badParameter(ex, response)

  /** Нарушение ограничений валидации на параметрах обработчика. */
  @ExceptionHandler(Array(classOf[HandlerMethodValidationException]))
  def handlerMethodValidation(ex: HandlerMethodValidationException, response: HttpServletResponse): ModelAndView =
    badParameter(ex, response)

  /** Метод запроса не поддерживается: страница 405, заголовок Allow сохраняем (как это делал DHER). */
  @ExceptionHandler(Array(classOf[HttpRequestMethodNotSupportedException]))
  def methodNotSupported(ex: HttpRequestMethodNotSupportedException, response: HttpServletResponse): ModelAndView =
    response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value())

    ex.getHeaders.forEach((name, values) => values.forEach(value => response.addHeader(name, value)))

    new ModelAndView("errors/code405", "exception", ex)
  end methodNotSupported

  private def badParameter(ex: Exception, response: HttpServletResponse): ModelAndView =
    response.setStatus(HttpStatus.BAD_REQUEST.value())

    new ModelAndView("errors/bad-parameter", "exception", ex)
