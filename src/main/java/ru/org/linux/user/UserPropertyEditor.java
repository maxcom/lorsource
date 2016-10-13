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

package ru.org.linux.user;

import java.beans.PropertyEditorSupport;

public class UserPropertyEditor extends PropertyEditorSupport {
  private final UserService userService;

  public UserPropertyEditor(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void setAsText(String s) throws IllegalArgumentException {
    if (s.isEmpty()) {
      setValue(null);
      return;
    }

    try {
      setValue(userService.getUser(s));
    } catch (UserNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public String getAsText() {
    if (getValue() == null) {
      return "";
    }

    return ((User) getValue()).getNick();
  }
}
