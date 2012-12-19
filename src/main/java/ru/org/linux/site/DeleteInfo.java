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

package ru.org.linux.site;

import java.sql.Timestamp;

public class DeleteInfo {
  private final int userid;
  private final String reason;
  private final Timestamp delDate;
  private final Integer bonus;

  public DeleteInfo(int userid, String reason, Timestamp delDate, Integer bonus) {
    this.reason = reason;
    this.userid = userid;
    this.delDate = delDate;
    this.bonus = bonus;
  }

  public int getUserid() {
    return userid;
  }

  public String getReason() {
    if (bonus!=null) {
      return reason + " ("+bonus+ ')';
    } else {
      return reason;
    }
  }

  public Timestamp getDelDate() {
    return delDate;
  }

  public int getBonus() {
    return bonus!=null?bonus:0;
  }
}
