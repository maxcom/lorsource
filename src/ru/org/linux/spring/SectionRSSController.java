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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;

@Controller
public class SectionRSSController {
  @RequestMapping("/section-rss.jsp")
  public ModelAndView showRSS(
    @RequestParam(value="section", required = false) Integer sectionId,
    @RequestParam(value="group", required = false) Integer groupId,
    HttpServletRequest request
  ) throws Exception {
    Map params = new HashMap();

    if (sectionId==null) {
      sectionId = 1;
    }

    if (groupId==null) {
      groupId = 0;
    }

    String userAgent = request.getHeader("User-Agent");
    boolean feedBurner = userAgent!=null && userAgent.contains("FeedBurner");

    if (sectionId==1 && groupId==0 && !feedBurner && request.getParameter("noredirect")==null) {
      return new ModelAndView(new RedirectView("http://feeds.feedburner.com/org/LOR"));
    }

    NewsViewer nv = new NewsViewer();
    nv.addSection(sectionId);
    nv.setDatelimit(" postdate>(CURRENT_TIMESTAMP-'3 month'::interval) ");
    if (groupId !=0) {
      nv.setGroup(groupId);
    }

    nv.setLimit("LIMIT 20");

    Connection db = null;
    try {
      db = LorDataSource.getConnection();

      Section section = new Section(db, sectionId);
      params.put("section", section);

      Group group = null;
      if (groupId!=0) {
        group = new Group(db, groupId);

        if (group.getSectionId()!=sectionId) {
          throw new BadGroupException("группа #"+groupId+" не принадлежит разделу #"+sectionId);
        }

        params.put("group", group);
      }

      String ptitle = section.getName();
      if (group!=null) {
        ptitle += " - " + group.getTitle();
      }

      params.put("ptitle", ptitle);

      List<Message> messages=feedBurner?nv.getMessages(db):nv.getMessagesCached(db);
      params.put("messages", messages);

      return new ModelAndView("section-rss", params);
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }
}
