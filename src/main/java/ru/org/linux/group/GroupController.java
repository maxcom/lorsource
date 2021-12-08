/*
 * Copyright 1998-2021 Linux.org.ru
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

package ru.org.linux.group;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.Template;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.topic.ArchiveDao;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ServletParameterBadValueException;
import ru.org.linux.util.image.ImageInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class GroupController {
  public static final int MAX_OFFSET = 300;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private ArchiveDao archiveDao;

  @Autowired
  private SectionService sectionService;

  @Autowired
  private GroupInfoPrepareService prepareService;

  @Autowired
  private GroupPermissionService groupPermissionService;

  @Autowired
  private SiteConfig siteConfig;

  @Autowired
  private GroupListDao groupListDao;

  @RequestMapping("/group.jsp")
  public ModelAndView topics(
          @RequestParam("group") int groupId,
          @RequestParam(value = "offset", required = false) Integer offsetObject
  ) {
    Group group = groupDao.getGroup(groupId);

    if (offsetObject != null) {
      return new ModelAndView(new RedirectView(group.getUrl() + "?offset=" + offsetObject.toString()));
    } else {
      return new ModelAndView(new RedirectView(group.getUrl()));
    }
  }

  @RequestMapping("/group-lastmod.jsp")
  public ModelAndView topicsLastmod(
          @RequestParam("group") int groupId,
          @RequestParam(value = "offset", required = false) Integer offsetObject
  ) {
    Group group = groupDao.getGroup(groupId);

    if (offsetObject != null) {
      return new ModelAndView(new RedirectView(group.getUrl() + "?offset=" + offsetObject.toString() + "&lastmod=true"));
    } else {
      return new ModelAndView(new RedirectView(group.getUrl() + "?lastmod=true"));
    }
  }

  @RequestMapping("/forum/{group}/{year:\\d+}/{month:\\d+}")
  public ModelAndView forumArchive(
    @PathVariable("group") String groupName,
    @RequestParam(defaultValue = "0", value="offset") int offset,
    @PathVariable int year,
    @PathVariable int month,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    return forum(groupName, offset, false, request, response, year, month);
  }

  @RequestMapping("/forum/{group}")
  public ModelAndView forum(
    @PathVariable("group") String groupName,
    @RequestParam(defaultValue = "0", value="offset") int offset,
    @RequestParam(defaultValue = "false") boolean lastmod,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    return forum(groupName, offset, lastmod, request, response, null, null);
  }

  private ModelAndView forum(
    @PathVariable("group") String groupName,
    @RequestParam(defaultValue = "0", value="offset") int offset,
    @RequestParam(defaultValue = "false") boolean lastmod,
    HttpServletRequest request,
    HttpServletResponse response,
    Integer year,
    Integer month
  ) throws Exception {
    Map<String, Object> params = new HashMap<>();
    Template tmpl = Template.getTemplate(request);

    boolean showDeleted = request.getParameter("deleted") != null;
    params.put("showDeleted", showDeleted);

    Section section = sectionService.getSection(Section.SECTION_FORUM);
    params.put("groupList", groupDao.getGroups(section));

    Group group = groupDao.getGroup(section, groupName);

    if (showDeleted && !"POST".equals(request.getMethod())) {
      return new ModelAndView(new RedirectView(group.getUrl()));
    }

    if (showDeleted && !tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Вы не авторизованы");
    }

    boolean firstPage;

    if (offset != 0) {
      firstPage = false;

      if (offset < 0) {
        throw new ServletParameterBadValueException("offset", "offset не может быть отрицательным");
      }

      if (year == null && offset>MAX_OFFSET) {
        return new ModelAndView(new RedirectView(group.getUrl()+"archive"));
      }
    } else {
      firstPage = true;
    }

    params.put("firstPage", firstPage);
    params.put("offset", offset);
    params.put("prevPage", offset - tmpl.getProf().getTopics());
    params.put("nextPage", offset + tmpl.getProf().getTopics());
    params.put("lastmod", lastmod);

    boolean showIgnored = false;
    if (request.getParameter("showignored") != null) {
      showIgnored = "t".equals(request.getParameter("showignored"));
    }

    params.put("showIgnored", showIgnored);

    params.put("group", group);

    if(group.getImage() != null) {
      try {
        params.put("groupImagePath", '/' + "tango" + group.getImage());
        ImageInfo info = new ImageInfo(siteConfig.getHTMLPathPrefix() + "tango" + group.getImage());
        params.put("groupImageInfo", info);
      } catch (BadImageException ex) {
        params.put("groupImagePath", null);
        params.put("groupImageInfo", null);
      }
    } else {
      params.put("groupImagePath", null);
      params.put("groupImageInfo", null);
    }

    params.put("section", section);

    params.put("groupInfo", prepareService.prepareGroupInfo(group));

    if (year!=null) {
      if (year<1990 || year > 3000) {
        throw new ServletParameterBadValueException("year", "указан некорректный год");
      }

      if (month<1 || month > 12) {
        throw new ServletParameterBadValueException("month", "указан некорректный месяц");
      }

      params.put("year", year);
      params.put("month", month);
      params.put("url", group.getUrl()+year+ '/' +month+ '/');
    } else {
      params.put("url", group.getUrl());
    }

    List<TopicsListItem> mainTopics;

    if (!lastmod) {
      mainTopics = groupListDao.getGroupListTopics(
              group.getId(),
              tmpl.getCurrentUser(),
              tmpl.getProf().getTopics(),
              offset,
              tmpl.getProf().getMessages(),
              showIgnored,
              showDeleted,
              year,
              month);
    } else {
      mainTopics = groupListDao.getGroupTrackerTopics(
              group.getId(),
              tmpl.getCurrentUser(),
              tmpl.getProf().getTopics(),
              offset,
              tmpl.getProf().getMessages());
    }

    if (year==null && offset==0 && !lastmod) {
      List<TopicsListItem> stickyTopics = groupListDao.getGroupStickyTopics(group, tmpl.getProf().getMessages());

      params.put("topicsList",  Lists.newArrayList(Iterables.concat(stickyTopics, mainTopics)));
    } else {
      params.put("topicsList", mainTopics);
    }

    if (year != null) {
      params.put("hasNext", offset + tmpl.getProf().getTopics() < archiveDao.getArchiveCount(group.getId(), year, month));
    } else {
      params.put("hasNext", offset<MAX_OFFSET && mainTopics.size()==tmpl.getProf().getTopics());
    }

    params.put("addable", groupPermissionService.isTopicPostingAllowed(group, tmpl.getCurrentUser()));

    response.setDateHeader("Expires", System.currentTimeMillis() + 90 * 1000);

    if (tmpl.isSessionAuthorized() && tmpl.getCurrentUser().getScore()>=500 && !tmpl.getProf().isOldTracker()) {
      return new ModelAndView("group-new", params);
    } else {
      return new ModelAndView("group", params);
    }
  }

  @ExceptionHandler(GroupNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ModelAndView handleNotFoundException() {
    return new ModelAndView("errors/code404");
  }
}
