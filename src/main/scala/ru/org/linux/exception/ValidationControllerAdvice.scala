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

import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.{ControllerAdvice, ExceptionHandler}
import org.springframework.web.servlet.ModelAndView

/** Ошибки валидации аргументов контроллеров (@Validated без параметра Errors) показываем дружелюбной страницей
  * errors/bad-parameter вместо дефолтного ответа 400 от DefaultHandlerExceptionResolver (который пишет WARN в лог).
  */
@ControllerAdvice
class ValidationControllerAdvice:
  @ExceptionHandler(Array(classOf[MethodArgumentNotValidException]))
  def methodArgumentNotValid(ex: MethodArgumentNotValidException): ModelAndView =
    new ModelAndView("errors/bad-parameter", "exception", ex)
