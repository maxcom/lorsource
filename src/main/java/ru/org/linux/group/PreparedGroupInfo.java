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

package ru.org.linux.group;

public class PreparedGroupInfo {
  private final Group group;
  private final String longInfo;

  public PreparedGroupInfo(Group group, String longInfo) {
    this.group = group;
    this.longInfo = longInfo;
  }

  public Group getGroup() {
    return group;
  }

  public String getLongInfo() {
    return longInfo;
  }

  public String getInfo() {
    return group.getInfo();
  }

  public int getId() {
    return group.getId();
  }
}
