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

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;

@Controller
public class GroupController {
  @RequestMapping("/group.jsp")
  public ModelAndView topics(@RequestParam("group") int groupId, HttpServletRequest request) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    boolean showDeleted = request.getParameter("deleted") != null;
    Template tmpl = Template.getTemplate(request);

    if (showDeleted && !"POST".equals(request.getMethod())) {
      return new ModelAndView(new RedirectView(tmpl.getMainUrl() + "/group.jsp?group=" + groupId));
    }

    if (showDeleted && !Template.isSessionAuthorized(request.getSession())) {
      throw new AccessViolationException("Вы не авторизованы");
    }

    params.put("showDeleted", showDeleted);

    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      Group group = new Group(db, groupId);
      params.put("group", group);

      Section section = new Section(db, group.getSectionId());
      params.put("section", section);
      
      return new ModelAndView("group", params);
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }
}
