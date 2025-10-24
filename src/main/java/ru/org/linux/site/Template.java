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

package ru.org.linux.site;

import ru.org.linux.auth.AuthUtil;
import ru.org.linux.user.Profile;
import ru.org.linux.user.User;

/*
  Current session helpers for JSP. Do not use in Scala/Java code.
 */
public final class Template {
  private final Profile userProfile;

  public Template() {
    userProfile = AuthUtil.getProfile();
  }

  /* used in jsp */
  public String getStyle() {
    return getTheme().getId();
  }

  /* used in jsp */
  public Theme getTheme() {
    User user = AuthUtil.getCurrentUser();

    if (user == null) {
      return DefaultProfile.getDefaultTheme();
    } else {
      return DefaultProfile.getTheme(user.getStyle());
    }
  }

  /* used in jsp */
  public String getFormatMode() {
    return userProfile.formatMode().formId();
  }

  /* used in jsp */
  public Profile getProf() {
    return userProfile;
  }

  /* used in jsp */
  public boolean isSessionAuthorized() {
    return AuthUtil.isSessionAuthorized();
  }

  /* used in jsp */
  public boolean isModeratorSession() {
    return AuthUtil.isModeratorSession();
  }

  /* used in jsp */
  public boolean isCorrectorSession() {
    return AuthUtil.isCorrectorSession();
  }

  /* used in jsp */
  public static Template getTemplate() {
    return new Template();
  }
}
