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
package ru.org.linux.util.paginator;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class PageTag extends SimpleTagSupport {
  private PreparedPagination preparedPagination;
  private UriTemplate baseTemplate;
  private UriTemplate pageTemplate;

  public void setPreparedPagination(PreparedPagination preparedPagination) {
    this.preparedPagination = preparedPagination;
  }

  /**
   * @param template like /forum/talks/12345
   */
  public void setBaseTemplate(String template) {
    this.baseTemplate = new UriTemplate(template);
  }

  /**
   * @param template like /forum/talks/12345/page{page}
   */
  public void setPageTemplate(String template) {
    this.pageTemplate = new UriTemplate(template);
  }

  private String templateToString(int page) {
    URI uri;
    if(page <= 1) {
      uri = baseTemplate.expand();
    } else {
      uri = pageTemplate.expand(page);
    }
    UriComponentsBuilder builder = UriComponentsBuilder.fromUri(uri);
    String lastmod = preparedPagination.getLastmod();
    String filter = preparedPagination.getFilter();
    if(lastmod != null && !lastmod.isEmpty()) {
      builder.queryParam("lastmod", lastmod);
    }
    if(filter != null && !filter.isEmpty()) {
      builder.queryParam("filter", filter);
    }
    return builder.build().toUriString();
  }

  @Override
  public void doTag() throws JspException, IOException {
    StringBuilder sb = new StringBuilder();
    if (preparedPagination == null || preparedPagination.getItems() == null
    				|| preparedPagination.getItems().size() == 0) {
      getJspContext().getOut().print("");
      return;
    }

    if (preparedPagination.getCount() > 1) {
      if (preparedPagination.getIndex() == 1) {
        sb.append(createPrePage(0, true));
      } else {
        sb.append(createPrePage(preparedPagination.getIndex() - 1, false));
      }
    }

    for(int i=1; i<= preparedPagination.getCount(); i++) {
      if(i == preparedPagination.getIndex()) {
        sb.append(createPageIndex(i, true));
      } else {
        sb.append(createPageIndex(i, false));
      }
    }

    if (preparedPagination.getCount() > 1) {
      if (preparedPagination.getIndex() == preparedPagination.getCount()) {
        sb.append(createNextPage(0, true));
      } else {
        sb.append(createNextPage(preparedPagination.getIndex() + 1, false));
      }
    }
    getJspContext().getOut().print(sb.toString());
  }

  private String createPrePage(int pageIndex, boolean distable) {
    StringBuilder sb = new StringBuilder();
    if (distable) {
      sb.append("<span class='page-number'>←</span>");
    } else {
      sb.append("<a class='page-number' href='").append(templateToString(pageIndex)).append("'>←</a>");
    }
    return sb.toString();
  }

  private String createNextPage(int pageIndex, boolean distable) {
    StringBuilder sb = new StringBuilder();
    if (distable) {
      sb.append(" <span class='page-number'>→</span>");
    } else {
      sb.append(" <a class='page-number' href='").append(templateToString(pageIndex)).append("'>→</a>");
    }
    return sb.toString();
  }

  private String createPageIndex(int pageIndex, boolean cur) {
    StringBuilder sb = new StringBuilder();
    if (!cur) {
      sb.append(" <a class='page-number' href='").append(templateToString(pageIndex)).append("'>")
          .append(pageIndex)
          .append("</a>");
    } else {
      sb.append(" <strong class='page-number'>").append(pageIndex).append("</strong>");
    }
    return sb.toString();
  }

}
