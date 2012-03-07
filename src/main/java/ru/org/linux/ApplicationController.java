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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

abstract public class ApplicationController extends ApplicationObjectSupport {

  @Autowired
  protected ApplicationLayout applicationLayout;

  /**
   * Подготовка к показу страницы.
   *
   * @param modelAndView source ModelAndView object
   * @return ModelAndView object
   */
  protected ModelAndView render(ModelAndView modelAndView) {
    modelAndView.addObject("javascriptsForLayout", applicationLayout.getJavascripts());
    modelAndView.addObject("cssForLayout", applicationLayout.getCss());
    return modelAndView;
  }

  /**
   * Локальный редирект на указанный URL.
   *
   * @param url относительный URL
   * @return
   */
  protected ModelAndView redirect(String url) {
    return redirect(url, null);
  }

  /**
   * Локальный редирект на указанный URL с параметрами запроса.
   *
   * @param url            относительный URL
   * @param redirectParams параметры запроса
   * @return
   */
  protected ModelAndView redirect(String url, Map<String, String> redirectParams) {
    RedirectView redirectView = new RedirectView(url, true);
    if (redirectParams != null) {
      redirectView.setAttributesMap(redirectParams);
    }
    ModelAndView modelAndView = new ModelAndView(redirectView);
    return modelAndView;
  }

  /**
   * Добавление JavaScript-файлов к текущему показу.
   *
   * @param javaScriptName JavaScript-файл
   */
  protected void setJavaScriptForLayout(String javaScriptName) {
    applicationLayout.getJavascripts().add(javaScriptName);
  }

  /**
   * Добавление CSS-файлов к текущему показу.
   *
   * @param csstName CSS-файл
   */
  protected void setCssForLayout(String csstName) {
    applicationLayout.getCss().add(csstName);
  }

}
