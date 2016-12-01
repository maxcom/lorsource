/*
 * Copyright 1998-2016 Linux.org.ru
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

package ru.org.linux.exception

import java.util.Date
import javax.mail.internet.{AddressException, InternetAddress}
import javax.mail.{Message, Transport}

import akka.actor.{Actor, ActorLogging, Props}
import ru.org.linux.email.EmailService
import ru.org.linux.exception.ExceptionMailingActor._
import ru.org.linux.util.SiteConfig

import scala.concurrent.duration._
import scala.language.existentials
import scala.util.control.NonFatal

class ExceptionMailingActor(siteConfig: SiteConfig) extends Actor with ActorLogging {
  import context.dispatcher

  context.system.scheduler.schedule(ResetAt, ResetAt, self, Reset)

  private var count = 0
  private var currentTypes = Set.empty[String]

  override def receive: Receive = {
    case Report(ex, msg) ⇒
      count += 1

      if (count < MaxMessages || !currentTypes.contains(ex.toString)) {
        sendErrorMail(s"Linux.org.ru: $ex", msg)
      } else {
        log.warning(s"Too many errors; skipped logging of $ex")
      }

      currentTypes = currentTypes + ex.toString
    case Reset ⇒
      if (count >= MaxMessages) {
        sendErrorMail(s"Linux.org.ru: high exception rate ($count in $ResetAt)", currentTypes.mkString("\n"))
      }

      count = 0
      currentTypes = Set.empty[String]
  }
  
  private def sendErrorMail(subject: String, text: String): Boolean = {
    val adminEmailAddress = siteConfig.getAdminEmailAddress

    val emailMessage = EmailService.createMessage

    try {
      val mail = new InternetAddress(adminEmailAddress, true)

      emailMessage.setFrom(new InternetAddress("no-reply@linux.org.ru"))
      emailMessage.addRecipient(Message.RecipientType.TO, mail)
      emailMessage.setSubject(subject)
      emailMessage.setSentDate(new Date)
      emailMessage.setText(text.toString, "UTF-8")
      Transport.send(emailMessage)

      log.info(s"Sent crash report to $adminEmailAddress")

      true
    } catch {
      case e: AddressException =>
        log.warning(s"Неправильный e-mail адрес: $adminEmailAddress")
        false
      case NonFatal(e) =>
        log.error(e, "An error occured while sending e-mail!")
        false
    }
  }
}

object ExceptionMailingActor {
  case class Report(ex: Class[_ <: Throwable], msg: String)
  case object Reset

  val ResetAt = 5 minutes
  val MaxMessages = 5

  def props(siteConfig: SiteConfig) = Props(classOf[ExceptionMailingActor], siteConfig)
}
