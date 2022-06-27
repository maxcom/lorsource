/*
 * Copyright 1998-2022 Linux.org.ru
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
import ru.org.linux.markup.MarkupPermissions;
import ru.org.linux.markup.MarkupType;
import ru.org.linux.user.Profile;
import ru.org.linux.user.User;

import javax.annotation.Nonnull;

public final class Template {
  private final Profile userProfile;

  public Template() {
    userProfile = AuthUtil.getProfile();
  }

  @Deprecated
  public String getStyle() {
    return getTheme().getId();
  }

  public Theme getTheme() {
    User user = AuthUtil.getCurrentUser();

    if (user == null) {
      return DefaultProfile.getDefaultTheme();
    } else {
      return DefaultProfile.getTheme(user.getStyle());
    }
  }

  public String getFormatMode() {
    String mode = userProfile.getFormatMode();

    if (MarkupPermissions.allowedFormatsJava(AuthUtil.getCurrentUser()).stream().map(MarkupType::formId).anyMatch(s -> s.equals(mode))) {
      return mode;
    } else {
      return MarkupType.Lorcode$.MODULE$.formId();
    }
  }

  @Nonnull
  public Profile getProf() {
    return userProfile;
  }

  public boolean isSessionAuthorized() {
    return AuthUtil.isSessionAuthorized();
  }

  public boolean isModeratorSession() {
    return AuthUtil.isModeratorSession();
  }

  public boolean isCorrectorSession() {
    return AuthUtil.isCorrectorSession();
  }

  public static Template getTemplate() {
    return new Template();
  }
}
