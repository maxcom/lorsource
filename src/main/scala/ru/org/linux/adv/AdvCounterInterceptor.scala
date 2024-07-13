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
package ru.org.linux.adv

import com.typesafe.scalalogging.StrictLogging
import org.springframework.web.servlet.{HandlerInterceptor, ModelAndView}

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

class AdvCounterInterceptor(advCounterDao: AdvCounterDao) extends HandlerInterceptor with StrictLogging {
  override def postHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any,
                          modelAndView: ModelAndView): Unit = {
    if (response.getStatus < 400 && response.getStatus >= 200) {
      val path = request.getRequestURI

      logger.debug(s"Adv counter: ${path}")

      advCounterDao.count(path, 1)
    }
  }
}
