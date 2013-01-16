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

import java.io.Serializable;
import java.util.List;

/**
 * Подготовка данных для отображения в View
 */
public class PreparedPagination<T> implements Serializable {

  /**
   * Записи на текущей странице списка.
   */
  private List<T> items;
  /**
   * Всего записей в списке.
   */
  private int total;

  /**
   *
   */
  private int start;

  /**
   *
   */
  private int end;

  /**
   *
   */
  private int count;

  /**
   * Объект пагинации. полученный из GET-запроса
   */
  private final Pagination pagination;

  public PreparedPagination(Pagination pagination) {
    this.pagination = pagination;
    int size = pagination.getSize();
    start = size * (pagination.getIndex() - 1) + 1;
    end = start + size - 1;
  }

  public List<T> getItems() {
    return items;
  }

  public void setItems(List<T> items) {
    this.items = items;
  }

  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;

    int size = pagination.getSize();
    int index = pagination.getIndex();

    if (total < size) {
      count = 1;
    } else {
      if (total % size == 0) {
        count = total / size;
      } else {
        count = total / size + 1;
      }
    }
    if (index > count) {
      index = count;
      start = size * (index - 1) + 1;
      end = start + size - 1;
    }
  }

  public int getEnd() {
    if (end > total) {
      return total;
    }
    return end;
  }

  public int getStart() {
    return start;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public void setEnd(int end) {
    this.end = end;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  /**
   * Делегатор для получения номера текущей страницы.
   *
   * @return номер текущей страницы.
   */
  public int getIndex() {
    return pagination.getIndex();
  }

  /**
   * Делегатор для получения размера страницы.
   *
   * @return размер страницы.
   */
  public int getSize() {
    return pagination.getSize();
  }

  public String getLastmod() {
    return pagination.getLastmod();
  }

  public String getFilter() {
    return pagination.getFilter();
  }

  public boolean isFirstPage() {
    return count > 0 && getIndex() == 1;
  }

  public boolean isLastPage() {
    return count > 0 && getIndex() == count;
  }

}
