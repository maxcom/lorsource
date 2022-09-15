/*
 * Copyright 1998-2022 Linux.org.ru
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

package ru.org.linux.topic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.group.GroupNotFoundException;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;

import java.util.List;

@Controller
public class ArchiveController {
  private final SectionService sectionService;

  private final GroupDao groupDao;

  private final ArchiveDao archiveDao;

  public ArchiveController(SectionService sectionService, GroupDao groupDao, ArchiveDao archiveDao) {
    this.sectionService = sectionService;
    this.groupDao = groupDao;
    this.archiveDao = archiveDao;
  }

  public ModelAndView archiveList(
    int sectionid
  ) {
    return archiveList(sectionid, null);
  }

  private ModelAndView archiveList(
    int sectionid,
    String groupName
  ) {
    ModelAndView mv = new ModelAndView("view-news-archive");

    Section section = sectionService.getSection(sectionid);
    mv.getModel().put("section", section);

    Group group = null;
    if (groupName!=null) {
      group = groupDao.getGroup(section, groupName);
    }

    mv.getModel().put("group", group);

    List<ArchiveDao.ArchiveStats> items = archiveDao.getArchiveStats(section, group);

    mv.getModel().put("items", items);

    return mv;
  }

  @RequestMapping("/gallery/archive")
  public ModelAndView galleryArchive() {
    return archiveList(Section.SECTION_GALLERY);
  }

  @RequestMapping("/news/archive")
  public ModelAndView newsArchive() {
    return archiveList(Section.SECTION_NEWS);
  }

  @RequestMapping("/polls/archive")
  public ModelAndView pollsArchive() {
    return archiveList(Section.SECTION_POLLS);
  }

  @RequestMapping("/articles/archive")
  public ModelAndView articlesArchive() {
    return archiveList(Section.SECTION_ARTICLES);
  }

  @RequestMapping("/forum/{group}/archive")
  public ModelAndView forumArchive(@PathVariable String group) {
    return archiveList(Section.SECTION_FORUM, group);
  }

  @ExceptionHandler(GroupNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ModelAndView handleNotFoundException() {
    return new ModelAndView("errors/code404");
  }
}
