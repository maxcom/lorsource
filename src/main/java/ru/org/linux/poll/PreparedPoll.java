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

package ru.org.linux.poll;

import com.google.common.collect.ImmutableList;
import ru.org.linux.util.StringUtil;

import java.util.List;

/**
 * Подготовленные данные опроса для отображения
 */
public class PreparedPoll {
  private final Poll poll;
  private final int maximumValue;
  private final int totalOfVotesPerson;
  private final int totalVotes;
  private final ImmutableList<PreparedPollVariantResult> variants;

  /**
   * Конструктор
   * @param poll опрос
   * @param maximumValue вариант опроса с самым большим кол-вом голосов(?)
   * @param totalOfVotesPerson кол-во проголосовавших пользователей
   * @param variants1 варианты опроса
   */
  public PreparedPoll(Poll poll, int maximumValue, int totalOfVotesPerson, List<PollVariantResult> variants1) {
    this.poll = poll;
    this.maximumValue = maximumValue;
    this.totalOfVotesPerson = totalOfVotesPerson;
    ImmutableList.Builder<PreparedPollVariantResult> variantsBuilder = new ImmutableList.Builder<>();
    int total=0;
    for(PollVariantResult variant : variants1) {
      total += variant.getVotes();
    }
    totalVotes = total;
    for(PollVariantResult variant : variants1) {
      int variantWidth = 20*variant.getVotes()/maximumValue;
      variantsBuilder.add(new PreparedPollVariantResult(
          variant.getId(),
          variant.getLabel(),
          variant.getVotes(),
          variant.getUserVoted(),
          (int)Math.round(100 * (double)variant.getVotes() / totalVotes),
          variantWidth*16,  // пингвин 16px
          StringUtil.repeat("*", variantWidth)
          ));
    }
    variants = variantsBuilder.build();
  }

  public Poll getPoll() {
    return poll;
  }

  public int getMaximumValue() {
    return maximumValue;
  }

  public ImmutableList<PreparedPollVariantResult> getVariants() {
    return variants;
  }

  public int getTotalOfVotesPerson() {
    return totalOfVotesPerson;
  }

  public int getTotalVotes() {
    return totalVotes;
  }

  /**
   * Функция отображения результатов опроса
   * используем в RSS
   * @return html табличку результатов голосования
   */
  public String renderPoll()  {
    StringBuilder out = new StringBuilder();
    out.append("<table>");
    int total = 0;
    for (PollVariantResult var : variants) {
      //                      label      votes     imgTag
      String formatRow = "<tr><td>%s</td><td>%d</td></tr>";
      int votes = var.getVotes();
      String row = String.format(formatRow, StringUtil.escapeHtml(var.getLabel()), votes);
      out.append(row);
      total += votes;
    }
    out.append("<tr><td colspan=2>Всего голосов: ").append(total).append("</td></tr>");
    if(poll.isMultiSelect()) {
      out.append("<tr><td colspan=2>Всего проголосовавших: ").append(totalOfVotesPerson).append("</td></tr>");
    }
    out.append("</table>");
    return out.toString();
  }
}
