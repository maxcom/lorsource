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
package ru.org.linux.exception

import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView

@Controller
class ExceptionController(exceptionResolver: ExceptionResolver) {
  @RequestMapping(Array("/ExceptionResolver"))
  def defaultExceptionHandler(request: HttpServletRequest, response: HttpServletResponse, handler: AnyRef): ModelAndView = {
    val ex = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION).asInstanceOf[Throwable]

    if (ex == null) {
      new ModelAndView(new RedirectView("/"))
    } else if (!ex.isInstanceOf[Exception]) {
      exceptionResolver.resolveException(request, response, handler, new RuntimeException(ex))
    } else {
      exceptionResolver.resolveException(request, response, handler, ex.asInstanceOf[Exception])
    }
  }
}