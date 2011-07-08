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

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.Group;
import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.Section;

@Controller
public class SectionController {
  @RequestMapping("/view-section.jsp")
  public ModelAndView handleRequestInternal(@RequestParam("section") int sectionid) throws Exception {
    Connection db = null;
    try {
      db = LorDataSource.getConnection();

      Section section = new Section(db, sectionid);

      Map<String, Object> params = new HashMap<String, Object>();
      params.put("section", section);
      params.put("groups", Group.getGroups(db, section));

      return new ModelAndView("section", params);
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }

  @RequestMapping("/forum")
  public ModelAndView forum() throws Exception {
    return handleRequestInternal(Section.SECTION_FORUM);
  }

  @RequestMapping(value="/view-section.jsp", params = {"section=2"})
  public View forumOld() {
    return new RedirectView("/forum/");
  }
}
