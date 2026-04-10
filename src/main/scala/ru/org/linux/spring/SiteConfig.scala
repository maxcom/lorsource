/*
 * Copyright 1998-2026 Linux.org.ru
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

package ru.org.linux.spring

import org.apache.commons.httpclient.{URI, URIException}
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

import java.util.Properties

/** Конфигурация
  */
@Service
class SiteConfig(
    @Qualifier("properties")
    properties: Properties):
  val mainURI: URI =
    try
      new URI(properties.getProperty("MainUrl"), true, "UTF-8")
    catch
      case e: Exception =>
        throw new RuntimeException(SiteConfig.ErrMsg + e.getMessage)

  if !mainURI.isAbsoluteURI then
    throw new RuntimeException(SiteConfig.ErrMsg + "URI not absolute path")

  try
    val mainHost = mainURI.getHost
    if mainHost == null then
      throw new RuntimeException(SiteConfig.ErrMsg + "bad URI host")
  catch
    case e: URIException =>
      throw new RuntimeException(SiteConfig.ErrMsg + e.getMessage)

  val secureURI: URI =
    try
      new URI(properties.getProperty("SecureUrl", mainURI.toString.replaceFirst("http", "https")), true, "UTF-8")
    catch
      case e: Exception =>
        throw new RuntimeException(SiteConfig.ErrMsg + e.getMessage)

  def getSecureUrlWithoutSlash: String = getSecureUrl.replaceFirst("/$", "")

  def getSecureUrl: String = secureURI.toString

  def getSecureURI: URI = secureURI

  def getWSUrl: String = properties.getProperty("WSUrl")

  def getMainURI: URI = mainURI

  def getElasticsearch: String = properties.getProperty("Elasticsearch")

  def getHTMLPathPrefix: String = properties.getProperty("HTMLPathPrefix")

  def getUploadPath: String = properties.getProperty("upload.path")

  def getCaptchaPublicKey: String = properties.getProperty("captcha.public")

  def getCaptchaPrivateKey: String = properties.getProperty("captcha.private")

  def getSecret: String = properties.getProperty("Secret")

  def getAdminEmailAddress: String = properties.getProperty("admin.emailAddress")

  /** Разрешено ли модераторам править чужие комментарии.
    *
    * @return
    *   true если разрешено, иначе false
    */
  def isModeratorAllowedToEditComments: Boolean =
    val property = properties.getProperty("comment.isModeratorAllowedToEdit")
    if property == null then
      false
    else
      property.toBoolean

  /** Добавление заголовков Strict-Transport-Security.
    *
    * @return
    *   true если разрешено, иначе false
    */
  def enableHsts(): Boolean =
    val property = properties.getProperty("EnableHsts")
    if property == null then
      false
    else
      property.toBoolean

  /** По истечении какого времени с момента добавления комментарий нельзя будет изменять.
    *
    * @return
    *   время в минутах
    */
  def getCommentExpireMinutesForEdit: Int =
    val property = properties.getProperty("comment.expireMinutesForEdit")
    property.toInt

  /** Разрешено ли редактировать комментарии, если есть ответы.
    *
    * @return
    *   true если разрешено, иначе false
    */
  def isCommentEditingAllowedIfAnswersExists: Boolean =
    val property = properties.getProperty("comment.isEditingAllowedIfAnswersExists")
    if property == null then
      false
    else
      property.toBoolean

  /** какое минимальное значение скора должно быть, чтобы пользователь мог редактировать комментарии.
    *
    * @return
    *   минимальное значение скора
    */
  def getCommentScoreValueForEditing: Int =
    val property = properties.getProperty("comment.scoreValueForEditing")
    property.toInt

  def getTelegramToken: String = properties.getProperty("telegram.token")

  def getFallbackProxyHost: String = properties.getProperty("fallback.proxy.host")

  def getFallbackProxyPort: Int = properties.getProperty("fallback.proxy.port").toInt

object SiteConfig:
  private val ErrMsg = "Invalid MainUrl property: "
