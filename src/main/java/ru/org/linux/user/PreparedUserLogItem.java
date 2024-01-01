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

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class PreparedUserLogItem {
  private final UserLogItem item;

  private final User actionUser;
  private final ImmutableMap<String, String> options;
  private final boolean self;

  public PreparedUserLogItem(UserLogItem item, User actionUser, Map<String, String> options) {
    this.item = item;
    this.actionUser = actionUser;
    this.options = ImmutableMap.copyOf(options);
    self = item.getUser()==item.getActionUser();
  }

  public UserLogItem getItem() {
    return item;
  }

  public User getActionUser() {
    return actionUser;
  }

  public boolean isSelf() {
    return self;
  }

  public ImmutableMap<String, String> getOptions() {
    return options;
  }
}
