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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.*;
import ru.org.linux.util.DateUtil;
import ru.org.linux.util.ServletParameterException;
import ru.org.linux.util.ServletParameterMissingException;

@Controller
public class NewsViewerController {
  @RequestMapping(value = "/view-news.jsp", method = RequestMethod.GET)
  public ModelAndView showNews(
    @RequestParam(value="month", required=false) Integer month,
    @RequestParam(value="year", required=false) Integer year,
    @RequestParam(value="section", required=false) Integer sectionid,
    @RequestParam(value="group", required=false) Integer groupid,
    @RequestParam(value="tag", required=false) String tag,
    @RequestParam(value="offset", required=false) Integer offset,
    HttpServletResponse response
  ) throws Exception {
    Connection db = null;

    Map<String, Object> params = new HashMap<String, Object>();

    try {
      if (month == null) {
        response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 1000);
        response.setDateHeader("Last-Modified", System.currentTimeMillis());
      } else {
        long expires = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L;

        if (year==null) {
          throw new ServletParameterMissingException("year");
        }

        params.put("year", year);
        params.put("month", month);

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1);
        calendar.add(Calendar.MONTH, 1);

        long lastmod = calendar.getTimeInMillis();

        if (lastmod < System.currentTimeMillis()) {
          response.setDateHeader("Expires", expires);
          response.setDateHeader("Last-Modified", lastmod);
        } else {
          response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 1000);
          response.setDateHeader("Last-Modified", System.currentTimeMillis());
        }
      }

      db = LorDataSource.getConnection();

      Section section = null;

      if (sectionid != null) {
        section = new Section(db, sectionid);

        params.put("section", section);
      }

      Group group = null;

      if (groupid != null) {
        group = new Group(db, groupid);

        if (group.getSectionId() != sectionid) {
          throw new ScriptErrorException("группа #" + groupid + " не принадлежит разделу #" + sectionid);
        }

        params.put("group", group);
      }

      if (tag != null) {
        Tags.checkTag(tag);
        params.put("tag", tag);
      }

      if (section == null && tag == null) {
        throw new ServletParameterException("section or tag required");
      }

      String navtitle;
      if (section != null) {
        navtitle = section.getName();
      } else {
        navtitle = tag;
      }

      if (group != null) {
        navtitle = "<a href=\"view-news.jsp?section=" + section.getId() + "\">" + section.getName() + "</a> - " + group.getTitle();
      }

      String ptitle;

      if (month == null) {
        if (section != null) {
          ptitle = section.getName();
          if (group != null) {
            ptitle += " - " + group.getTitle();
          }

          if (tag != null) {
            ptitle += " - " + tag;
          }
        } else {
          ptitle = tag;
        }
      } else {
        ptitle = "Архив: " + section.getName();

        if (group != null) {
          ptitle += " - " + group.getTitle();
        }

        if (tag != null) {
          ptitle += " - " + tag;
        }

        ptitle += ", " + year + ", " + DateUtil.getMonth(month);
        navtitle += " - Архив " + year + ", " + DateUtil.getMonth(month);
      }

      params.put("ptitle", ptitle);
      params.put("navtitle", navtitle);

      NewsViewer newsViewer = new NewsViewer();

      if (section!=null) {
        newsViewer.addSection(sectionid);
      }

      if (group != null) {
        newsViewer.setGroup(group.getId());
      }

      if (tag!=null) {
        newsViewer.setTag(tag);
      }

      offset = fixOffset(offset);

      if (month != null) {
        newsViewer.setDatelimit("postdate>='" + year + '-' + month + "-01'::timestamp AND (postdate<'" + year + '-' + month + "-01'::timestamp+'1 month'::interval)");
      } else if (tag==null) {
        if (section.isPremoderated()) {
          newsViewer.setDatelimit("(commitdate>(CURRENT_TIMESTAMP-'6 month'::interval))");
        } else {
          newsViewer.setDatelimit("(postdate>(CURRENT_TIMESTAMP-'6 month'::interval))");
        }

        newsViewer.setLimit("LIMIT 20" + (offset > 0 ? (" OFFSET " + offset) : ""));
      } else {
        newsViewer.setLimit("LIMIT 20" + (offset > 0 ? (" OFFSET " + offset) : ""));
      }

      params.put("messages", newsViewer.getMessagesCached(db));

      params.put("offsetNavigation", month==null);
      params.put("offset", offset);

      if (section!=null) {
        String rssLink = "section-rss.jsp?section="+section.getId();
        if (group!=null) {
          rssLink += "&group="+group.getId();
        }

        params.put("rssLink", rssLink);
      }

      return new ModelAndView("view-news", params);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  @RequestMapping(value = "/show-topics.jsp", method = RequestMethod.GET)
  public ModelAndView showUserTopics(
    @RequestParam("nick") String nick,
    @RequestParam(value="offset", required=false) Integer offset,
    @RequestParam(value="output", required=false) String output,
    HttpServletResponse response
  ) throws Exception {
    Connection db = null;

    Map<String, Object> params = new HashMap<String, Object>();

    try {
      response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 1000);
      response.setDateHeader("Last-Modified", System.currentTimeMillis());

      db = LorDataSource.getConnection();

      User user = User.getUser(db, nick);

      params.put("ptitle", "Сообщения "+user.getNick());
      params.put("navtitle", "Сообщения "+user.getNick());

      params.put("user", user);

      NewsViewer newsViewer = new NewsViewer();

      offset = fixOffset(offset);

      newsViewer.setLimit("LIMIT 20" + (offset > 0 ? (" OFFSET " + offset) : ""));

      newsViewer.setCommitMode(NewsViewer.CommitMode.ALL);

      if (user.getId()==2) {
        throw new UserErrorException("Лента для пользователя anonymous не доступна");
      }

      newsViewer.setUserid(user.getId());

      params.put("messages", newsViewer.getMessagesCached(db));

      params.put("offsetNavigation", true);
      params.put("offset", offset);

      params.put("rssLink", "show-topics.jsp?nick="+nick+"&output=rss");

      if (output!=null && output.equals("rss")) {
        return new ModelAndView("section-rss", params);
      } else {
        return new ModelAndView("view-news", params);
      }
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  private int fixOffset(Integer offset) {
    if (offset!=null) {
      if (offset<0) {
          return 0;
      }

      if (offset>200) {
        return 200;
      }

      return offset;
    } else {
      return 0;
    }
  }
}
