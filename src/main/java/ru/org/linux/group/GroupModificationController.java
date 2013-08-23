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

package ru.org.linux.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.Template;

import javax.servlet.ServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class GroupModificationController {
  @Autowired
  private GroupDao groupDao;

  @Autowired
  private GroupInfoPrepareService prepareService;

  @RequestMapping(value="/groupmod.jsp", method = RequestMethod.GET)
  public ModelAndView showForm(@RequestParam("group") int id, ServletRequest request) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    Group group = groupDao.getGroup(id);

    ModelAndView mv = new ModelAndView("groupmod", "group", group);

    mv.getModel().put("groupInfo", prepareService.prepareGroupInfo(group, request.isSecure()));

    return mv;
  }

  @RequestMapping(value="/groupmod.jsp", method = RequestMethod.POST)
  public ModelAndView modifyGroup(
    @RequestParam("group") int id,
    @RequestParam("title") String title,
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

    Group group = groupDao.getGroup(id);

    if (preview != null) {
      group.setTitle(title);
      group.setInfo(info);
      group.setLongInfo(longInfo);

      Map<String, Object> params = new HashMap<>();
      params.put("group", group);
      params.put("groupInfo", prepareService.prepareGroupInfo(group, request.isSecure()));
      params.put("preview", true);

      return new ModelAndView("groupmod", params);
    }

    groupDao.setParams(group, title, info, longInfo, resolvable!=null, urlName);

    return new ModelAndView("action-done", "message", "Параметры изменены");
  }
}
