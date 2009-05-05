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

package ru.org.linux.spring;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.AccessViolationException;
import ru.org.linux.site.Template;
import ru.org.linux.site.User;
import ru.org.linux.spring.validators.EditBoxesFormValidator;
import ru.org.linux.storage.StorageException;
import ru.org.linux.util.UtilException;

@Controller
public class EditBoxesController extends ApplicationObjectSupport {

  private static final Log log = LogFactory.getLog(EditBoxesController.class);

  @RequestMapping(value = "/edit-boxes.jsp")
  public ModelAndView view(HttpServletRequest request) throws AccessViolationException, UtilException {
    boolean isThreeColumn = getThreeColumns(request);
    ModelAndView result = new ModelAndView("edit-boxes");
    result.addObject("isThreeColumn", isThreeColumn);
    return result;
  }

  private boolean getThreeColumns(HttpServletRequest request)
    throws AccessViolationException, UtilException {
    Template t = Template.getTemplate(request);
    if (t.isUsingDefaultProfile() || t.getProfileName().charAt(0) == '_') {
      throw new AccessViolationException("нельзя изменить системный профиль; создайте сначала свой");
    }
    boolean isThreeColumn = t.getProf().getBoolean("main.3columns");
    return isThreeColumn;
  }

  @RequestMapping(value = "/remove-box.jsp", method = RequestMethod.GET)
  public ModelAndView showRemove(@RequestParam(required = true) String tag,
                                 @RequestParam(required = true) Integer pos)
    throws AccessViolationException, UtilException {
    log.debug("showRemove()");
    ModelAndView result = new ModelAndView("remove-box");
    EditBoxesForm form = new EditBoxesForm();
    form.setPosition(pos);
    form.setTag(tag);
    result.addObject("form", form);
    return result;
  }

  @RequestMapping(value = "/remove-box.jsp", method = RequestMethod.POST)
  public String doRemove(@ModelAttribute("form") EditBoxesForm form, BindingResult result,
                         SessionStatus status, HttpServletRequest request)
    throws IOException,
    UtilException, AccessViolationException, StorageException {
    new EditBoxesFormValidator().validate(form, result);
    if (result.hasErrors()) {
      return "remove-box";
    }
    final DataSource ds = (DataSource) getApplicationContext().getBean("datasource");
    Template t = Template.getTemplate(request);
    try {
      User user = User.getUser(ds.getConnection(), t.getProfileName());
      user.checkAnonymous();
      user.checkPassword(request.getParameter("password"));
    } catch (Exception e){
      log.error(e);
      result.addError(new ObjectError("password", "Неправильный пароль"));
    }

    if (result.hasErrors()){
      return "remove-box";
    }

    String objectName = getObjectName(form, request);
    final List boxlets = (List) t.getProf().getObject(objectName);
    if (boxlets != null && ! boxlets.isEmpty()){
      if (boxlets.size() > form.position){
        boxlets.remove(form.position.intValue());
        t.getProf().setObject(objectName, boxlets);
        t.writeProfile(t.getProfileName());
      }
    }
    status.setComplete();
    return "redirect:/edit-boxes.jsp";
  }

  private String getObjectName(EditBoxesForm form, HttpServletRequest request) throws AccessViolationException, UtilException {
    String objectName;
    if ("left".equals(form.getTag())) {
      if (getThreeColumns(request)) {
        objectName = "main3-1";
      } else {
        objectName = "main2";
      }
    } else {
      objectName = "main3-2";
    }
    return objectName;
  }

  public static class EditBoxesForm {
    private Integer position;
    private String tag;
    private String password;

    public Integer getPosition() {
      return position;
    }

    public void setPosition(Integer position) {
      this.position = position;
    }

    public String getTag() {
      return tag;
    }

    public void setTag(String tag) {
      this.tag = tag;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}
