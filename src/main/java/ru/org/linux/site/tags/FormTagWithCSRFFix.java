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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.tags.form.FormTag;
import org.springframework.web.servlet.tags.form.TagWriter;

import javax.servlet.jsp.JspException;

/**
 */
public class FormTagWithCSRFFix extends FormTag{
  private TagWriter tagWriter;

  @Override
  protected int writeTagContent(TagWriter tagWriter) throws JspException {
    this.tagWriter = tagWriter;
    return super.writeTagContent(tagWriter);
  }

  @Override
  public int doEndTag() throws JspException {
    if("POST".equals(getMethod().toUpperCase()) ||
        "PUT".equals(getMethod().toUpperCase()) ||
        "DELETE".equals(getMethod().toUpperCase())) {
      return super.doEndTag();
    } else {
      this.tagWriter.endTag();
      return EVAL_PAGE;
    }
  }
}
