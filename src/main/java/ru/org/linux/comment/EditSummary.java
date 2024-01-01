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

package ru.org.linux.comment;

import java.sql.Timestamp;

public class EditSummary {
  private final String editNick;
  private final Timestamp editDate;
  private final int editCount;

  public EditSummary(String editNick, Timestamp editDate, int editCount) {
    this.editNick = editNick;
    this.editDate = editDate;
    this.editCount = editCount;
  }

  public String getEditNick() {
    return editNick;
  }

  public Timestamp getEditDate() {
    return editDate;
  }

  public int getEditCount() {
    return editCount;
  }
}
