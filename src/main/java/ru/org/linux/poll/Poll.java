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

package ru.org.linux.poll;

import java.io.Serializable;

public class Poll implements Serializable {
  public static final int MAX_POLL_SIZE = 15;
  public static final int ORDER_ID = 1;
  public static final int ORDER_VOTES = 2;

  private final int id;
  private final int topic;

  private final boolean current;
  private final boolean multiSelect;
  private static final long serialVersionUID = -6541849807995680089L;

  public Poll(int id, int topic, boolean multiSelect, boolean current) {
    this.id = id;
    this.topic = topic;
    this.multiSelect = multiSelect;
    this.current = current;
  }

  public int getId() {
    return id;
  }

  public int getTopicId() {
    return topic;
  }

  public boolean isCurrent() {
    return current;
  }

  public boolean isMultiSelect() {
    return multiSelect;
  }
}
