/*
 * Copyright 1998-2016 Linux.org.ru
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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.concurrent.TimeUnit;

@Component
public class FloodProtector {
  private final Cache<String,DateTime> hash =
          CacheBuilder.newBuilder()
                  .expireAfterWrite(30, TimeUnit.MINUTES)
                  .maximumSize(100000)
                  .build();

  public enum Action {
    ADD_COMMENT(30000, 3000) , ADD_TOPIC(600000, 30000);

    private final int threshold;
    private final int thresholdTrusted;

    Action(int threshold, int thresholdTrusted) {
      this.threshold = threshold;
      this.thresholdTrusted = thresholdTrusted;
    }

    public int getThreshold() {
      return threshold;
    }

    public int getThresholdTrusted() {
      return thresholdTrusted;
    }
  }

  private boolean check(Action action, String ip, int threshold) {
    String key = action.toString() + ':' + ip;

    DateTime date = hash.getIfPresent(key);

    if (date!=null) {
      if (date.plusMillis(threshold).isAfterNow()) {
        return false;
      }
    }

    hash.put(key, new DateTime());

    return true;
  }

  public void checkDuplication(Action action, String ip, boolean trusted, Errors errors) {
    int threshold = trusted?action.getThresholdTrusted():action.getThreshold();

    if (!check(action, ip, threshold)) {
      errors.reject(
              null,
              String.format("Следующее сообщение может быть записано не менее чем через %d секунд после предыдущего", threshold/1000)
      );
    }
  }
}
