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

import java.net.URLEncoder;
import java.sql.Connection;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;
import com.danga.MemCached.MemCachedClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.*;
import ru.org.linux.util.ServletParameterException;

@Controller
public class ShowCommentsController {
  @RequestMapping("/show-comments.jsp")
  public ModelAndView showComments(
    @RequestParam String nick,
    @RequestParam(defaultValue="0") int offset,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView mv = new ModelAndView("show-comments");

    int topics = 50;
    mv.getModel().put("topics", topics);

    if (offset<0) {
      throw new ServletParameterException("offset<0!?");
    }

    mv.getModel().put("offset", offset);

    boolean firstPage = offset==0;

    if (firstPage) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 90 * 1000);
    } else {
      response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 60 * 1000L);
    }

    mv.getModel().put("firstPage", firstPage);

    Connection db = null;

    MemCachedClient mcc= MemCachedSettings.getClient();
    String showCommentsId = MemCachedSettings.getId( "show-comments?id="+ URLEncoder.encode(nick)+"&offset="+offset);

    try {
      db = LorDataSource.getConnection();

      User user = User.getUser(db, nick);

      mv.getModel().put("user", user);

      if (user.isAnonymous()) {
        throw new UserErrorException("Функция только для зарегистрированных пользователей");
      }

      String res = (String) mcc.get(showCommentsId);

      if (res == null) {
        res = MessageTable.showComments(db, user, offset, topics);

        if (firstPage) {
          mcc.add(showCommentsId, res, new Date(new Date().getTime() + 90 * 1000));
        } else {
          mcc.add(showCommentsId, res, new Date(new Date().getTime() + 60 * 60 * 1000L));
        }
      }

      mv.getModel().put("list", res);

      return mv;
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }
}
