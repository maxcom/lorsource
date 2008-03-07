package ru.org.linux.spring;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ru.org.linux.site.BadSectionException;
import ru.org.linux.site.Group;
import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.Section;
import ru.org.linux.util.ServletParameterParser;

public class SectionController extends AbstractController {
  protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
    int sectionid = new ServletParameterParser(request).getInt("section");

    Connection db = null;
    try {
      db = LorDataSource.getConnection();

      Section section = new Section(db, sectionid);

      if (!section.isBrowsable()) {
        throw new BadSectionException(sectionid);
      }

      Map<String, Object> params = new HashMap<String, Object>();
      params.put("section", section);
      params.put("groups", Group.getGroups(db, section));

      return new ModelAndView("section", params);
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }
}
