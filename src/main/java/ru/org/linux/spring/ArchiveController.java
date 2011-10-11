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

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.Section;
import ru.org.linux.site.Group;
import ru.org.linux.spring.dao.SectionDao;

@Controller
public class ArchiveController {
  @Autowired
  private SectionDao sectionDao;

  public ModelAndView archiveList(
    int sectionid
  ) throws Exception {
    return archiveList(sectionid, null);
  }

  public ModelAndView archiveList(
    int sectionid,
    String groupName
  ) throws Exception {
    Connection db = null;

    Section section = sectionDao.getSection(sectionid);

    try {
      db = LorDataSource.getConnection();

      ModelAndView mv = new ModelAndView("view-news-archive");
      mv.getModel().put("section", section);

      Group group = null;
      if (groupName!=null) {
        group = new Group(db, sectionid, groupName);
      }

      PreparedStatement pst;
      if (group==null) {
        pst = db.prepareStatement("select year, month, c from monthly_stats where section=? and groupid is null order by year, month");
      } else {
        pst = db.prepareStatement("select year, month, c from monthly_stats where section=? and groupid=? order by year, month");
        pst.setInt(2, group.getId());
      }
      pst.setInt(1, sectionid);

      mv.getModel().put("group", group);

      ResultSet rs = pst.executeQuery();

      List<NewsArchiveListItem> items = new ArrayList<NewsArchiveListItem>();

      while (rs.next()) {
        items.add(new NewsArchiveListItem(section, group, rs.getInt("year"), rs.getInt("month"), rs.getInt("c")));
      }

      rs.close();
      pst.close();

      mv.getModel().put("items", items);

      return mv;
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  @RequestMapping("/gallery/archive")
  public ModelAndView galleryArchive(
  ) throws Exception {
    return archiveList(Section.SECTION_GALLERY);
  }
  @RequestMapping("/news/archive")
  public ModelAndView newsArchive(
  ) throws Exception {
    return archiveList(Section.SECTION_NEWS);
  }

  @RequestMapping("/polls/archive")
  public ModelAndView pollsArchive(
  ) throws Exception {
    return archiveList(Section.SECTION_POLLS);
  }

  @RequestMapping("/forum/{group}/archive")
  public ModelAndView forumArchive(
    @PathVariable String group
  ) throws Exception {
    return archiveList(Section.SECTION_FORUM, group);
  }

  @RequestMapping(value="/view-news-archive.jsp")
  public View galleryArchiveOld(@RequestParam("section") int id, HttpServletResponse response) throws Exception {
    String link = Section.getArchiveLink(id);

    if (link==null) {
      response.sendError(404, "No archive for this section");
      return null;
    }

    return new RedirectView(link);
  }

  public static class NewsArchiveListItem {
    private final int year;
    private final int month;
    private final int count;
    private final Section section;
    private final Group group;

    public NewsArchiveListItem(Section section, Group group, int year, int month, int count) {
      this.year = year;
      this.month = month;
      this.count = count;
      this.group = group;
      this.section = section;
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

    public String getLink() {
      if (group!=null) {
        return group.getArchiveLink(year, month);
      }
      return section.getArchiveLink(year, month);
    }
  }
}