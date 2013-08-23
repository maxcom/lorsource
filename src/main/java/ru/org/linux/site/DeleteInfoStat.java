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

package ru.org.linux.site;

public class DeleteInfoStat {
  private final String reason;
  private final int count;
  private final int sum;

  public DeleteInfoStat(String reason, int count, int sum) {
    this.reason = reason;
    this.count = count;
    this.sum = sum;
  }

  public String getReason() {
    return reason;
  }

  public int getCount() {
    return count;
  }

  public int getSum() {
    return sum;
  }

  public double getAvg() {
    if (count==0) {
      return 0;
    } else {
      return sum/(double) count;
    }
  }
}
