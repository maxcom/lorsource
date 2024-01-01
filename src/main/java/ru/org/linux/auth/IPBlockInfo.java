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

package ru.org.linux.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

public class IPBlockInfo {
  private final boolean initialized;
  private final String ip;
  private final String reason;
  private final Timestamp banDate;
  private final Timestamp originalDate;
  private final int moderator;
  private final boolean allowPosting;
  private final boolean captchaRequired;

  public IPBlockInfo(String ip) {
    this.ip = ip;
    reason = null;
    banDate = null;
    originalDate = null;
    moderator = 0;
    allowPosting = false;
    captchaRequired = false;
    initialized = false;
  }

  public IPBlockInfo(ResultSet rs) throws SQLException {
    ip = rs.getString("ip");
    reason = rs.getString("reason");
    banDate = rs.getTimestamp("ban_date");
    originalDate = rs.getTimestamp("date");
    moderator = rs.getInt("mod_id");
    allowPosting = rs.getBoolean("allow_posting");
    captchaRequired = rs.getBoolean("captcha_required");
    initialized = true;
  }

  public String getIp() {
    return ip;
  }

  public boolean isBlocked() {
    return initialized && (banDate == null || banDate.after(new Date()));
  }

  public Timestamp getOriginalDate() {
    return originalDate;
  }

  public Timestamp getBanDate() {
    return banDate;
  }

  public String getReason() {
    return reason;
  }

  public int getModerator() {
    return moderator;
  }

  public boolean isInitialized() {
    return initialized;
  }

  public boolean isAllowRegistredPosting() {
    return !isBlocked() || allowPosting;
  }

  public boolean isCaptchaRequired() {
    return isBlocked() && allowPosting && captchaRequired;
  }
}
