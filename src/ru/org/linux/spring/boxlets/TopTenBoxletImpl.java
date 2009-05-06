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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Closure;

import ru.org.linux.spring.dao.TopTenDaoImpl;
import ru.org.linux.site.Template;
import ru.org.linux.util.ProfileHashtable;

/**
 * User: rsvato
 * Date: May 4, 2009
 * Time: 1:56:02 PM
 */
@Controller
public class TopTenBoxletImpl extends SpringBoxlet {
  private TopTenDaoImpl topTenDao;

  public TopTenDaoImpl getTopTenDao() {
    return topTenDao;
  }
  @Autowired
  public void setTopTenDao(TopTenDaoImpl topTenDao) {
    this.topTenDao = topTenDao;
  }

  @RequestMapping("/top10.boxlet")
  protected ModelAndView getData(HttpServletRequest request, HttpServletResponse response) {
    List<TopTenDaoImpl.TopTenMessageDTO> list = getTopTenDao().getMessages();
    final ProfileHashtable profile = Template.getTemplate(request).getProf();
    final int itemsPerPage = profile.getInt("messages");
    final String style = profile.getString("style");
    CollectionUtils.forAllDo(list, new Closure() {
      public void execute(Object o) {
        TopTenDaoImpl.TopTenMessageDTO dto = (TopTenDaoImpl.TopTenMessageDTO) o;
        dto.setPages((int) Math.ceil(dto.getAnswers() / itemsPerPage));
      }
    });
    final ModelAndView view = new ModelAndView("boxlets/top10", "messages", list);
    view.addObject("style", style);
    view.addObject("perPage", itemsPerPage);
    return view;
  }
}
