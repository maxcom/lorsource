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


package ru.org.linux.site.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;

import static ru.org.linux.util.StringUtil.makeTitle;

/**
 */
public class MakeTitleTag extends BodyTagSupport {
  @Override
  public int doAfterBody() throws JspException {
    try {
      BodyContent bc = getBodyContent();
      String body = bc.getString();
      JspWriter out = bc.getEnclosingWriter();
      out.print(makeTitle(body));
    } catch (IOException e) {
      throw new JspException("Error:" + e.getMessage());
    }
    return SKIP_BODY;
  }
}
