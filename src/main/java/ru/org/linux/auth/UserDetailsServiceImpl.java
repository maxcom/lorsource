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
package ru.org.linux.auth;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import ru.org.linux.user.*;

import java.util.ArrayList;
import java.util.Collection;

@Component
public class UserDetailsServiceImpl implements UserDetailsService {
  private final UserDao userDao;
  private final UserService userService;
  private final ProfileDao profileDao;

  public UserDetailsServiceImpl(UserDao userDao, UserService userService, ProfileDao profileDao) {
    this.userDao = userDao;
    this.userService = userService;
    this.profileDao = profileDao;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
    User user;

    if (username.contains("@")) {
      user = userDao.getByEmail(username, true);
      if (user == null) {
        throw new UsernameNotFoundException(username);
      }
    } else {
      try {
        user = userService.getUser(username);
      } catch (UserNotFoundException e) {
        throw new UsernameNotFoundException(username);
      }
    }

    return new UserDetailsImpl(user, retrieveUserAuthorities(user), profileDao.readProfile(user.getId()));
  }

  private static Collection<GrantedAuthority> retrieveUserAuthorities(User user) {
    Collection<GrantedAuthority> results = new ArrayList<>();
    if(user.isActivated()) {
      results.add(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));
      if (user.getScore() >= 50) {
        results.add(new SimpleGrantedAuthority("ROLE_USER"));
      }
      if (user.canCorrect()) {
        results.add(new SimpleGrantedAuthority("ROLE_CORRECTOR"));
      }
      if (user.isModerator()) {
        results.add(new SimpleGrantedAuthority("ROLE_MODERATOR"));
      }
      if (user.isAdministrator()) {
        results.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
      }
    }
    return results;
  }
}
