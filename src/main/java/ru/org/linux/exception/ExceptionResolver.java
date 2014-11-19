/*
 * Copyright 1998-2014 Linux.org.ru
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import ru.org.linux.email.EmailService;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.user.UserErrorException;
import ru.org.linux.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Класс. предназначенный для перехвата исключений приложения.
 */

@Component
public class ExceptionResolver extends SimpleMappingExceptionResolver {
  private static final Logger logger = LoggerFactory.getLogger(ExceptionResolver.class);

  @Autowired
  private EmailService emailService;

  enum ExceptionType {
    IGNORED,
    SCRIPT_ERROR,
    OTHER
  }

  /**
   * Общий обработчик исключительных ситуаций.
   * Предназначен для расширения функционала стандартного обработчика.
   *
   * @param request  данные запроса от web-клиента
   * @param response данные ответа web-клиенту
   * @param handler  объект, в котором возникло исключение
   * @param ex       исключение
   * @return объект web-модели
   */
  @Override
  protected ModelAndView doResolveException(
    HttpServletRequest request,
    HttpServletResponse response,
    Object handler,
    Exception ex
  ) {
    // http://stackoverflow.com/questions/8271843/how-to-exclude-clientabortexception-from-simplemappingexceptionresolver
    if (ex!=null && ex.getClass().getName().endsWith(".ClientAbortException") && response.isCommitted()) {
      return null;
    }

    ModelAndView modelAndView = super.doResolveException(request, response, handler, ex);
    if (modelAndView == null) {
      modelAndView = new ModelAndView("errors/common");
      prepareModelForCommonException(modelAndView, request, ex);
    }
    modelAndView.addObject("exception", ex);
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    return modelAndView;
  }

  /**
   * Наполнение объекта web-модели необходимыми для показа данными.
   *
   * @param modelAndView объект web-модели
   * @param request      данные запроса от web-клиента
   * @param exception    исключение
   */
  private void prepareModelForCommonException(
    ModelAndView modelAndView,
    HttpServletRequest request,
    Exception exception
  ) {
    modelAndView.addObject("headTitle", StringUtil.escapeHtml(exception.getClass().getName()));

    String errorMessage = exception.getMessage() == null
      ? StringUtil.escapeHtml(exception.getClass().getName())
      : StringUtil.escapeHtml(exception.getMessage());
    modelAndView.addObject("errorMessage", errorMessage);

    ExceptionType exceptionType = ExceptionType.OTHER;
    if (exception instanceof UserErrorException) {
      exceptionType = ExceptionType.IGNORED;
    } else if (exception instanceof ScriptErrorException) {
      logger.debug("errors/common.jsp", exception);
      exceptionType = ExceptionType.SCRIPT_ERROR;
    } else {
      logger.warn("Unexcepted exception caught", exception);
      String infoMessage = emailService.sendExceptionReport(request, exception);
      modelAndView.addObject("infoMessage", infoMessage);
    }
    modelAndView.addObject("exceptionType", exceptionType.name());
  }
}
