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
import ru.org.linux.site.DefaultProfile;
import ru.org.linux.spring.Configuration;
import ru.org.linux.storage.FileStorage;
import ru.org.linux.storage.Storage;
import ru.org.linux.storage.StorageException;
import ru.org.linux.user.*;
import ru.org.linux.util.ProfileHashtable;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.*;

/**
 */
@Component
public class UserDetailsServiceImpl implements UserDetailsService {

  private static final Log logger = LogFactory.getLog(UserDetailsServiceImpl.class);

  @Autowired
  private UserDao userDao;

  @Autowired
  private Configuration configuration;

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
        user = userDao.getUser(username);
        userDao.updateLastlogin(user, false);
      } catch (UserNotFoundException e) {
        throw new UsernameNotFoundException(username);
      }
    }
    return new UserDetailsImpl(user, retrieveUserAuthorities(user), readProfile(user.getNick()), configuration);
  }

  private Profile readProfile(String username) {
    Storage storage = new FileStorage(configuration.getPathPrefix() + "linux-storage/");
    InputStream df = null;
    Map<String, Object> userProfile = null;
    try {
      df = storage.getReadStream("profile", username);
      ObjectInputStream dof = null;
      try {
        dof = new ObjectInputStream(df);
        userProfile = (Map<String, Object>) dof.readObject();
        dof.close();
        df.close();
      } catch (IOException e) {
        logger.info("Bad profile for user " + username);
      } finally {
        if (dof != null) {
          try {
            dof.close();
          } catch (IOException e) {
            logger.info("Bad profile for user " + username);
          }
        }
      }
    } catch (StorageException e) {
      logger.info("Bad profile for user " + username);
    } catch (ClassNotFoundException e) {
      logger.info("Bad profile for user " + username);
    } finally {
      if (df != null) {
        try {
          df.close();
        } catch (IOException e) {
          logger.info("Bad profile for user " + username);
        }
      }
    }
    ProfileProperties properties;
    if (userProfile != null) {
      properties = new ProfileProperties(new ProfileHashtable(DefaultProfile.getDefaultProfile(), userProfile));
    } else {
      properties = new ProfileProperties(new ProfileHashtable(DefaultProfile.getDefaultProfile(), new HashMap<String, Object>()));
    }
    return new Profile(properties, false);
  }

  private static Collection<GrantedAuthority> retrieveUserAuthorities(User user) {
    logger.debug("retrive auth for:" + user.getNick()) ;
    Collection<GrantedAuthority> results = new ArrayList<>();
    if(user.isActivated()) {
      results.add(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));
      if (user.getScore() >= 50) {
        results.add(new SimpleGrantedAuthority("ROLE_USER"));
      }
      if (user.isCorrector()) {
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
