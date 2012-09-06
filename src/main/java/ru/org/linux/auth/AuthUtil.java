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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.org.linux.user.Profile;
import ru.org.linux.user.ProfileProperties;
import ru.org.linux.user.User;

/**
 */
public class AuthUtil {

  public static boolean isSessionAuthorized() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.isAuthenticated();
  }

  public static boolean isModeratorSession() {
    return !isSessionAuthorized() && hasAuthority("ROLE_MODERATOR");
  }

  public static boolean isCorrectorSession() {
    return !isSessionAuthorized() && hasAuthority("ROLE_CORRECTOR");
  }

  public static boolean hasAuthority(String authName) {
    for (GrantedAuthority auth : SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
      if (auth.getAuthority().equals(authName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get current authorized users nick
   *
   * @return nick or null if not authorized
   */
  public static String getNick() {
    if (!isSessionAuthorized()) {
      return null;
    }
    return getCurrentUser().getNick();
  }

  public static User getCurrentUser() {
    if (!isSessionAuthorized()) {
      return null;
    } else {
      return ((UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
    }
  }

  public static Profile getCurrentProfile() {
    if (!isSessionAuthorized()) {
      return null;
    } else {
      return ((UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getProfile();
    }
  }

  public static boolean isUsingDefaultProfile() {
    return getCurrentProfile().isDefault();
  }

  public static ProfileProperties getProf() {
    return getCurrentProfile().getProperties();
  }

  public static String getFormatMode() {
    return getProf().getFormatMode();
  }

  public static String getStyle() {
    if(isSessionAuthorized()) {
      return getCurrentUser().getStyle();
    } else {
      return "tango";
    }
  }


}
