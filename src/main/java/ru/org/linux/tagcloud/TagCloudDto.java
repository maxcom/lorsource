/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.tagcloud;

import java.io.Serializable;

public class TagCloudDto implements Serializable, Comparable<TagCloudDto> {
  private Integer weight;
  private String value;
  private double counter;
  private static final long serialVersionUID = -1928783200997267710L;

  public Integer getWeight() {
    return weight;
  }

  public void setWeight(Integer weight) {
    this.weight = weight;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public double getCounter() {
    return counter;
  }

  public void setCounter(double counter) {
    this.counter = counter;
  }


  @Override
  public int compareTo(TagCloudDto o) {
    return value.compareTo(o.value);
  }
}
