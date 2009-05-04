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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Closure;

import ru.org.linux.spring.dao.TopTenDaoImpl;
import ru.org.linux.site.Template;

/**
 * User: rsvato
 * Date: May 4, 2009
 * Time: 1:56:02 PM
 */
public class TopTenBoxletImpl extends SpringBoxlet {
  private TopTenDaoImpl topTenDao;

  public TopTenDaoImpl getTopTenDao() {
    return topTenDao;
  }

  public void setTopTenDao(TopTenDaoImpl topTenDao) {
    this.topTenDao = topTenDao;
  }

  protected ModelAndView getData(HttpServletRequest request, HttpServletResponse response) {
    List<TopTenDaoImpl.TopTenMessageDTO> list = getTopTenDao().getMessages();
    final int itemsPerPage = Template.getTemplate(request).getProf().getInt("messages");
    CollectionUtils.forAllDo(list, new Closure() {
      public void execute(Object o) {
        TopTenDaoImpl.TopTenMessageDTO dto = (TopTenDaoImpl.TopTenMessageDTO) o;
        dto.setPages((int) Math.ceil(dto.getAnswers() / itemsPerPage));
      }
    });
    return new ModelAndView("boxlets/top10", "messages", list);
  }
}
