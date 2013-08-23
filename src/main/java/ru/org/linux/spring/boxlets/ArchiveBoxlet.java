/*
 * Copyright 1998-2013 Linux.org.ru
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

package ru.org.linux.spring.boxlets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionNotFoundException;
import ru.org.linux.section.SectionService;
import ru.org.linux.topic.ArchiveDao;
import ru.org.linux.topic.ArchiveDao.ArchiveDTO;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class ArchiveBoxlet extends AbstractBoxlet {
  @Autowired
  private ArchiveDao archiveDao;

  @Autowired
  private SectionService sectionService;

  private Section sectionNews;

  /**
   * Инициализация бокслета.
   * Метод вызывается автоматически сразу после создания бина.
   *
   * @throws SectionNotFoundException
   */
  @PostConstruct
  private void initializeBoxlet() throws SectionNotFoundException {
    sectionNews = sectionService.getSection(Section.SECTION_NEWS);
  }

  @Override
  @RequestMapping("/archive.boxlet")
  protected ModelAndView getData(HttpServletRequest request) throws Exception {
    List<ArchiveDTO> list = archiveDao.getArchiveDTO(sectionNews, 13);

    return new ModelAndView("boxlets/archive", "items", list);
  }
}
