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

package ru.org.linux.paginator;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class PagesInfo {
  private final ImmutableList<Page> pageLinks;

  private final boolean hasPrevious;
  private final boolean hasNext;

  private final String previous;
  private final String next;

  public PagesInfo(List<String> pageLinks, int currentPage) {
    ImmutableList.Builder<Page> builder = ImmutableList.builder();

    for (int i=0; i<pageLinks.size(); i++) {
      builder.add(new Page(pageLinks.get(i), i, i==currentPage ));
    }

    this.pageLinks = builder.build();

    hasPrevious = currentPage > 0;
    hasNext = currentPage>=0 && (currentPage < pageLinks.size() - 1);

    if (hasPrevious) {
      previous = this.pageLinks.get(currentPage - 1).getUrl();
    } else {
      previous = null;
    }

    if (hasNext) {
      next = this.pageLinks.get(currentPage + 1).getUrl();
    } else {
      next = null;
    }
  }

  public ImmutableList<Page> getPageLinks() {
    return pageLinks;
  }

  public boolean isHasPrevious() {
    return hasPrevious;
  }

  public boolean isHasNext() {
    return hasNext;
  }

  public String getPrevious() {
    return previous;
  }

  public String getNext() {
    return next;
  }

  public static class Page {
    private final String url;
    private final int index;
    private final boolean current;

    public Page(String url, int index, boolean current) {
      this.url = url;
      this.index = index;
      this.current = current;
    }

    public String getUrl() {
      return url;
    }

    public int getIndex() {
      return index;
    }

    public boolean isCurrent() {
      return current;
    }
  }
}
