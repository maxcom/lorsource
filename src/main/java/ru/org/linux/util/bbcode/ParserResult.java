/*
 * Copyright 1998-2014 Linux.org.ru
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

package ru.org.linux.util.bbcode;

import ru.org.linux.user.User;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 8/24/11
 * Time: 7:12 PM
 */
public class ParserResult {
  private final String html;
  private final Set<User> replier;

  public ParserResult(String html, Set<User> replier) {
    this.html = html;
    this.replier = replier;
  }

  public String getHtml() {
    return html;
  }

  public Set<User> getReplier() {
    return replier;
  }
}
