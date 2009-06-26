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

package ru.org.linux.spring.boxlets;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.spring.commons.CacheProvider;
import ru.org.linux.spring.dao.ArchiveDaoImpl;

/**
 * User: sreentenko
 * Date: 01.05.2009
 * Time: 23:15:32
 */
@Controller
public class ArchiveBoxletImpl extends SpringBoxlet{
  private ArchiveDaoImpl archiveDao;
  private CacheProvider cacheProvider;

  public ArchiveDaoImpl getArchiveDao() {
    return archiveDao;
  }
  @Autowired
  public void setArchiveDao(ArchiveDaoImpl archiveDao) {
    this.archiveDao = archiveDao;
  }

  public CacheProvider getCacheProvider() {
    return cacheProvider;
  }

  @Autowired
  public void setCacheProvider(CacheProvider cacheProvider) {
    this.cacheProvider = cacheProvider;
  }

  @RequestMapping("/archive.boxlet")
  protected ModelAndView getData(HttpServletRequest request, HttpServletResponse response) {
    List<ArchiveDaoImpl.ArchiveDTO> list = getFromCache(new GetCommand<List<ArchiveDaoImpl.ArchiveDTO>>() {
      public List<ArchiveDaoImpl.ArchiveDTO> get() {
        return archiveDao.getArchiveDTO();
      }
    });

    return new ModelAndView("boxlets/archive", "items", list);
  }
}
