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
package ru.org.linux;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Класс. предназначенный для перехвата исключений приложения.
 */
public class ExceptionResolver extends SimpleMappingExceptionResolver {

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
    ModelAndView modelAndView = super.doResolveException(request, response, handler, ex);
    if (modelAndView == null) {
      modelAndView = new ModelAndView("error");

    }
    modelAndView.addObject("exception", ex);
    return modelAndView;
  }
}
