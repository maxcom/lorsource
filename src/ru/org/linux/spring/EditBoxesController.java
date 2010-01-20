/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.spring;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.AccessViolationException;
import ru.org.linux.site.Template;
import ru.org.linux.util.UtilException;

@Controller
public class EditBoxesController{
  @RequestMapping(value = "/edit-boxes.jsp")
  public ModelAndView view(HttpServletRequest request) throws AccessViolationException, UtilException {
    boolean isThreeColumn = getThreeColumns(request);
    ModelAndView result = new ModelAndView("edit-boxes");
    result.addObject("isThreeColumn", isThreeColumn);
    return result;
  }

  protected static boolean getThreeColumns(HttpServletRequest request)
    throws AccessViolationException, UtilException {
    Template t = Template.getTemplate(request);
    if (t.isUsingDefaultProfile() || t.getProfileName().charAt(0) == '_') {
      throw new AccessViolationException("нельзя изменить системный профиль; создайте сначала свой");
    }
    return t.getProf().getBoolean("main.3columns");
  }


}
