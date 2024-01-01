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

package ru.org.linux.auth;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import ru.org.linux.user.UserDao;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class GenerationBasedTokenRememberMeServices extends TokenBasedRememberMeServices {
  private final UserDao userDao;

  public GenerationBasedTokenRememberMeServices(String key, UserDetailsService userDetailsService, UserDao userDao) {
    super(key, userDetailsService);

    this.userDao = userDao;
  }

  @Override
  protected String makeTokenSignature(long tokenExpiryTime, String username, String password) {
    String data = username + ":" + tokenExpiryTime + ":" + password + ":" + getKey();

    int tokenGeneration = userDao.getTokenGeneration(username);
    if (tokenGeneration > 0) { // zero means user does not use close all sessions ever
       data += ":" + String.format("%d", tokenGeneration);
    }

    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("MD5");
    }
    catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("No MD5 algorithm available!");
    }

    return new String(Hex.encode(digest.digest(data.getBytes())));
  }
}
