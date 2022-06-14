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

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.Template;

import javax.servlet.ServletRequest;
import java.util.List;

@Controller
public class ShowRemarkController {
  @Autowired
  private UserDao userDao;

  @Autowired
  private RemarkDao remarkDao;

  @Autowired
  private PreparedRemarkService prepareService;
  
  @RequestMapping("/people/{nick}/remarks")
  public ModelAndView showRemarks(ServletRequest request
    , @PathVariable String nick
    , @RequestParam(value = "offset", defaultValue = "0") int offset
    , @RequestParam(value = "sort", defaultValue = "0") int sortorder
    ) {
    Template tmpl = Template.getTemplate(request);
    if (!tmpl.isSessionAuthorized() || !Template.getCurrentUser().getNick().equals(nick)) {
      throw new AccessViolationException("Not authorized");
    }

    int count = remarkDao.remarkCount(Template.getCurrentUser());

    ModelAndView mv = new ModelAndView("view-remarks");

    int limit = tmpl.getProf().getMessages();

    if(count > 0 ){
      if( offset >= count ){
        throw new UserErrorException("Offset is too long");
      }
      if( offset < 0 ) offset=0;

      if( sortorder != 1 ){
        sortorder = 0;
        mv.getModel().put("sortorder","");
      }  else {
        mv.getModel().put("sortorder","&amp;sort=1");
      }


      List<Remark> remarks = remarkDao.getRemarkList(Template.getCurrentUser(), offset, sortorder, limit);
      List<PreparedRemark> preparedRemarks = prepareService.prepareRemarkList(remarks);

      mv.getModel().put("remarks", preparedRemarks);
    } else {
      mv.getModel().put("remarks", ImmutableList.of() );
    }
    mv.getModel().put("offset",offset);
    mv.getModel().put("limit",limit);
    mv.getModel().put("hasMore",(count > (offset+limit)) );

    return mv;
  }
}
