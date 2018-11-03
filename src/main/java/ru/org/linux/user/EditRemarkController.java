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

package ru.org.linux.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.Template;
import scala.Option;

import javax.servlet.ServletRequest;

@Controller
@RequestMapping("/people/{nick}/remark")
public class EditRemarkController {
  @Autowired
  private UserService userService;

  @Autowired
  private RemarkDao remarkDao;

  @RequestMapping(method=RequestMethod.GET)
  public ModelAndView showForm(ServletRequest request, @PathVariable String nick) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    ModelAndView mv = new ModelAndView("edit-remark");

    User user = userService.getUserCached(nick);
    if (tmpl.isSessionAuthorized() && !tmpl.getNick().equals(nick) ) {
      Option<Remark> remark = remarkDao.getRemark(tmpl.getCurrentUser(), user);
      if (remark.isDefined()) {
        mv.getModel().put("remark", remark.get());
      }
    } else {
      throw new AccessViolationException("Not Authorized");
    }
    return mv;
  }

  @RequestMapping(method=RequestMethod.POST)
  public ModelAndView editProfile(
          ServletRequest request,
          @RequestParam("text") String text,
          @PathVariable String nick
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }
    if(text.length()>255){
      text=text.substring(0,255);
    }
    User user = tmpl.getCurrentUser();
    User refUser = userService.getUserCached(nick);
    remarkDao.setOrUpdateRemark(user, refUser, text);

    return new ModelAndView(new RedirectView("/people/" + nick + "/profile"));
  }
}
