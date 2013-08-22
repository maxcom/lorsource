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

import ru.org.linux.tag.TagService;
import ru.org.linux.topic.TagTopicListController;
import ru.org.linux.util.StringUtil;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.util.List;

/**
 * tags tag
 */
public class TagsTag extends TagSupport {
  private List<String> list;

  public void setList(List<String> list) {
    this.list = list;
  }


  @Override
  public int doStartTag() throws JspException {
    JspWriter out = pageContext.getOut();
    if (list != null) {
      try {
        StringBuilder buf = new StringBuilder();
        for (String el : list) {
          if (buf.length() > 0) {
            buf.append(", ");
          }
          if (TagService.isGoodTag(el)) {
            buf
                .append("<a class=tag rel=tag href=\"")
                .append(TagTopicListController.tagListUrl(el))
                .append("\">")
                .append(StringUtil.escapeHtml(el))
                .append("</a>");
          } else {
            buf.append(StringUtil.escapeHtml(el));
          }
        }
        out.append(buf);
      } catch (IOException e) {
        throw new JspException("Error:" + e.getMessage());
      }
    }
    return SKIP_BODY;
  }
}
