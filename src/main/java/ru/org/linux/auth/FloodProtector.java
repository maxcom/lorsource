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

package ru.org.linux.auth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import ru.org.linux.user.User;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class FloodProtector {
  private final Cache<String, Instant> performedActions =
          CacheBuilder.newBuilder()
                  .expireAfterWrite(30, TimeUnit.MINUTES)
                  .build();

  public enum Action {
    ADD_COMMENT(Duration.ofMinutes(5), Duration.ofSeconds(30), Duration.ofSeconds(3)),
    ADD_TOPIC(Duration.ofMinutes(10), Duration.ofMinutes(10), Duration.ofSeconds(30));

    private final Duration thresholdLowScore;
    private final Duration threshold;
    private final Duration thresholdTrusted;

    Action(Duration thresholdLowScore, Duration threshold, Duration thresholdTrusted) {
      this.thresholdLowScore = thresholdLowScore;
      this.threshold = threshold;
      this.thresholdTrusted = thresholdTrusted;
    }
 }

  private boolean check(Action action, String ip, Duration threshold) {
    String key = action.toString() + ':' + ip;

    Instant date = performedActions.getIfPresent(key);

    if (date!=null) {
      if (date.plus(threshold).isAfter(Instant.now())) {
        return false;
      }
    }

    performedActions.put(key, Instant.now());

    return true;
  }

  public void checkRateLimit(Action action, String ip, User user, Errors errors) {
    Duration threshold;

    if (!user.isAnonymous() && user.getScore() < 35) {
      threshold = action.thresholdLowScore;
    } else if (user.getScore() >= 100) {
      threshold = action.thresholdTrusted;
    } else {
      threshold = action.threshold;
    }

    if (!check(action, ip, threshold)) {
      errors.reject(
              null,
              String.format("Следующее сообщение может быть записано не менее чем через %d секунд после предыдущего", threshold.toSeconds())
      );
    }
  }
}
