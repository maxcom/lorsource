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

import java.util.LinkedList;
import java.util.List;

/**
 * Хранение параметров пагинации, полученных из GET-запроса.
 */
public class Pagination {
  /**
   * Номер страницы.
   */
  private int index;
  /**
   * Количество элементов на странице.
   */
  private int size;

  /**
   * Сортировка списка.
   */
  private final List<Sort> sortList = new LinkedList<Sort>();

  public Pagination() {
  }

  public Pagination(int index, int size) {
    this.index = index < 1 ? 1 : index;
    this.size = size;
  }

  public List<Sort> getSortList() {
    return sortList;
  }

  public void addSort(Sort sort) {
    sortList.add(sort);
  }

  public void addSort(List<Sort> sortList) {
    this.sortList.addAll(sortList);
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


  /**
   * Класс для хранения параметров сортировки.
   */
  public static class Sort {
    /**
     * Поле, по которому необходимо сортировать.
     */
    private final String field;
    /**
     * Направление сортировки.
     */
    private final Direction direction;

    /**
     * перечисление для хранения возможных значений направления сортировки.
     */
    public static enum Direction {
      ASC, DESC
    }

    public Sort(String field, Direction direction) {
      this.field = field;
      this.direction = direction;
    }

    public String getField() {
      return field;
    }

    public Direction getDirection() {
      return direction;
    }
  }

}
