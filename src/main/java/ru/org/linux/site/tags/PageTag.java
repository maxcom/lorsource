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

import ru.org.linux.util.Pagination;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

/**
 */
public class PageTag extends SimpleTagSupport {
  private Pagination pagination;
  private String baseUrl;

  public void setPagination(Pagination pagination) {
    this.pagination = pagination;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  private final static int MAX_PAGE_HTML_LENGTH = 8;
  private final static int PRE_LAST_PAGE_LENGTH = 5;

  @Override
  public void doTag() throws JspException, IOException {
    StringBuilder sb = new StringBuilder();
    if (pagination == null || pagination.getItems() == null
    				|| pagination.getItems().size() == 0) {
      getJspContext().getOut().print("");
      return;
    }

    if (pagination.getCount() > 1) {
      if (pagination.getIndex() == 1) {
        sb.append(createPrePage(0, true));
      } else {
        sb.append(createPrePage(pagination.getIndex() - 1, false));
      }
    }

    for(int i=1; i<= pagination.getCount(); i++) {
      if(i == pagination.getIndex()) {
        sb.append(createPageIndex(i, true));
      } else {
        sb.append(createPageIndex(i, false));
      }
    }

    if (pagination.getCount() > 1) {
      if (pagination.getIndex() == pagination.getCount()) {
        sb.append(createNextPage(0, true));
      } else {
        sb.append(createNextPage(pagination.getIndex() + 1, false));
      }
    }
    getJspContext().getOut().print(sb.toString());
  }

  private String createPrePage(int pageIndex, boolean distable) {
    StringBuilder sb = new StringBuilder();
    if (distable) {
      sb.append("<span class='page-number'>←</span>");
    } else {
      sb.append("<a class='page-number' href='").append(baseUrl).append('?').append("page=").append(pageIndex).append("'>←</a>");
    }
    return sb.toString();
  }

  private String createNextPage(int pageIndex, boolean distable) {
    StringBuilder sb = new StringBuilder();
    if (distable) {
      sb.append(" <span class='page-number'>→</span>");
    } else {
      sb.append(" <a class='page-number' href='").append(baseUrl).append('?').append("page=").append(pageIndex).append("'>→</a>");
    }
    return sb.toString();
  }

  private String createPageIndex(int pageIndex, boolean cur) {
    StringBuilder sb = new StringBuilder();
    if (!cur) {
      sb.append(" <a class='page-number' href='").append(baseUrl).append('?').append("page=").append(pageIndex).append("'>")
          .append(pageIndex)
          .append("</a>");
    } else {
      sb.append(" <strong class='page-number'>").append(pageIndex).append("</strong>");
    }
    return sb.toString();
  }

}
