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
package ru.org.linux.exception;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.site.Template;
import ru.org.linux.spring.Configuration;
import ru.org.linux.user.UserErrorException;
import ru.org.linux.util.StringUtil;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Класс. предназначенный для перехвата исключений приложения.
 */

@Component
public class ExceptionResolver extends SimpleMappingExceptionResolver {
  private static final Log logger = LogFactory.getLog(ExceptionResolver.class);

  private static final String EMAIL_SENT = "Произошла непредвиденная ошибка. Администраторы получили об этом сигнал.";
  private static final String EMAIL_NOT_SENT = "Произошла непредвиденная ошибка. К сожалению сервер временно не принимает сообщения об ошибках.";

  @Autowired
  private Configuration configuration;

  enum ExceptionType {
    IGNORED,
    SCRIPT_ERROR,
    OTHER
  }

  /**
   * Общий обработчик исключительных ситуаций.
   * Предназначен для расширения функционала стандартного обработчика.
   *
   * @param request  данные запроса от web-клиента
   * @param response данные ответа web-клиенту
   * @param handler  объект, в котором возникло исключение
   * @param ex       исключение
   * @return объект web-модели
   */
  @Override
  protected ModelAndView doResolveException(
    HttpServletRequest request,
    HttpServletResponse response,
    Object handler,
    Exception ex
  ) {
    // http://stackoverflow.com/questions/8271843/how-to-exclude-clientabortexception-from-simplemappingexceptionresolver
    if (ex!=null && ex.getClass().getName().endsWith(".ClientAbortException") && response.isCommitted()) {
      return null;
    }

    ModelAndView modelAndView = super.doResolveException(request, response, handler, ex);
    if (modelAndView == null) {
      modelAndView = new ModelAndView("errors/common");
      prepareModelForCommonException(modelAndView, request, ex);
    }
    modelAndView.addObject("exception", ex);
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    return modelAndView;
  }

  /**
   * Наполнение объекта web-модели необходимыми для показа данными.
   *
   * @param modelAndView объект web-модели
   * @param request      данные запроса от web-клиента
   * @param exception    исключение
   */
  private void prepareModelForCommonException(
    ModelAndView modelAndView,
    HttpServletRequest request,
    Exception exception
  ) {
    modelAndView.addObject("headTitle", StringUtil.escapeHtml(exception.getClass().getName()));

    String errorMessage = exception.getMessage() == null
      ? StringUtil.escapeHtml(exception.getClass().getName())
      : StringUtil.escapeHtml(exception.getMessage());
    modelAndView.addObject("errorMessage", errorMessage);

    ExceptionType exceptionType = ExceptionType.OTHER;
    if (exception instanceof UserErrorException) {
      exceptionType = ExceptionType.IGNORED;
    } else if (exception instanceof ScriptErrorException) {
      logger.debug("errors/common.jsp", exception);
      exceptionType = ExceptionType.SCRIPT_ERROR;
    } else {
      logger.warn("Unexcepted exception caught", exception);
      String infoMessage = sendEmailToAdmin(request, exception);
      modelAndView.addObject("infoMessage", infoMessage);
    }
    modelAndView.addObject("exceptionType", exceptionType.name());
  }

  /**
   * Отсылка E-mail администраторам.
   *
   * @param request   данные запроса от web-клиента
   * @param exception исключение
   * @return Строку, содержащую состояние отсылки письма
   */
  private String sendEmailToAdmin(
    HttpServletRequest request,
    Exception exception
  ) {
    InternetAddress mail;
    String adminEmailAddress = configuration.getAdminEmailAddress();

    try {
      mail = new InternetAddress(adminEmailAddress, true);
    } catch (AddressException e) {
      return EMAIL_NOT_SENT + " Неправильный e-mail адрес: " + adminEmailAddress;
    }
    StringBuilder text = new StringBuilder();

    if (exception.getMessage() == null) {
      text.append(exception.getClass().getName());
    } else {
      text.append(exception.getMessage());
    }
    text.append("\n\n");

    Template tmpl = Template.getTemplate(request);
//    text.append("Main URL: ").append(tmpl.getMainUrl()).append(request.getAttribute("javax.servlet.error.request_uri"));
    String mainUrl = "<unknown>";

    mainUrl = configuration.getMainUrl();

    text.append("Main URL: ").append(mainUrl).append(request.getServletPath());

    if (request.getQueryString() != null) {
      text.append('?').append(request.getQueryString()).append('\n');
    }
    text.append('\n');

    text.append("IP: " + request.getRemoteAddr() + '\n');

    text.append(" Headers: ");
    Enumeration enu = request.getHeaderNames();
    while (enu.hasMoreElements()) {
      String paramName = (String) enu.nextElement();
      text.append("\n         ").append(paramName).append(": ").append(request.getHeader(paramName));
    }
    text.append("\n\n");

    StringWriter exceptionStackTrace = new StringWriter();
    exception.printStackTrace(new PrintWriter(exceptionStackTrace));
    text.append(exceptionStackTrace.toString());

    Properties props = new Properties();
    props.put("mail.smtp.host", "localhost");
    Session mailSession = Session.getDefaultInstance(props, null);

    MimeMessage emailMessage = new MimeMessage(mailSession);
    try {
      emailMessage.setFrom(new InternetAddress("no-reply@linux.org.ru"));
      emailMessage.addRecipient(Message.RecipientType.TO, mail);
      emailMessage.setSubject("Linux.org.ru: " + exception.getClass());
      emailMessage.setSentDate(new Date());
      emailMessage.setText(text.toString(), "UTF-8");
    } catch (Exception e) {
      logger.error("An error occured while creating e-mail!", e);
      return EMAIL_NOT_SENT;
    }
    try {
      Transport.send(emailMessage);
      return EMAIL_SENT;
    } catch (Exception e) {
      return EMAIL_NOT_SENT;
    }
  }
}
