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
import java.sql.ResultSet;
import java.sql.Statement;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.beans.factory.annotation.Autowired;

import ru.org.linux.site.*;

@Controller
public class MainPageController {
  private final SectionStore sectionStore;

  @Autowired
  public MainPageController(SectionStore sectionStore) {
    this.sectionStore = sectionStore;
  }

  @RequestMapping({"/", "/index.jsp"})
  public ModelAndView mainPage(HttpServletRequest request) throws Exception {
    Template tmpl = Template.getTemplate(request);

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      NewsViewer nv = NewsViewer.getMainpage(sectionStore);

      if (tmpl.getProf().getBoolean(DefaultProfile.MAIN_GALLERY)) {
        nv.addSection(3);
      }

      ModelAndView mv = new ModelAndView("index");

      mv.getModel().put("news", nv.getPreparedMessages(db));

      if (tmpl.isModeratorSession() || tmpl.isCorrectorSession()) {
        Statement st = db.createStatement();
        ResultSet allUncommited = st.executeQuery("select count(*) from topics,groups,sections where section=sections.id AND sections.moderate and topics.groupid=groups.id and not deleted and not topics.moderate AND postdate>(CURRENT_TIMESTAMP-'1 month'::interval)");

        int uncommited = 0;

        if (allUncommited.next()) {
          uncommited = allUncommited.getInt(1);
        }

        allUncommited.close();

        mv.getModel().put("uncommited", uncommited);

        int uncommitedNews = 0;

        if (uncommited>0) {
          ResultSet rs = st.executeQuery("select count(*) from topics,groups where section=1 AND topics.groupid=groups.id and not deleted and not topics.moderate AND postdate>(CURRENT_TIMESTAMP-'1 month'::interval)");

          if (rs.next()) {
            uncommitedNews = rs.getInt(1);
          }

          rs.close();
        }

        mv.getModel().put("uncommitedNews", uncommitedNews);
      }

      return mv;
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
}
