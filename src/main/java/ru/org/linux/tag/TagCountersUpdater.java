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

package ru.org.linux.tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TagCountersUpdater {
  private static final int HOUR = 60*60*1000;
  private static final int FIVE_MINS = 5 * 60 * 1000;

  @Autowired
  private TagService tagService;
  
  @Scheduled(fixedDelay = HOUR, initialDelay = FIVE_MINS)
  public void recalcTagsCounters() {
    tagService.reCalculateAllCounters();
  }
}
