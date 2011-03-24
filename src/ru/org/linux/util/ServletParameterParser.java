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

package ru.org.linux.util;

import java.util.regex.Pattern;

import javax.servlet.ServletRequest;

public class ServletParameterParser {
  private static final Pattern ipRE = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$");

  private final ServletRequest rq;

  public ServletParameterParser(ServletRequest request) {
    rq = request;
  }

  public String getString(String name) throws ServletParameterException {
    if (name == null) {
      throw new ServletParameterBadNameException(name);
    }

    String value = rq.getParameter(name);

    if (value == null) {
      throw new ServletParameterMissingException(name);
    }

    return value;
  }

  public int getInt(String name) throws ServletParameterException {
    if (name == null) {
      throw new ServletParameterBadNameException(name);
    }

    String svalue = rq.getParameter(name);

    if (svalue == null) {
      throw new ServletParameterMissingException(name);
    }

    int value;

    try {
      value = Integer.parseInt(svalue);
    } catch (NumberFormatException e) {
      throw new ServletParameterBadValueException(name, e);
    }

    return value;
  }

  public boolean getBoolean(String name) throws ServletParameterException {
    int value = getInt(name);

    switch (value) {
      case 0:
        return false;
      case 1:
        return true;
      default:
        throw new ServletParameterBadValueException(name, "not boolean");
    }
  }

  public String getIP(String name) throws ServletParameterException {
    String ip = getString(name);

    if (!ipRE.matcher(ip).matches()) {
      throw new ServletParameterBadValueException(name, "not ip");
    }

    return ip;
  }
}
