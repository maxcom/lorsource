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
package ru.org.linux.util;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 */
public class Pagination<T> implements Serializable {
  private int total;
  private int index;
  private int size;
  private List<T> items;
  private int start;
  private int end;
  private int count;

  /**
   * Сортировка списка.
   */
  private final List<Sort> sortList = new LinkedList<Sort>();

  public Pagination() {
  }

  public Pagination(int index, int size) {
    this.index = index <= 0 ? 1 : index;
    this.size = size;
    start = size * (index - 1) + 1;
    end = start + size - 1;
  }

  public List<Sort> getSortList() {
    return sortList;
  }

  public void addSort(Sort sort) {
    sortList.add(sort);
  }

  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;
    if(total < size) {
      count = 1;
    } else {
      if (total % size == 0) {
        count = total / size;
      } else {
        count = total / size + 1;
      }
    }
    if(index > count) {
      index = count;
      start = size * (index - 1) + 1;
      end = start + size - 1;
    }
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public List<T> getItems() {
    return items;
  }

  public void setItems(List<T> items) {
    this.items = items;
  }

  public int getStart() {
    return start;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public int getEnd() {
    if(end > total) {
      return total;
    }
    return end;
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

  public static class Sort {
    String field;
    Direction direction;

    public static enum Direction {
      ASC, DESC;
    }

    public String getField() {
      return field;
    }

    public void setField(String field) {
      this.field = field;
    }

    public Direction getDirection() {
      return direction;
    }

    public void setDirection(Direction direction) {
      this.direction = direction;
    }
  }

}
