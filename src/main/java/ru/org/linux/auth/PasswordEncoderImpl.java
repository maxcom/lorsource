package ru.org.linux.auth;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jasypt.util.password.PasswordEncryptor;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 */
@Component
public class PasswordEncoderImpl implements PasswordEncoder {
  private static final Log logger = LogFactory.getLog(PasswordEncoderImpl.class);

  static PasswordEncryptor encryptor = new BasicPasswordEncryptor();

  public String encodePassword(String rawPass, Object salt) {
    return encryptor.encryptPassword(rawPass);
  }

  public boolean isPasswordValid(String encPass, String rawPass, Object salt) {
    logger.info(rawPass);
    return encryptor.checkPassword(rawPass, encPass);
  }

}
