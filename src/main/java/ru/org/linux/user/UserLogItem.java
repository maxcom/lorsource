/*
 * Copyright 1998-2024 Linux.org.ru
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

package ru.org.linux.user;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.stream.Collectors;

public class UserLogItem {
  private final int id;
  private final int user;
  private final int actionUser;
  private final DateTime actionDate;
  private final UserLogAction action;
  private final ImmutableMap<String, String> options;

  public UserLogItem(
          int id,
          int user,
          int actionUser,
          DateTime actionDate,
          UserLogAction action,
          Map<String, String> options
  ) {
    this.id = id;
    this.user = user;
    this.actionUser = actionUser;
    this.actionDate = actionDate;
    this.action = action;
    this.options = options
            .entrySet()
            .stream()
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, v -> Strings.nullToEmpty(v.getValue())));
  }

  public int getId() {
    return id;
  }

  public int getUser() {
    return user;
  }

  public int getActionUser() {
    return actionUser;
  }

  public DateTime getActionDate() {
    return actionDate;
  }

  public UserLogAction getAction() {
    return action;
  }

  public ImmutableMap<String, String> getOptions() {
    return options;
  }
}
