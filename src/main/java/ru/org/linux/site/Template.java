/*
 * Copyright 1998-2013 Linux.org.ru
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

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.spring.Configuration;
import ru.org.linux.user.Profile;
import ru.org.linux.user.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletRequest;

public final class Template {
  @Nonnull
  private final Profile userProfile;

  private final Configuration configuration;

  public Template(WebApplicationContext ctx) {
    configuration = (Configuration)ctx.getBean("configuration");
    userProfile = AuthUtil.getProfile();
  }

  public Template(ServletRequest request) {
    this(WebApplicationContextUtils.getWebApplicationContext(request.getServletContext()));
  }

  public String getStyle() {
    User user = getCurrentUser();
    if(user == null) {
      return "tango"; // TODO move to properties?
    } else {
      return user.getStyle();
    }
  }

  public String getFormatMode() {
    return userProfile.getFormatMode();
  }

  @Nonnull
  public Profile getProf() {
    return userProfile;
  }

  public String getMainUrl() {
    return configuration.getMainUrl();
  }

  public String getMainUrlNoSlash() {
    return configuration.getMainUrlWithoutSlash();
  }

  public String getSecureMainUrl() {
    return configuration.getSecureUrl();
  }

  public Configuration getConfig() {
    return configuration;
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

  /**
   * Get current authorized users nick
   * @return nick or null if not authorized
   */
  public String getNick() {
    User currentUser = getCurrentUser();

    if (currentUser==null) {
      return null;
    } else {
      return currentUser.getNick();
    }
  }

  @Nonnull
  public static Template getTemplate(ServletRequest request) {
    return new Template(request);
  }

  @Nullable
  public User getCurrentUser()  {
    return AuthUtil.getCurrentUser();
  }
}
