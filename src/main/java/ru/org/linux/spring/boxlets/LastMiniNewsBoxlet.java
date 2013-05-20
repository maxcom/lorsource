/*
 * Copyright 1998-2012 Linux.org.ru
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.site.Template;
import ru.org.linux.topic.LastMiniNewsDao;
import ru.org.linux.user.Profile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 */
@Controller
public class LastMiniNewsBoxlet extends AbstractBoxlet{

  @Autowired
  private LastMiniNewsDao lastMiniNewsDao;

  @Override
  @RequestMapping("/lastMiniNews.boxlet")
  protected ModelAndView getData(HttpServletRequest request) {
    Profile profile = Template.getTemplate(request).getProf();
    String style = profile.getStyle();

    Map<String, Object> params = new HashMap<>();
    params.put("topics", lastMiniNewsDao.getTopics());
    params.put("style", style);

    return new ModelAndView("boxlets/lastMiniNews", params);
  }
}
