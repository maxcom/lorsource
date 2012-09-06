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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import ru.org.linux.spring.Configuration;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;

import java.util.ArrayList;
import java.util.Collection;

/**
 */
@Component
public class UserDetailsServiceImpl implements UserDetailsService {
  private static final Log logger = LogFactory.getLog(UserDetailsServiceImpl.class);

  @Autowired
  private UserDao userDao;

  @Autowired
  private Configuration configuration;

  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
    User user;

    if(username.contains("@")) {
      user = userDao.getByEmail(username, true);
      if(user == null) {
        throw new UsernameNotFoundException(username);
      }
    } else {
      try {
        user = userDao.getUser(username);
      } catch (UserNotFoundException e) {
        throw new UsernameNotFoundException(username);
      }
    }

    UserDetailsImpl userDetails = new UserDetailsImpl(user, retrieveUserAuthorities(user), configuration);

    logger.debug(userDetails.getUsername());
    logger.debug(userDetails.getPassword());
    logger.debug(userDetails.isAccountNonExpired());
    logger.debug(userDetails.isAccountNonLocked());
    logger.debug(userDetails.isCredentialsNonExpired());
    logger.debug(userDetails.isEnabled());

    return userDetails;
  }

  private Collection<GrantedAuthority> retrieveUserAuthorities(User user) {
    Collection<GrantedAuthority> results = new ArrayList<GrantedAuthority>();
    results.add(new SimpleGrantedAuthority("ROLE_USER"));
    if(user.isCorrector()) {
      results.add(new SimpleGrantedAuthority("ROLE_CORRECTOR"));
    }
    if(user.isModerator()) {
      results.add(new SimpleGrantedAuthority("ROLE_MODERATOR"));
    }
    if(user.isAdministrator()) {
      results.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
    return results;
  }
}
