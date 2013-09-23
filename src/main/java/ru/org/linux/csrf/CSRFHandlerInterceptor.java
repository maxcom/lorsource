/*
 * Copyright 1998-2013 Linux.org.ru
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
package ru.org.linux.csrf;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import ru.org.linux.auth.AccessViolationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CSRFHandlerInterceptor extends HandlerInterceptorAdapter {
  private static final Logger logger = LoggerFactory.getLogger(CSRFHandlerInterceptor.class);

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    if (!request.getMethod().equalsIgnoreCase("POST")) {
      // Not a POST - allow the request
      return true;
    } else {
      // This is a POST request - need to check the CSRF token
      //CSRFProtectionService.checkCSRF(request);

      if ((handler instanceof HandlerMethod) && (((HandlerMethod) handler).getMethodAnnotation(CSRFNoAuto.class)!=null)) {
        logger.debug("Auto CSRF disabled for "+((HandlerMethod) handler).getBeanType().getName());
        return true;
      }

      String csrfInput = request.getParameter(CSRFProtectionService.CSRF_INPUT_NAME);

      if (Strings.isNullOrEmpty(csrfInput)) {
        if ((handler instanceof HandlerMethod)) {
          logger.warn("Missing CSRF field for " + request.getRequestURI()+ ' ' +((HandlerMethod) handler).getBeanType().getName()+ '.' +((HandlerMethod) handler).getMethod().getName());
        } else {
          logger.warn("Missing CSRF field for " + request.getRequestURI()+" handler="+handler.getClass().toString()+" ip="+request.getRemoteAddr());
        }
      }

      if (!CSRFProtectionService.checkCSRF(request)) {
        throw new AccessViolationException("Неправильный код защиты CSRF. Возможно сессия устарела");
      }

      return true;
    }
  }
}
