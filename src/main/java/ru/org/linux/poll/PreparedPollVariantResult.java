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

/**
 */
public class PreparedPollVariantResult extends PollVariantResult {
  final int percentage;
  final int width;
  final int penguinPercent;
  final String alt;

  /**
   * Подготовленные варианты опроса
   * @param id                - id
   * @param label             -
   * @param votes             - кол-во голосов
   * @param userVoted         - голосовал ли текущий пользователь
   * @param percentage        - процент
   * @param width             -
   * @param penguinPercent    - процент кратный ширине пингвина
   * @param alt               -
   */
  public PreparedPollVariantResult(int id, String label, int votes, boolean userVoted,
                                   int percentage, int width, int penguinPercent, String alt) {
    super(id, label, votes, userVoted);
    this.percentage = percentage;
    this.width = width;
    this.penguinPercent = penguinPercent;
    this.alt = alt;
  }

  public int getPercentage() {
    return percentage;
  }

  public int getWidth() {
    return width;
  }

  public int getPenguinPercent() {
    return penguinPercent;
  }

  public String getAlt() {
    return alt;
  }
}
