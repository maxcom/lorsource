/*
 * Copyright 1998-2019 Linux.org.ru
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import ru.org.linux.site.DefaultProfile;
import ru.org.linux.site.Template;

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
  public int doStartTag() {
    Template t = Template.getTemplate();

    List<String> boxnames = ImmutableList.copyOf(
            Iterables.filter(t.getProf().getBoxlets(), DefaultProfile.boxPredicate()::test)
    );

    pageContext.setAttribute(var, boxnames);
    return EVAL_BODY_INCLUDE;
  }

  @Override
  public int doEndTag() {
    pageContext.removeAttribute(var);
    return SKIP_BODY;
  }
}
