package ru.org.linux.site;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import com.octo.captcha.service.CaptchaServiceException;
import com.octo.captcha.service.image.DefaultManageableImageCaptchaService;
import com.octo.captcha.service.image.ImageCaptchaService;

import ru.org.linux.util.LorHttpUtils;

public class CaptchaSingleton {
  private static final Logger logger = Logger.getLogger("ru.org.linux");
  private static final ImageCaptchaService instance = new DefaultManageableImageCaptchaService();

  private CaptchaSingleton() {
  }

  public static ImageCaptchaService getInstance() {
    return instance;
  }

  public static void checkCaptcha(HttpSession session, HttpServletRequest request) throws BadInputException {
    String captchaId = session.getId();
    String captchaResponse = request.getParameter("j_captcha_response");

    if (request.getAttribute("j_captcha_response") != null && !"".equals(request.getAttribute("j_captcha_response"))) {
      captchaResponse = (String) request.getAttribute("j_captcha_response");
    }

    try {
      if (!CaptchaSingleton.getInstance().validateResponseForID(captchaId, captchaResponse)) {
        String logmessage = "Captcha: сбой проверки response='" + captchaResponse + "' " + LorHttpUtils.getRequestIP(request);
        logger.info(logmessage);

        throw new BadInputException("сбой добавления: код проверки не совпадает");
      }
    } catch (CaptchaServiceException e) {
      throw new BadInputException("сбой добавления: сбой проверки кода проверки");
    }
  }
}
