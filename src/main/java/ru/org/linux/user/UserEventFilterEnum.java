/*
 * Copyright 1998-2013 Linux.org.ru
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

public enum UserEventFilterEnum {
  ALL("all", "все уведомления", ""),
  ANSWERS("answers", "ответы", "REPLY"),
  FAVORITES("favorites", "отслеживаемое", "WATCH"),
  DELETED("deleted", "удаленное", "DEL"),
  REFERENCE("reference", "упоминания", "REF"),
  TAG("tag", "теги", "TAG");

  private final String value;
  private final String label;
  private final String type;

  UserEventFilterEnum(String value, String label, String type) {
    this.value = value;
    this.label = label;
    this.type = type;
  }

  public String getValue() {
    return value;
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
}
