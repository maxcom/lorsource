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

import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jasypt.util.password.PasswordEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordEncoderImpl implements PasswordEncoder {
  private static final PasswordEncryptor encryptor = new BasicPasswordEncryptor();
  private static final Logger logger = LoggerFactory.getLogger(PasswordEncoderImpl.class);

  @Override
  public String encode(CharSequence rawPassword) {
    return encryptor.encryptPassword(rawPassword.toString());
  }

  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    if (rawPassword.length()!=0) {
      try {
        return encryptor.checkPassword(rawPassword.toString(), encodedPassword);
      } catch (EncryptionOperationNotPossibleException ex) {
        logger.warn("Can't check password", ex);

        return false;
      }
    } else {
      return false;
    }
  }
}
