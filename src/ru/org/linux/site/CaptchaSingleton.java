/*
 * Copyright 1998-2009 Linux.org.ru
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
import javax.servlet.http.HttpSession;
import com.octo.captcha.service.CaptchaServiceException;
import com.octo.captcha.service.image.DefaultManageableImageCaptchaService;
import com.octo.captcha.service.image.ImageCaptchaService;

public class CaptchaSingleton {
//  private static final Logger logger = Logger.getLogger("ru.org.linux");
  private static final ImageCaptchaService instance = new DefaultManageableImageCaptchaService();

  private CaptchaSingleton() {
  }

  public static ImageCaptchaService getInstance() {
    return instance;
  }

  public static void checkCaptcha(HttpSession session, ServletRequest request) throws BadInputException {
    String captchaResponse = request.getParameter("j_captcha_response");

    if (request.getAttribute("j_captcha_response") != null && !"".equals(request.getAttribute("j_captcha_response"))) {
      captchaResponse = (String) request.getAttribute("j_captcha_response");
    }

    checkCaptcha(session, captchaResponse);
  }

  public static void checkCaptcha(HttpSession session, String captchaResponse) throws BadInputException {
    String captchaId = session.getId();

    try {
      if (!getInstance().validateResponseForID(captchaId, captchaResponse)) {
//        String logmessage = "Captcha: сбой проверки response='" + captchaResponse + "' " + LorHttpUtils.getRequestIP(request);
//        logger.info(logmessage);

        throw new BadInputException("сбой добавления: код проверки не совпадает");
      }
    } catch (CaptchaServiceException e) {
      throw new BadInputException("сбой добавления: сбой проверки кода проверки");
    }
  }
}
