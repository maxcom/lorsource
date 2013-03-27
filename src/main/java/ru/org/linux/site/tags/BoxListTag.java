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

package ru.org.linux.site.tags;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import ru.org.linux.site.DefaultProfile;
import ru.org.linux.site.Template;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.util.List;


public class BoxListTag extends BodyTagSupport {
  private String var;

  public String getVar() {
    return var;
  }

  public void setVar(String var) {
    this.var = var;
  }

  @Override
  public int doStartTag() throws JspException {
    Template t = Template.getTemplate(pageContext.getRequest());
    List<String> boxnames = t.getProf().getBoxlets();
    CollectionUtils.filter(boxnames, new Predicate() {
      @Override
      public boolean evaluate(Object o) {
        String s = (String) o;
        return DefaultProfile.isBox(s);
      }
    });
    pageContext.setAttribute(var, boxnames);
    return EVAL_BODY_INCLUDE;
  }

  @Override
  public int doEndTag() throws JspException {
    pageContext.removeAttribute(var);
    return SKIP_BODY;
  }
}
