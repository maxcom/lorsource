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

package ru.org.linux.auth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.concurrent.TimeUnit;

@Component
public class FloodProtector {
  private static final int THRESHOLD = 30000;
  private static final int THRESHOLD_TRUSTED = 3000;
  
  private final Cache<String,DateTime> hash =
          CacheBuilder.newBuilder()
                  .expireAfterWrite(30, TimeUnit.MINUTES)
                  .maximumSize(100000)
                  .build();

  public static final String MESSAGE = "Следующее сообщение может быть записано не менее чем через 30 секунд после предыдущего";

  private boolean check(String ip, boolean trusted) {
    DateTime date = hash.getIfPresent(ip);

    if (date!=null) {
      if (date.plusMillis((trusted?THRESHOLD_TRUSTED:THRESHOLD)).isAfterNow()) {
        return false;
      }
    }

    hash.put(ip, new DateTime());

    return true;
  }

  public void checkDuplication(String ip,boolean trusted, Errors errors) {
    if (!check(ip,trusted)) {
      errors.reject(null, MESSAGE);
    }
  }
}