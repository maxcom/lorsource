package ru.org.linux.spring;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.Tags;

public class TagsController extends AbstractController {
  protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

    Connection db = null;
    try {
      db = LorDataSource.getConnection();

      Map<String, Object> params = new HashMap<String, Object>();
      params.put("tags", Tags.getAllTags(db));

      return new ModelAndView("tags", params);
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }
}
