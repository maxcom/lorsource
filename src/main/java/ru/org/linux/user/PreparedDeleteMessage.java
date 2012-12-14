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
package ru.org.linux.user;

import java.sql.Timestamp;

/**
 */
public class PreparedDeleteMessage {
  int id;
  private final String sectionTitle;
  private final String groupTitle;
  private final String title;
  private final String reason;
  private final int bonus;
  private final User moderator;
  private final Timestamp date;

  public PreparedDeleteMessage(int id, String sectionTitle, String groupTitle, String title, String reason, int bonus, User moderator, Timestamp date) {
    this.id = id;
    this.sectionTitle = sectionTitle;
    this.groupTitle = groupTitle;
    this.title = title;
    this.reason = reason;
    this.bonus = bonus;
    this.moderator = moderator;
    this.date = date;
  }

  public int getId() {
    return id;
  }

  public String getSectionTitle() {
    return sectionTitle;
  }

  public String getTitle() {
    return title;
  }

  public String getGroupTitle() {
    return groupTitle;
  }

  public String getReason() {
    return reason;
  }

  public int getBonus() {
    return bonus;
  }

  public User getModerator() {
    return moderator;
  }

  public Timestamp getDate() {
    return date;
  }
}
