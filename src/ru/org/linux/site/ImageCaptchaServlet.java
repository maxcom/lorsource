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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.octo.captcha.service.CaptchaServiceException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

public class ImageCaptchaServlet extends HttpServlet {
  protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {

    byte[] captchaChallengeAsJpeg = null;
    // the output stream to render the captcha image as jpeg into
    ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();
    try {
      // get the session id that will identify the generated captcha.
      //the same id must be used to validate the response, the session id is a good candidate!
      String captchaId = httpServletRequest.getSession().getId();
      // call the ImageCaptchaService getChallenge method
      BufferedImage challenge =
          CaptchaSingleton.getInstance().getImageChallengeForID(captchaId,
              httpServletRequest.getLocale());

      // a jpeg encoder
      JPEGImageEncoder jpegEncoder =
          JPEGCodec.createJPEGEncoder(jpegOutputStream);
      jpegEncoder.encode(challenge);
    } catch (IllegalArgumentException e) {
      httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    } catch (CaptchaServiceException e) {
      httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }

    captchaChallengeAsJpeg = jpegOutputStream.toByteArray();

    // flush it in the response
    httpServletResponse.setHeader("Cache-Control", "no-store");
    httpServletResponse.setHeader("Pragma", "no-cache");
    httpServletResponse.setDateHeader("Expires", 0);
    httpServletResponse.setContentType("image/jpeg");
    ServletOutputStream responseOutputStream =
        httpServletResponse.getOutputStream();
    responseOutputStream.write(captchaChallengeAsJpeg);
    responseOutputStream.flush();
    responseOutputStream.close();
  }
}
