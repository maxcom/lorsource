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

import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.*;
import ru.org.linux.spring.commons.CacheProvider;
import ru.org.linux.util.ServletParameterException;

@Controller
public class ShowCommentsController {
  private CacheProvider cacheProvider;

  @Autowired
  public void setCacheProvider(CacheProvider cacheProvider) {
    this.cacheProvider = cacheProvider;
  }

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

    String showCommentsId = MemCachedSettings.getId( "show-comments?id="+ URLEncoder.encode(nick)+"&offset="+offset);

    try {
      db = LorDataSource.getConnection();

      User user = User.getUser(db, nick);

      mv.getModel().put("user", user);

      if (user.isAnonymous()) {
        throw new UserErrorException("Функция только для зарегистрированных пользователей");
      }

      String res = (String) cacheProvider.getFromCache(showCommentsId);

      if (res == null) {
        res = MessageTable.showComments(db, user, offset, topics);

        if (firstPage) {
          cacheProvider.storeToCache(showCommentsId, res, 90 * 1000);
        } else {
          cacheProvider.storeToCache(showCommentsId, res, 60 * 60 * 1000);
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
