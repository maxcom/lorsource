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

package ru.org.linux.spring;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.AccessViolationException;
import ru.org.linux.site.Group;
import ru.org.linux.site.Template;

@Controller
public class GroupModificationController {
  private DataSource dataSource;

  @RequestMapping(value="/groupmod.jsp", method = RequestMethod.GET)
  public ModelAndView showForm(@RequestParam("group") int id, ServletRequest request) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    Connection db = null;
    try {
      db = dataSource.getConnection();

      Group group = new Group(db, id);

      return new ModelAndView("groupmod", "group", group);
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }

  @RequestMapping(value="/groupmod.jsp", method = RequestMethod.POST)
  public ModelAndView modifyGroup(
    @RequestParam("group") int id,
    @RequestParam("info") String info,
    @RequestParam("urlName") String urlName,
    @RequestParam("longinfo") String longInfo,
    @RequestParam(value = "preview", required = false) String preview,
    @RequestParam(value = "resolvable", required = false) String resolvable,
    ServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    Connection db = null;

    try {
      db = dataSource.getConnection();

      Group group = new Group(db, id);

      if (preview!=null) {
        group.setInfo(info);
        group.setLongInfo(longInfo);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("group", group);
        params.put("preview", true);

        return new ModelAndView("groupmod", params);
      }

      PreparedStatement pst = db.prepareStatement("UPDATE groups SET info=?, longinfo=?,resolvable=?,urlname=? WHERE id=?");

      if (info.length()>0) {
        pst.setString(1, info);
      } else {
        pst.setString(1, null);
      }

      if (longInfo.length()>0) {
        pst.setString(2, longInfo);
      } else {
        pst.setString(2, null);
      }

      pst.setBoolean(3, resolvable!=null);
      pst.setString(4, urlName);
      pst.setInt(5, id);

      pst.executeUpdate();

      return new ModelAndView("action-done", "message", "Параметры изменены");
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }

  @Autowired
  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }
}
