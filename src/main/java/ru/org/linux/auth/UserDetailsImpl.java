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

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.org.linux.user.Profile;
import ru.org.linux.user.User;

import java.util.ArrayList;
import java.util.Collection;

/**
 */
public class UserDetailsImpl implements UserDetails {
  private final User user;
  private final Collection<GrantedAuthority> authorities;
  private final Profile profile;

  public UserDetailsImpl(User user1, Collection<GrantedAuthority> authorities1, Profile profile) {
    user = user1;
    authorities = new ArrayList<>(authorities1);
    this.profile = profile;
  }

  public User getUser() {
    return user;
  }

  public Profile getProfile() {
    return profile;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return user.getPassword();
  }

  @Override
  public String getUsername() {
    return user.getNick();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return !user.isBlocked();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return !user.isBlocked();
  }
}
