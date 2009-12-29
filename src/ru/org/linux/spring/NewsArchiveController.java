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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.Section;

@Controller
public class NewsArchiveController {
  @RequestMapping("/view-news-archive.jsp")
  public ModelAndView archiveList(
    @RequestParam("section") int sectionid
  ) throws Exception {
    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      Section section = new Section(db, sectionid);

      Statement st = db.createStatement();

      ModelAndView mv = new ModelAndView("view-news-archive");
      mv.getModel().put("section", section);

      ResultSet rs = st.executeQuery("select year, month, c from monthly_stats where section=" + sectionid + " order by year, month");

      List<NewsArchiveListItem> items = new ArrayList<NewsArchiveListItem>();

      while (rs.next()) {
        items.add(new NewsArchiveListItem(rs.getInt("year"), rs.getInt("month"), rs.getInt("c")));
      }

      rs.close();
      st.close();

      mv.getModel().put("items", items);

      return mv;
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  public class NewsArchiveListItem {
    private int year;
    private int month;
    private int count;

    public NewsArchiveListItem(int year, int month, int count) {
      this.year = year;
      this.month = month;
      this.count = count;
    }

    public int getYear() {
      return year;
    }

    public int getMonth() {
      return month;
    }

    public int getCount() {
      return count;
    }
  }
}
