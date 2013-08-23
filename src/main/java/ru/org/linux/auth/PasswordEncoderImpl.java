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

package ru.org.linux.auth;

import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jasypt.util.password.PasswordEncryptor;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 */
@Component
public class PasswordEncoderImpl implements PasswordEncoder {
  static PasswordEncryptor encryptor = new BasicPasswordEncryptor();

  public String encodePassword(String rawPass, Object salt) {
    return encryptor.encryptPassword(rawPass);
  }

  public boolean isPasswordValid(String encPass, String rawPass, Object salt) {
    return encryptor.checkPassword(rawPass, encPass);
  }

}
