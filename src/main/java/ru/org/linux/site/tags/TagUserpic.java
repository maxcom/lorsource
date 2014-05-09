/*
 * Copyright 1998-2014 Linux.org.ru
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

import ru.org.linux.user.Userpic;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;

/**
 * userpic tag
 */
public class TagUserpic extends TagSupport {
  private Userpic userpic;

  public void setUserpic(Userpic author) {
    this.userpic = author;
  }

  @Override
  public int doStartTag() throws JspException {
    JspWriter out = pageContext.getOut();

    try {
       out
            .append("<div class=\"userpic\">")
            .append("<img class=\"photo\" src=\"")
            .append(userpic.getUrl())
            .append("\" alt=\"\" width="+userpic.getWidth()+" height="+userpic.getHeight())
            .append(" >")
            .append("</div>");
    } catch (IOException ex) {
      throw new JspException(ex);
    }

    return SKIP_BODY;
  }
}
