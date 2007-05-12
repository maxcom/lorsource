package ru.org.linux.site;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import com.octo.captcha.service.CaptchaServiceException;
import com.octo.captcha.service.image.DefaultManageableImageCaptchaService;
import com.octo.captcha.service.image.ImageCaptchaService;

import ru.org.linux.util.LorHttpUtils;
import ru.org.linux.logger.Logger;

public class CaptchaSingleton {
  private static final ImageCaptchaService instance = new DefaultManageableImageCaptchaService();

  public static ImageCaptchaService getInstance() {
    return instance;
  }

  public static void checkCaptcha(HttpSession session, HttpServletRequest request, Logger logger) throws BadInputException {
    String captchaId = session.getId();
    String captchaResponse = request.getParameter("j_captcha_response");

    try {
      if (!CaptchaSingleton.getInstance().validateResponseForID(captchaId, captchaResponse).booleanValue()) {
        String logmessage = "Captcha: сбой проверки response='" + captchaResponse + "' " + LorHttpUtils.getRequestIP(request);
        logger.notice("register", logmessage);

        throw new BadInputException("сбой добавления: код проверки не совпадает");
      }
    } catch (CaptchaServiceException e) {
      throw new BadInputException("сбой добавления: сбой проверки кода проверки");
    }
  }
}
