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
package ru.org.linux.tracker;

import com.google.common.collect.ImmutableSet;

import java.util.Optional;

public enum TrackerFilterEnum {
  ALL("all", "все", true, false),
  MAIN("main", "основные", true, false),
  NOTALKS("notalks", "без talks", false, false),
  TECH("tech", "тех. форум", false, false),
  SCORE50("score50", "score < 50", false, true),
  SCORE100("score100", "score < 100", false, true);

  private final String value;
  private final String label;
  private final boolean canBeDefault;
  private final boolean moderatorOnly;

  TrackerFilterEnum(String value, String label, boolean canBeDefault, boolean moderatorOnly) {
    this.value = value;
    this.label = label;
    this.canBeDefault = canBeDefault;
    this.moderatorOnly = moderatorOnly;
  }

  public String getValue() {
    return value;
  }

  public String getLabel() {
    return label;
  }

  public boolean isCanBeDefault() {
    return canBeDefault;
  }

  public boolean isModeratorOnly() {
    return moderatorOnly;
  }

  private static final ImmutableSet<String> valuesSet;

  static {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (TrackerFilterEnum eventFilter : TrackerFilterEnum.values()) {
      builder.add(eventFilter.getValue());
    }
    valuesSet = builder.build();
  }

  public static Optional<TrackerFilterEnum> getByValue(String filterAction, boolean isModeratorSession) {
    if (valuesSet.contains(filterAction)) {
      return Optional.of(TrackerFilterEnum.valueOf(filterAction.toUpperCase()))
                .filter(v -> isModeratorSession || !v.moderatorOnly);
    } else {
      return Optional.empty();
    }
  }
}
