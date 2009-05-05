/*
 * Copyright 1998-2009 Linux.org.ru
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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ru.org.linux.site.Template;
import ru.org.linux.site.AccessViolationException;
import ru.org.linux.util.UtilException;

@Controller
public class EditBoxesController extends ApplicationObjectSupport {
  @RequestMapping(value = "/edit-boxes.jsp")
  public ModelAndView view(HttpServletRequest request) throws AccessViolationException, UtilException {
    boolean isThreeColumn = getThreeColumns(request);
    ModelAndView result = new ModelAndView("edit-boxes");
    result.addObject("isThreeColumn", isThreeColumn);
    return result;
  }

  private boolean getThreeColumns(HttpServletRequest request) throws AccessViolationException, UtilException {
    Template t = Template.getTemplate(request);
    if (t.isUsingDefaultProfile() || t.getProfileName().charAt(0) == '_') {
      throw new AccessViolationException("нельзя изменить системный профиль; создайте сначала свой");
    }
    boolean isThreeColumn = t.getProf().getBoolean("main.3columns");
    return isThreeColumn;
  }

  @RequestMapping(value = "/remove-box.jsp", method = RequestMethod.GET)
  public ModelAndView showRemove(HttpServletRequest request, @RequestParam(required = true) String tag,
                                 @RequestParam(required = true) Integer pos) throws AccessViolationException, UtilException{

    ModelAndView result = new ModelAndView("remove-box");
    result.addObject("tag", tag);
    result.addObject("pos", pos);
    return result;
  }

  @RequestMapping(value = "/remove-box.jsp", method = RequestMethod.POST)
  public void doRemove(HttpServletRequest request, HttpServletResponse response) throws IOException {
    //todo
    response.sendRedirect("/edit-boxes.jsp");
  }
}
