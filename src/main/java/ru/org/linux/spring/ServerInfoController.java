/*
 * Copyright 1998-2016 Linux.org.ru
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.user.User;
import ru.org.linux.user.UserService;

import java.util.List;

@Controller
public class ServerInfoController {
  @Autowired
  private UserService userService;

  @RequestMapping("/about")
  public ModelAndView serverInfo() {
    List<User> moderators = userService.getModerators();

    ModelAndView mv = new ModelAndView("server");
    mv.getModel().put("moderators", moderators);

    List<User> correctors = userService.getCorrectors();

    mv.getModel().put("correctors", correctors);

    return mv;
  }
}
