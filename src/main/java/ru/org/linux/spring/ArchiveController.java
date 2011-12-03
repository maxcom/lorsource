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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.dao.ArchiveDao;
import ru.org.linux.dao.GroupDao;
import ru.org.linux.dao.SectionDao;
import ru.org.linux.dto.ArchiveDto;
import ru.org.linux.dto.GroupDto;
import ru.org.linux.dto.SectionDto;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class ArchiveController {
  @Autowired
  private SectionDao sectionDao;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private ArchiveDao archiveDao;

  public ModelAndView archiveList(
      int sectionid
  ) throws Exception {
    return archiveList(sectionid, null);
  }

  public ModelAndView archiveList(
      int sectionid,
      String groupName
  ) throws Exception {
    ModelAndView mv = new ModelAndView("view-news-archive");

    SectionDto sectionDto = sectionDao.getSection(sectionid);
    mv.getModel().put("section", sectionDto);

    GroupDto groupDto = null;
    if (groupName != null) {
      groupDto = groupDao.getGroup(sectionDto, groupName);
    }

    mv.getModel().put("group", groupDto);

    List<ArchiveDto> items = archiveDao.getArchiveDTO(sectionDto, groupDto);

    mv.getModel().put("items", items);

    return mv;
  }

  @RequestMapping("/gallery/archive")
  public ModelAndView galleryArchive(
  ) throws Exception {
    return archiveList(SectionDto.SECTION_GALLERY);
  }

  @RequestMapping("/news/archive")
  public ModelAndView newsArchive(
  ) throws Exception {
    return archiveList(SectionDto.SECTION_NEWS);
  }

  @RequestMapping("/polls/archive")
  public ModelAndView pollsArchive(
  ) throws Exception {
    return archiveList(SectionDto.SECTION_POLLS);
  }

  @RequestMapping("/forum/{group}/archive")
  public ModelAndView forumArchive(
      @PathVariable String group
  ) throws Exception {
    return archiveList(SectionDto.SECTION_FORUM, group);
  }

  @RequestMapping(value = "/view-news-archive.jsp")
  public View galleryArchiveOld(@RequestParam("section") int id, HttpServletResponse response) throws Exception {
    String link = SectionDto.getArchiveLink(id);

    if (link == null) {
      response.sendError(404, "No archive for this section");
      return null;
    }

    return new RedirectView(link);
  }
}