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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

import ru.org.linux.spring.dao.GalleryDaoImpl;

/**
 * User: sreentenko
 * Date: 01.05.2009
 * Time: 1:05:06
 */
@Controller
public class GalleryBoxletImpl extends SpringBoxlet {
  private GalleryDaoImpl galleryDao;

  public GalleryDaoImpl getGalleryDao() {
    return galleryDao;
  }
  @Autowired
  public void setGalleryDao(GalleryDaoImpl galleryDao) {
    this.galleryDao = galleryDao;
  }

  @RequestMapping("/gallery.boxlet")
  protected ModelAndView getData(HttpServletRequest request, HttpServletResponse response) {
    ModelAndView mav = new ModelAndView();
    mav.setViewName("boxlets/gallery");
    mav.addObject("items", getGalleryDao().getGalleryItems());
    return mav;
  }
}
