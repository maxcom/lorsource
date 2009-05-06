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
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.FieldError;
import org.springframework.context.support.ApplicationObjectSupport;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.org.linux.site.AccessViolationException;
import ru.org.linux.site.Template;
import ru.org.linux.site.User;
import ru.org.linux.site.cli.mkdefprofile;
import ru.org.linux.util.UtilException;
import ru.org.linux.storage.StorageException;
import ru.org.linux.spring.validators.EditBoxesFormValidator;

@Controller
@SessionAttributes("allboxes")
public class AddRemoveBoxesController extends ApplicationObjectSupport {

  private static final Log log = LogFactory.getLog(AddRemoveBoxesController.class);

  @RequestMapping(value = {"/remove-box.jsp", "/add-box.jsp"}, method = RequestMethod.GET)
  public ModelMap showRemove(@RequestParam String tag,
                             @RequestParam(required = false) Integer pos)
    throws AccessViolationException, UtilException {
    ModelMap result = new ModelMap();
    EditBoxesForm form = new EditBoxesForm();
    form.setPosition(pos);
    form.setTag(tag);
    result.addAttribute("form", form);
    return result;
  }

  @RequestMapping(value = "/remove-box.jsp", method = RequestMethod.POST)
  public String doRemove(@ModelAttribute("form") EditBoxesForm form, BindingResult result,
                         SessionStatus status, HttpServletRequest request)
    throws IOException,
    UtilException, AccessViolationException, StorageException {
    new EditBoxesFormValidator().validate(form, result);
    ValidationUtils.rejectIfEmptyOrWhitespace(result, "position", "position.empty", "Не указанa позиция бокслета");
    if (result.hasErrors()) {
      return "remove-box";
    }
    Template t = Template.getTemplate(request);
    checkUserPassword(form, result, t);

    if (result.hasErrors()) {
      return "remove-box";
    }

    String objectName = getObjectName(form, request);
    final List boxlets = (List) t.getProf().getObject(objectName);
    if (boxlets != null && !boxlets.isEmpty()) {
      if (boxlets.size() > form.position) {
        boxlets.remove(form.position.intValue());
        t.getProf().setObject(objectName, boxlets);
        t.writeProfile(t.getProfileName());
      }
    }
    status.setComplete();
    return "redirect:/edit-boxes.jsp";
  }

  private void checkUserPassword(EditBoxesForm form, BindingResult result, final Template t) {
    final DataSource ds = (DataSource) getApplicationContext().getBean("datasource");
    try {
      User user = User.getUser(ds.getConnection(), t.getProfileName());
      user.checkAnonymous();
      user.checkPassword(form.getPassword());
    } catch (Exception e) {
      log.error(e);
      result.addError(new FieldError("password", "password.invalid", "Неправильный пароль"));
    }
  }

  @ModelAttribute("allboxes")
  public String[] getAllBoxes() {
    return mkdefprofile.getBoxlist();
  }

  @RequestMapping(value = "/add-box.jsp", method = RequestMethod.POST)
  public String doAdd(@ModelAttribute("form") EditBoxesForm form, BindingResult result,
                      SessionStatus status, HttpServletRequest request)
    throws IOException,
    UtilException, AccessViolationException, StorageException {

    new EditBoxesFormValidator().validate(form, result);
    ValidationUtils.rejectIfEmptyOrWhitespace(result, "boxName", "boxName.empty", "Не выбран бокслет");
    if (StringUtils.isNotEmpty(form.getBoxName()) && !mkdefprofile.isBox(form.getBoxName())) {
      result.addError(new FieldError("boxName", "boxName.invalid", "Неверный бокслет"));
    }
    if (result.hasErrors()) {
      return "add-box";
    }
    Template t = Template.getTemplate(request);
    checkUserPassword(form, result, Template.getTemplate(request));

    if (result.hasErrors()) {
      return "add-box";
    }

    if (form.getPosition() == null) {
      form.setPosition(0);
    }

    String objectName = getObjectName(form, request);
    final List boxlets = (List) t.getProf().getObject(objectName);
    if (boxlets != null && !boxlets.isEmpty()) {
      if (boxlets.size() > form.position) {
        boxlets.add(form.position.intValue(), form.boxName);
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
      if (EditBoxesController.getThreeColumns(request)) {
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
    private String boxName;

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

    public String getBoxName() {
      return boxName;
    }

    public void setBoxName(String boxName) {
      this.boxName = boxName;
    }
  }
}
