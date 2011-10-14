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

package ru.org.linux.spring.boxlets;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.Section;
import ru.org.linux.site.SectionNotFoundException;
import ru.org.linux.spring.commons.CacheProvider;
import ru.org.linux.spring.dao.ArchiveDao;
import ru.org.linux.spring.dao.SectionDao;

@Controller
public class ArchiveBoxletImpl extends SpringBoxlet{
  private ArchiveDao archiveDao;
  private CacheProvider cacheProvider;

  private Section sectionNews;

  @Autowired
  public void setSectionDa(SectionDao sectionDao) throws SectionNotFoundException {
    sectionNews = sectionDao.getSection(Section.SECTION_NEWS);
  }

  @Autowired
  public void setArchiveDao(ArchiveDao archiveDao) {
    this.archiveDao = archiveDao;
  }

  @Autowired
  public void setCacheProvider(CacheProvider cacheProvider) {
    this.cacheProvider = cacheProvider;
  }

  @Override
  @RequestMapping("/archive.boxlet")
  protected ModelAndView getData(HttpServletRequest request) throws Exception {
    List<ArchiveDao.ArchiveDTO> list = getFromCache(cacheProvider, new GetCommand<List<ArchiveDao.ArchiveDTO>>() {
      @Override
      public List<ArchiveDao.ArchiveDTO> get() {
        return archiveDao.getArchiveDTO(sectionNews, 13);
      }
    });

    return new ModelAndView("boxlets/archive", "items", list);
  }
}
