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
package ru.org.linux.tracker;

public enum TrackerFilterEnum {
  ALL("all", "все сообщения", true),
  NOTALKS("notalks", "без talks", false),
  TECH("tech", "тех. разделы форума", false),
  MINE("mine", "мои темы", false),
  ZERO("zero", "без ответов", false);

  private final String value;
  private final String label;
  private final boolean def;

  TrackerFilterEnum(String value, String label, boolean def) {
    this.value = value;
    this.label = label;
    this.def = def;
  }

  public String getValue() {
    return value;
  }

  public String getLabel() {
    return label;
  }

  public boolean isDefaultValue() {
    return def;
  }
}