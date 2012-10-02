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

import ru.org.linux.topic.Topic;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;

/**
 */
public class TopicPaginator extends TagSupport {
  private Topic topic;
  private int topicsPerPage;

  public void setTopic(Topic topic) {
    this.topic = topic;
  }

  public void setTopicsPerPage(int topicPerPage) {
    this.topicsPerPage = topicPerPage;
  }

  @Override
  public int doStartTag() throws JspException {
    JspWriter out = pageContext.getOut();
    int pages = topic.getPageCount(topicsPerPage);
    try {
      if (pages != 1) {
        int PG_COUNT = 3;

        out.append("&nbsp;(стр.");
        boolean dots = false;

        for (int i = 1; i < pages; i++) {
          if (pages > PG_COUNT * 3 && (i > PG_COUNT && i < pages - PG_COUNT)) {
            if (!dots) {
              out.append(" ...");
              dots = true;
            }

            continue;
          }
          out.append(" <a href=\"").append(topic.getLinkPage(i)).append("\">").append(Integer.toString(i + 1)).append("</a>");
        }
        out.append(')');
      }
    } catch (IOException e) {
      throw new JspException("Error:" + e.getMessage());
    }
    return SKIP_BODY;
  }

}
