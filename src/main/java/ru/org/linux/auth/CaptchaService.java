/*
 * Copyright 1998-2015 Linux.org.ru
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

import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaException;
import net.tanesha.recaptcha.ReCaptchaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import javax.servlet.ServletRequest;

@Component
public class CaptchaService {
  private static final Logger logger = LoggerFactory.getLogger(CaptchaService.class);

  private ReCaptcha captcha;

  @Autowired
  public void setCaptcha(ReCaptcha captcha) {
    this.captcha = captcha;
  }

  public void checkCaptcha(ServletRequest request, Errors errors) {
    String captchaChallenge = request.getParameter("recaptcha_challenge_field");
    String captchaResponse = request.getParameter("recaptcha_response_field");

    if (captchaChallenge==null || captchaResponse==null) {
      errors.reject(null, "Код проверки защиты от роботов не указан");
      return;
    }

    try {
      ReCaptchaResponse response = captcha.checkAnswer(request.getRemoteAddr(), captchaChallenge, captchaResponse);

      if (!response.isValid()) {
        errors.reject(null, "Код проверки защиты от роботов не совпадает");
      }
    } catch (ReCaptchaException e) {
      logger.warn("Unable to check captcha", e);

      errors.reject(null, "Unable to check captcha: "+e.getMessage());
    }
  }
}
