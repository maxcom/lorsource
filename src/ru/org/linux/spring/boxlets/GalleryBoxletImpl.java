package ru.org.linux.spring.boxlets;

import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.springframework.web.servlet.ModelAndView;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.org.linux.spring.dao.GalleryDaoImpl;
import ru.org.linux.site.Template;

/**
 * User: sreentenko
 * Date: 01.05.2009
 * Time: 1:05:06
 */
public class GalleryBoxletImpl extends SpringBoxlet {
  private GalleryDaoImpl galleryDao;

  public GalleryDaoImpl getGalleryDao() {
    return galleryDao;
  }

  public void setGalleryDao(GalleryDaoImpl galleryDao) {
    this.galleryDao = galleryDao;
  }

  protected ModelAndView getData(HttpServletRequest request, HttpServletResponse response) {
    ModelAndView mav = new ModelAndView();


    mav.setViewName("boxlets/gallery");
    mav.addObject("items", getGalleryDao().getGalleryItems());
    return mav;
  }
}
