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

package ru.org.linux.user;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.auth.FileProfileReader;
import ru.org.linux.site.DefaultProfile;
import ru.org.linux.spring.Configuration;
import ru.org.linux.spring.validators.EditBoxesRequestValidator;
import ru.org.linux.storage.StorageException;

import static ru.org.linux.auth.AuthUtil.*;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Controller
@SessionAttributes("allboxes")
public class AddRemoveBoxesController {
  @Autowired
  private Configuration configuration;


  @RequestMapping(value = {"/remove-box.jsp", "/add-box.jsp"}, method = RequestMethod.GET)
  public ModelMap showRemove(@RequestParam String tag,
                             @RequestParam(required = false) Integer pos,
                             ServletRequest request)
    throws AccessViolationException {

    if (!isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    ModelMap result = new ModelMap();
    EditBoxesRequest form = new EditBoxesRequest();
    form.setPosition(pos);
    form.setTag(tag);
    result.addAttribute("form", form);
    return result;
  }

  @RequestMapping(value = "/remove-box.jsp", method = RequestMethod.POST)
  public String doRemove(@ModelAttribute("form") EditBoxesRequest form, BindingResult result,
                         SessionStatus status, HttpServletRequest request)
    throws Exception {

    if (!isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    new EditBoxesRequestValidator().validate(form, result);
    ValidationUtils.rejectIfEmptyOrWhitespace(result, "position", "position.empty", "Не указанa позиция бокслета");
    if (result.hasErrors()) {
      return "remove-box";
    }

    if (result.hasErrors()) {
      return "remove-box";
    }

    String objectName = getObjectName(form, request);
    List<String> boxlets = new ArrayList<String>(getProf().getList(objectName));

    if (!boxlets.isEmpty()) {
      if (boxlets.size() > form.position) {
        boxlets.remove(form.position.intValue());
        getProf().setList(objectName, boxlets);
        new FileProfileReader(configuration).writeProfile(getNick(), getCurrentProfile());
      }
    }
    
    status.setComplete();
    return "redirect:/edit-boxes.jsp";
  }

  @ModelAttribute("allboxes")
  public Set<String> getAllBoxes() {
    return DefaultProfile.getAllBoxes();
  }

  @RequestMapping(value = "/add-box.jsp", method = RequestMethod.POST)
  public String doAdd(@ModelAttribute("form") EditBoxesRequest form, BindingResult result,
                      SessionStatus status, HttpServletRequest request)
    throws IOException,
          AccessViolationException, StorageException {

    new EditBoxesRequestValidator().validate(form, result);
    ValidationUtils.rejectIfEmptyOrWhitespace(result, "boxName", "boxName.empty", "Не выбран бокслет");
    if (StringUtils.isNotEmpty(form.getBoxName()) && !DefaultProfile.isBox(form.getBoxName())) {
      result.addError(new FieldError("boxName", "boxName.invalid", "Неверный бокслет"));
    }
    if (result.hasErrors()) {
      return "add-box";
    }
    if (result.hasErrors()) {
      return "add-box";
    }

    if (form.getPosition() == null) {
      form.setPosition(0);
    }

    String objectName = getObjectName(form, request);
    List<String> boxlets = new ArrayList<String>(getProf().getList(objectName));

    CollectionUtils.filter(boxlets, DefaultProfile.getBoxPredicate());

    if (boxlets.size() > form.position) {
      boxlets.add(form.position, form.boxName);
    } else {
      boxlets.add(form.boxName);
    }
    
    getProf().setList(objectName, boxlets);
    new FileProfileReader(configuration).writeProfile(getNick(), getCurrentProfile());
    status.setComplete();
    return "redirect:/edit-boxes.jsp";
  }

  private static String getObjectName(EditBoxesRequest form, HttpServletRequest request)  {
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

  public static class EditBoxesRequest {
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
