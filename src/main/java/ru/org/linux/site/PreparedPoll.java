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

import com.google.common.collect.ImmutableList;
import ru.org.linux.util.StringUtil;

import java.util.List;

/**
 * Подготовленные данные опроса для отображения
 */
public class PreparedPoll {
  private final Poll poll;
  private final int maximumValue;
  private final int countUsers;
  private final ImmutableList<PollVariant> variants;

  /**
   * Конструктор
   * @param poll опрос
   * @param maximumValue вариант опроса с самым большим кол-вом голосов(?)
   * @param countUsers кол-во проголосовавших пользователей
   * @param variants варианты опроса
   */
  public PreparedPoll(Poll poll, int maximumValue, int countUsers, List<PollVariant> variants) {
    this.poll = poll;
    this.maximumValue = maximumValue;
    this.countUsers = countUsers;
    this.variants = ImmutableList.copyOf(variants);
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

  public int getCountUsers() {
    return countUsers;
  }

  /**
   * Функция отображения результатов опроса
   * используем так-же в poll.tag
   * @return html табличку результатов голосования
   */
  public String renderPoll()  {
    StringBuilder out = new StringBuilder();
    int max = maximumValue;
    out.append("<table>");
    int total = 0;
    for (PollVariant var : variants) {
      //                      label      votes     imgTag
      String formatRow = "<tr><td>%s</td><td>%d</td><td>%s</td></tr>";
      String formatImgTag = "<p class=\"poll\"><img class=\"pollimg\" width=\"%d\" alt=\"%s\"></p>";
      int votes = var.getVotes();
      String imgTag = String.format(
          formatImgTag,
          320*votes/max,                        // width
          StringUtil.repeat("*", 20*votes/max)  // alt
      );
      String row = String.format(formatRow, StringUtil.escapeHtml(var.getLabel()), votes, imgTag);
      out.append(row);
      total += votes;
    }
    out.append("<tr><td colspan=2>Всего голосов: ").append(total).append("</td></tr>");
    if(poll.isMultiSelect()) {
      out.append("<tr><td colspan=2>Всего проголосовавших: ").append(countUsers).append("</td></tr>");
    }
    out.append("</table>");
    return out.toString();
  }
}
