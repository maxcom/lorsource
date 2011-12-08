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

import ru.org.linux.dao.GalleryDao;
import ru.org.linux.dto.GalleryDto;
import ru.org.linux.spring.commons.CacheProvider;

@Controller
public class GalleryBoxlet extends AbstractBoxlet {
  private GalleryDao galleryDao;
  private CacheProvider cacheProvider;

  public GalleryDao getGalleryDao() {
    return galleryDao;
  }

  @Autowired
  public void setGalleryDao(GalleryDao galleryDao) {
    this.galleryDao = galleryDao;
  }

  @Autowired
  public void setCacheProvider(CacheProvider cacheProvider) {
    this.cacheProvider = cacheProvider;
  }

  @Override
  @RequestMapping("/gallery.boxlet")
  protected ModelAndView getData(HttpServletRequest request) throws Exception {
    ModelAndView mav = new ModelAndView();
    mav.setViewName("boxlets/gallery");
    List<GalleryDto> list = getFromCache(cacheProvider, new GetCommand<List<GalleryDto>>() {
      @Override
      public List<GalleryDto> get() {
        return galleryDao.getGalleryItems();
      }
    });
    mav.addObject("items", list);
    return mav;
  }
}
