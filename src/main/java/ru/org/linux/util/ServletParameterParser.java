/*
 * Copyright 1998-2017 Linux.org.ru
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

import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;

import javax.servlet.ServletRequest;
import java.util.regex.Pattern;

public class ServletParameterParser {
  private static final Pattern ipRE = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$");

  public static String getIP(ServletRequest rq, String name) throws ServletParameterException, ServletRequestBindingException {
    String ip = ServletRequestUtils.getRequiredStringParameter(rq, name);

    if (!ipRE.matcher(ip).matches()) {
      throw new ServletParameterBadValueException(name, "not ip");
    }

    return ip;
  }
}
