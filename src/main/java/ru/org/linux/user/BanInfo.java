/*
 * Copyright 1998-2014 Linux.org.ru
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

public class BanInfo {
  private final Timestamp date;
  private final String reason;
  private final User moderator;

  public BanInfo(Timestamp date, String reason, User moderator) {
    this.date = date;
    this.reason = reason;
    this.moderator = moderator;
  }

  public Timestamp getDate() {
    return date;
  }

  public String getReason() {
    return reason;
  }

  public User getModerator() {
    return moderator;
  }
}
