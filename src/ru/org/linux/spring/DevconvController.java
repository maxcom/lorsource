package ru.org.linux.spring;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.*;

@Controller
public class DevconvController {
  @RequestMapping(value="/devconf2010", method = RequestMethod.POST)
  public ModelAndView add(HttpServletRequest request) throws Exception {
    Connection db = null;

    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new UserErrorException("Not authorized");
    }

    try {
      db = LorDataSource.getConnection();

      Statement st = db.createStatement();

      User user = User.getCurrentUser(db, request.getSession());

      UserInfo info = new UserInfo(db, user.getId());

      if (info.getRegistrationDate()!=null && info.getRegistrationDate().after(new Date(110, 4, 15))) {
        throw new UserErrorException("Дата регистрации после 15/04/2010");
      }

      ResultSet rs = st.executeQuery("SELECT * FROM devconf2010 WHERE userid="+user.getId());

      if (!rs.next()) {
        st.executeUpdate("INSERT INTO devconf2010 VALUES('"+ user.getId()+ "')");
      }

      return new ModelAndView("action-done", "message", "OK");
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }
}
