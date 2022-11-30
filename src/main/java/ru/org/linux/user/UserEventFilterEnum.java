/*
 * Copyright 1998-2022 Linux.org.ru
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

import com.google.common.collect.ImmutableSet;

public enum UserEventFilterEnum {
  ALL("все", ""),
  ANSWERS("ответы", "REPLY"),
  FAVORITES("отслеживаемое", "WATCH"),
  DELETED("удаленное", "DEL"),
  REFERENCE("упоминания", "REF"),
  TAG("теги", "TAG"),
  REACTION("реакции", "REACTION");

  private final String label;
  private final String type;

  private static final ImmutableSet<String> VALUES;

  static {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();

    for (UserEventFilterEnum eventFilter : UserEventFilterEnum.values()) {
      builder.add(eventFilter.getName());
    }

    VALUES = builder.build();
  }

  UserEventFilterEnum(String label, String type) {
    this.label = label;
    this.type = type;
  }

  public String getName() {
    return toString().toLowerCase();
  }

  public String getLabel() {
    return label;
  }

  public String getType() {
    return type;
  }

  public static UserEventFilterEnum valueOfByType(String type) {
    for (UserEventFilterEnum eventFilterEnum : UserEventFilterEnum.values()) {
      if (eventFilterEnum.getType().equals(type)) {
        return eventFilterEnum;
      }
    }
    return null;
  }

  public static UserEventFilterEnum fromNameOrDefault(String filterAction) {
    if (VALUES.contains(filterAction)) {
      return UserEventFilterEnum.valueOf(filterAction.toUpperCase());
    } else {
      return UserEventFilterEnum.ALL;
    }
  }
}
