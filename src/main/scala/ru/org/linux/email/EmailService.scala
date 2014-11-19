package ru.org.linux.email

import java.io.{PrintWriter, StringWriter}
import java.util.{Date, Properties}
import javax.mail.internet.{AddressException, InternetAddress, MimeMessage}
import javax.mail.{Message, Session, Transport}
import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.slf4j.StrictLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ru.org.linux.spring.SiteConfig
import ru.org.linux.user.User

import scala.collection.JavaConversions._
import scala.util.control.NonFatal

@Service
class EmailService @Autowired () (siteConfig:SiteConfig) extends StrictLogging {
  def sendEmail(nick: String, email: String, isNew: Boolean):Unit = {
    val regcode = User.getActivationCode(siteConfig.getSecret, nick, email)

    val text = new StringBuilder
    text.append(
      """
        |Здравствуйте!
        |
      """.stripMargin)

    if (isNew) {
      text.append("На форуме по адресу http://www.linux.org.ru/ появилась регистрационная запись,\n")
    } else {
      text.append("На форуме по адресу http://www.linux.org.ru/ была изменена регистрационная запись,\n")
    }

    text.append(
      s"""
         |в которой был указал ваш электронный адрес (e-mail).
         |
         |При заполнении регистрационной формы было указано следующее имя пользователя: '$nick'
         |
         |Если вы не понимаете, о чем идет речь - просто проигнорируйте это сообщение!
         |
       """.stripMargin)

    if (isNew) {
      text.append(
        """
          |Если же именно вы решили зарегистрироваться на форуме по адресу http://www.linux.org.ru/,
          |то вам следует подтвердить свою регистрацию и тем самым активировать вашу учетную запись.
          |
        """.stripMargin)
    } else {
      text.append(
        """
          |Если же именно вы решили изменить свою регистрационную запись http://www.linux.org.ru/,
          |то вам следует подтвердить свое изменение.
          |
        """.stripMargin)
    }

    text.append(
      s"""
         |Для активации перейдите по ссылке https://www.linux.org.ru/activate.jsp
         |
         |Код активации: $regcode
         |
         |Благодарим за регистрацию!
         |
       """.stripMargin)

    sendRegistrationMail(email, text.toString())
  }

  private def sendRegistrationMail(email: String, text: String):Unit = {
    val emailMessage = prepareMimeMessage
    emailMessage.setFrom(new InternetAddress("no-reply@linux.org.ru"))
    emailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(email))
    emailMessage.setSubject("Linux.org.ru registration")
    emailMessage.setSentDate(new Date)
    emailMessage.setText(text, "UTF-8")
    Transport.send(emailMessage)

    logger.info(s"Sent new/update registration email to $email")
  }

  private def prepareMimeMessage = {
    val props = new Properties
    props.put("mail.smtp.host", "localhost")
    new MimeMessage(Session.getDefaultInstance(props, null))
  }

  /**
   * Отсылка E-mail администраторам.
   *
   * @param request   данные запроса от web-клиента
   * @param exception исключение
   * @return Строку, содержащую состояние отсылки письма
   */
  def sendExceptionReport(request: HttpServletRequest, exception: Exception): String = {
    val text = new StringBuilder

    if (exception.getMessage == null) {
      text.append(exception.getClass.getName)
    } else {
      text.append(exception.getMessage)
    }

    text.append("\n\n")
    val attributeUrl = request.getAttribute("javax.servlet.error.request_uri")
    if (attributeUrl != null) {
      text.append(s"Attribute URL: $attributeUrl\n")
    }
    val forwardUrl = request.getAttribute("javax.servlet.forward.request_uri")
    if (forwardUrl != null) {
      text.append(s"Forward URL: $forwardUrl\n")
    }
    val mainUrl = siteConfig.getMainUrlWithoutSlash
    text.append(s"${request.getMethod}: $mainUrl${request.getServletPath}")
    if (request.getQueryString != null) {
      text.append(s"?${request.getQueryString}")
    }
    text.append('\n')
    text.append(s"IP: ${request.getRemoteAddr}\n")
    text.append("Headers: ")

    for (name <- request.getHeaderNames) {
      text.append(s"\n         $name: ${request.getHeader(name)}")
    }

    text.append("\n\n")

    val exceptionStackTrace = new StringWriter
    exception.printStackTrace(new PrintWriter(exceptionStackTrace))
    text.append(exceptionStackTrace.toString)

    if (sendErrorMail(s"Linux.org.ru: ${exception.getClass}", text.toString())) {
      "Произошла непредвиденная ошибка. Администраторы получили об этом сигнал."
    } else {
      "Произошла непредвиденная ошибка. К сожалению сервер временно не принимает сообщения об ошибках."
    }
  }

  private def sendErrorMail(subject: String, text: String): Boolean = {
    val adminEmailAddress = siteConfig.getAdminEmailAddress

    val emailMessage = prepareMimeMessage

    try {
      val mail = new InternetAddress(adminEmailAddress, true)

      emailMessage.setFrom(new InternetAddress("no-reply@linux.org.ru"))
      emailMessage.addRecipient(Message.RecipientType.TO, mail)
      emailMessage.setSubject(subject)
      emailMessage.setSentDate(new Date)
      emailMessage.setText(text.toString, "UTF-8")
      Transport.send(emailMessage)

      logger.info(s"Sent crash report to $adminEmailAddress")

      true
    } catch {
      case e: AddressException =>
        logger.warn(s"Неправильный e-mail адрес: $adminEmailAddress")
        false
      case NonFatal(e) =>
        logger.error("An error occured while sending e-mail!", e)
        false
    }
  }
}
