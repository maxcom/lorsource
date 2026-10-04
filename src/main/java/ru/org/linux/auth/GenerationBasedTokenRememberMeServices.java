/*
 * Copyright 1998-2026 Linux.org.ru
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
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
  protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest request,
                                               HttpServletResponse response) {
    if (cookieTokens.length != 4 || !RememberMeTokenAlgorithm.SHA256.name().equals(cookieTokens[2])) {
      throw new InvalidCookieException("Cookie token did not contain a SHA-256 signature (token count: "
          + cookieTokens.length + ")");
    }

    return super.processAutoLoginCookie(cookieTokens, request, response);
  }

  @Override
  protected String makeTokenSignature(long tokenExpiryTime, String username, String password,
                                      RememberMeTokenAlgorithm algorithm) {
    String data = username + ":" + tokenExpiryTime + ":" + password + ":" + getKey();

    int tokenGeneration = userDao.getTokenGeneration(username);
    if (tokenGeneration > 0) { // zero means user does not use close all sessions ever
      data += ":" + String.format("%d", tokenGeneration);
    }

    try {
      MessageDigest digest = MessageDigest.getInstance(algorithm.getDigestAlgorithm());
      return new String(Hex.encode(digest.digest(data.getBytes())));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("No " + algorithm.name() + " algorithm available!");
    }
  }
}
