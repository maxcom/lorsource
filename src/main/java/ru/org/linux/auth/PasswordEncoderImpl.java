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
