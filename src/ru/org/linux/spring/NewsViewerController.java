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

import java.net.URLEncoder;
import java.sql.Connection;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

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

    params.put("url", "view-news.jsp");
    StringBuilder urlParams = new StringBuilder();
    if (sectionid!=null) {
      urlParams.append("section="+Integer.toString(sectionid));
    }

    if (tag!=null) {
      if (urlParams.length()>0) {
        urlParams.append("&");
      }

      urlParams.append("tag="+ URLEncoder.encode(tag));
    }

    if (groupid!=null) {
      if (urlParams.length()>0) {
        urlParams.append("&");
      }

      urlParams.append("group="+Integer.toString(groupid));
    }

    params.put("params", urlParams);

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
        params.put("archiveLink", section.getArchiveLink());
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
        navtitle = "<a href=\""+Section.getNewsViewerLink(group.getSectionId()) + "\">" + section.getName() + "</a> - <strong>" + group.getTitle()+"</strong>";
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
          newsViewer.setDatelimit("(commitdate>(CURRENT_TIMESTAMP-'1 year'::interval))");
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

  @RequestMapping(value="/people/{nick}")
  public ModelAndView showUserTopicsNew(
    @PathVariable String nick,
    @RequestParam(value="offset", required=false) Integer offset,
    @RequestParam(value="output", required=false) String output,
    HttpServletResponse response
  ) throws Exception {
    Connection db = null;

    Map<String, Object> params = new HashMap<String, Object>();

    params.put("url", "/people/"+nick+"/");
// TODO    params.put("archiveLink", "/people/"+nick+"/archive/");

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

      params.put("rssLink", "/people/"+nick+"/?output=rss");

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

  @RequestMapping(value="/view-all.jsp", method=RequestMethod.GET)
  public ModelAndView viewAll(
    @RequestParam(value="section", required = false) Integer sectionId
  ) throws Exception {
    Connection db = null;

    ModelAndView modelAndView = new ModelAndView("view-all");

    try {
      db = LorDataSource.getConnection();
      
      Section section = null;

      if (sectionId!=null) {
        section = new Section(db, sectionId);
        modelAndView.getModel().put("section", section);
      }

      NewsViewer newsViewer = new NewsViewer();
      newsViewer.setCommitMode(NewsViewer.CommitMode.UNCOMMITED_ONLY);
      newsViewer.setDatelimit("postdate>(CURRENT_TIMESTAMP-'1 month'::interval)");
      if (section != null) {
        newsViewer.addSection(section.getId());
      }

      modelAndView.getModel().put("messages", newsViewer.getMessages(db));

      return modelAndView;
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  @RequestMapping(value = "/show-topics.jsp", method = RequestMethod.GET)
  public View showUserTopics(
    @RequestParam("nick") String nick,
    @RequestParam(value="output", required=false) String output
  ) throws Exception {
    if (output!=null) {
      return new RedirectView("/people/"+nick+"/?output=rss");
    }
    
    return new RedirectView("/people/"+nick+"/");
  }

  @RequestMapping("/gallery/")
  public ModelAndView gallery(
    @RequestParam(required=false) Integer offset,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView mv = showNews(null, null, Section.SECTION_GALLERY, null, null, offset, response);

    mv.getModel().put("url", "/gallery/");
    mv.getModel().put("params", null);

    return mv;
  }

  @RequestMapping("/forum/lenta")
  public ModelAndView forum(
    @RequestParam(required=false) Integer offset,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView mv = showNews(null, null, Section.SECTION_FORUM, null, null, offset, response);

    mv.getModel().put("url", "/forum/lenta");
    mv.getModel().put("params", null);

    return mv;
  }

  @RequestMapping("/polls/")
  public ModelAndView polls(
    @RequestParam(required=false) Integer offset,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView mv = showNews(null, null, Section.SECTION_POLLS, null, null, offset, response);

    mv.getModel().put("url", "/polls/");
    mv.getModel().put("params", null);

    return mv;
  }

  @RequestMapping("/news/")
  public ModelAndView news(
    @RequestParam(required=false) Integer offset,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView mv = showNews(null, null, Section.SECTION_NEWS, null, null, offset, response);

    mv.getModel().put("url", "/news/");
    mv.getModel().put("params", null);

    return mv;
  }

  @RequestMapping("/gallery/{group}")
  public ModelAndView galleryGroup(
    @RequestParam(required=false) Integer offset,
    @PathVariable("group") String groupName,
    HttpServletResponse response
  ) throws Exception {
    return group(Section.SECTION_GALLERY, offset, groupName, response);
  }

  @RequestMapping("/news/{group}")
  public ModelAndView newsGroup(
    @RequestParam(required=false) Integer offset,
    @PathVariable("group") String groupName,
    HttpServletResponse response
  ) throws Exception {
    return group(Section.SECTION_NEWS, offset, groupName, response);
  }

  @RequestMapping("/polls/{group}")
  public ModelAndView pollsGroup(
    @RequestParam(required=false) Integer offset,
    @PathVariable("group") String groupName,
    HttpServletResponse response
  ) throws Exception {
    return group(Section.SECTION_POLLS, offset, groupName, response);
  }

  public ModelAndView group(
    int sectionId,
    Integer offset,
    String groupName,
    HttpServletResponse response
  ) throws Exception {
    Connection db = null;
    Group group;

    try {
      db = LorDataSource.getConnection();

      Section section = new Section(db, sectionId);
      group = section.getGroup(db, groupName);
    } finally {
      if (db!=null) {
        db.close();
      }
    }

    ModelAndView mv = showNews(null, null, group.getSectionId(), group.getId(), null, offset, response);

    mv.getModel().put("url", group.getUrl());
    mv.getModel().put("params", null);

    return mv;
  }

  @RequestMapping(value="/view-news.jsp", params={ "section" })
  public View galleryOldLink(
    @RequestParam int section,
    @RequestParam(required=false) Integer offset,
    @RequestParam(value="month", required=false) Integer month,
    @RequestParam(value="year", required=false) Integer year
  ) {
    if (offset!=null) {
      return new RedirectView(Section.getNewsViewerLink(section)+"?offset="+Integer.toString(offset));
    }

    if (year!=null) {
      return new RedirectView(Section.getArchiveLink(section)+Integer.toString(year)+"/"+Integer.toString(month));
    }

    return new RedirectView(Section.getNewsViewerLink(section));
  }

  @RequestMapping("/{section}/archive/{year}/{month}")
  public ModelAndView galleryArchive(
    @PathVariable String section,
    @PathVariable int year,
    @PathVariable int month,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView mv = showNews(month, year, Section.getSection(section), null, null, null, response);

    mv.getModel().put("url", "/gallery/archive/"+year+"/"+month+"/");
    mv.getModel().put("params", null);

    return mv;
  }
}
