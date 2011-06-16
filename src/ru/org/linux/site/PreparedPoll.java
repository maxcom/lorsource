/*
 * Copyright 1998-2010 Linux.org.ru
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

import java.sql.Connection;
import java.sql.SQLException;

import ru.org.linux.util.HTMLFormatter;

import com.google.common.collect.ImmutableList;

public class PreparedPoll {
  private final Poll poll;
  private final int maximumValue;
  private final ImmutableList<PollVariant> variants;

  public PreparedPoll(Connection db, Poll poll) throws SQLException {
    this.poll = poll;
    maximumValue = poll.getMaxVote(db);
    variants = poll.getPollVariants(db, Poll.ORDER_VOTES);
  }

  public Poll getPoll() {
    return poll;
  }

  public int getMaximumValue() {
    return maximumValue;
  }

  public ImmutableList<PollVariant> getVariants() {
    return variants;
  }

  /* TODO: move to JSP */
  public String renderPoll(String fullUrl)  {
    StringBuilder out = new StringBuilder();
    int max = maximumValue;
    out.append("<table>");
    int total = 0;
    for (PollVariant var : variants) {
      out.append("<tr><td>");
      int votes = var.getVotes();
      out.append(HTMLFormatter.htmlSpecialChars(var.getLabel()));
      out.append("</td><td>").append(votes).append("</td><td>");
      total += votes;
      for (int i = 0; i < 20 * votes / max; i++) {
        out.append("<img src=\"").append(fullUrl).append("white/img/votes.png\" alt=\"*\">");
      }
      out.append("</td></tr>");
    }
    out.append("<tr><td colspan=2>Всего голосов: ").append(total).append("</td></tr>");
    out.append("</table>");
    return out.toString();
  }
}
