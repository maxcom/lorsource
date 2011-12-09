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

package ru.org.linux.poll;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class PollVariant {
  private final int id;
  private final String label;
  private final int votes;

  public PollVariant(int id, String label, int votes) {
    this.id = id;
    this.label = label;
    this.votes = votes;
  }

  public int getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public int getVotes() {
    return votes;
  }

  public static SortedMap<Integer, String> toMap(List<PollVariant> list) {
    SortedMap<Integer, String> map = new TreeMap<Integer, String>();

    for (PollVariant v : list) {
      if (v.getId()!=0) {
        map.put(v.getId(), v.getLabel());
      }
    }

    return map;
  }
}
