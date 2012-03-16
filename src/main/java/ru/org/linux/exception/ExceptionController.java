/*
 * Copyright 1998-2012 Linux.org.ru
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
package ru.org.linux.exception;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class ExceptionController {

  @Autowired
  private ExceptionResolver exceptionResolver;

  @RequestMapping(value = "/ExceptionResolver", method = RequestMethod.GET)
  public ModelAndView defaultExceptionHandler(
    HttpServletRequest request,
    HttpServletResponse response,
    Object handler
  ) {
    Exception ex = (Exception) request.getAttribute("javax.servlet.error.exception");
    if (ex == null) {
      return new ModelAndView(new RedirectView("/"));
    }
    return exceptionResolver.resolveException(request, response, handler, ex);
  }

}
