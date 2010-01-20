/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.site;

import javax.servlet.ServletRequest;
import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaResponse;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.support.RequestContextUtils;

public class CaptchaUtils {
  private CaptchaUtils() {
  }

  public static void checkCaptcha(ServletRequest request) throws BadInputException {
    // TODO find better way to get ReCaptcha from Spring
    WebApplicationContext ctx= RequestContextUtils.getWebApplicationContext(request);
    ReCaptcha captcha = (ReCaptcha) ctx.getBean("reCaptcha");

    String captchaChallenge = request.getParameter("recaptcha_challenge_field");
    String captchaResponse = request.getParameter("recaptcha_response_field");

    if (captchaChallenge==null || captchaResponse==null) {
      throw new BadInputException("Код проверки не указан");
    }

    ReCaptchaResponse response = captcha.checkAnswer(request.getRemoteAddr(), captchaChallenge, captchaResponse);

    if (!response.isValid()) {
      throw new BadInputException("Код проверки не совпадает");
    }
  }
}
