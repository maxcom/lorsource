/*
 * Copyright 1998-2015 Linux.org.ru
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

package ru.org.linux.poll;

public class PollVariantResult {
  private final int id;
  private final String label;
  private final int votes;
  private final boolean userVoted;

  public PollVariantResult(int id, String label, int votes, boolean userVoted) {
    this.id = id;
    this.label = label;
    this.votes = votes;
    this.userVoted = userVoted;
  }

  public int getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public int getVotes() {
    return votes;
  }

  public boolean getUserVoted() {
    return userVoted;
  }
}
