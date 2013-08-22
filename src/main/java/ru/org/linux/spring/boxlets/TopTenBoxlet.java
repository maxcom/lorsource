/*
 * Copyright 1998-2013 Linux.org.ru
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

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.site.Template;
import ru.org.linux.topic.TopTenDao;
import ru.org.linux.topic.TopTenDao.TopTenMessageDTO;
import ru.org.linux.user.Profile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class TopTenBoxlet extends AbstractBoxlet {
  private TopTenDao topTenDao;

  public TopTenDao getTopTenDao() {
    return topTenDao;
  }

  @Autowired
  public void setTopTenDao(TopTenDao topTenDao) {
    this.topTenDao = topTenDao;
  }

  @Override
  @RequestMapping("/top10.boxlet")
  protected ModelAndView getData(HttpServletRequest request) {
    Profile profile = Template.getTemplate(request).getProf();
    final int itemsPerPage = profile.getMessages();

    List<TopTenMessageDTO> list = topTenDao.getMessages();
    CollectionUtils.forAllDo(list, new Closure() {
      @Override
      public void execute(Object o) {
        TopTenMessageDTO dto = (TopTenMessageDTO) o;
        int tmp = dto.getAnswers() / itemsPerPage;
        tmp = (dto.getAnswers() % itemsPerPage > 0) ? tmp + 1 : tmp;
        dto.setPages(tmp);
      }
    });

    Map<String, Object> params = new HashMap<>();
    params.put("messages", list);

    return new ModelAndView("boxlets/top10", params);
  }
}
