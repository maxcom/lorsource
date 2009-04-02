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
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ru.org.linux.site.BadSectionException;
import ru.org.linux.site.Group;
import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.Section;
import ru.org.linux.util.ServletParameterParser;

public class SectionController extends AbstractController {
  protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
    int sectionid = new ServletParameterParser(request).getInt("section");

    Connection db = null;
    try {
      db = LorDataSource.getConnection();

      Section section = new Section(db, sectionid);

      if (!section.isBrowsable()) {
        throw new BadSectionException(sectionid);
      }

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
}
