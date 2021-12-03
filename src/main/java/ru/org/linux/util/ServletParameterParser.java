/*
 * Copyright 1998-2021 Linux.org.ru
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

package ru.org.linux.util;

import ru.org.linux.site.BadInputException;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

public class ServletParameterParser {
  private static final Pattern ipRE = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$");

  private ServletParameterParser() {
  }

  @Nullable
  public static String cleanupIp(@Nullable String ip) {
    if (ip!=null) {
      if (!ipRE.matcher(ip).matches()) {
        throw new BadInputException("not ip");
      }
    }

    return ip;
  }
}
