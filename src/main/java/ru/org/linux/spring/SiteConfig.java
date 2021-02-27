/*
 * Copyright 1998-2018 Linux.org.ru
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

package ru.org.linux.spring;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Properties;

/**
 * Конфигурация
 */
@Service
public class SiteConfig {
  private static final String ERR_MSG = "Invalid MainUrl property: ";

  private final Properties properties;

  private URI mainURI;
  private URI secureURI;

  @Autowired
  public SiteConfig(@Qualifier("properties") Properties properties) {
    this.properties = properties;
  }

  /**
   * Предполагается, что на этапе запуска приложения, если с MainUrl что-то не так то контейнер не запустится :-)
   */
  @PostConstruct
  public void init() {
    try {
      mainURI = new URI(properties.getProperty("MainUrl"), true, "UTF-8");
    } catch (Exception e) {
      throw new RuntimeException(ERR_MSG +e.getMessage());
    }
    if(!mainURI.isAbsoluteURI()) {
      throw new RuntimeException(ERR_MSG +"URI not absolute path");
    }

    try {
      String mainHost = mainURI.getHost();
      if(mainHost == null) {
        throw new RuntimeException(ERR_MSG +"bad URI host");
      }
    } catch (URIException e) {
     throw new RuntimeException(ERR_MSG +e.getMessage());
    }

    try {
      secureURI = new URI(properties.getProperty("SecureUrl", mainURI.toString().replaceFirst("http", "https")), true, "UTF-8");
    } catch (Exception e) {
      throw new RuntimeException(ERR_MSG +e.getMessage());
    }
  }

  public String getSecureUrlWithoutSlash() {
    return getSecureUrl().replaceFirst("/$", "");
  }

  public String getSecureUrl() {
    return secureURI.toString();
  }

  public URI getSecureURI() {
    return secureURI;
  }

  public String getWSUrl() {
    return properties.getProperty("WSUrl");
  }

  public URI getMainURI() {
    return mainURI;
  }

  public String getElasticsearch() {
    return properties.getProperty("Elasticsearch");
  }

  public String getHTMLPathPrefix() {
    return properties.getProperty("HTMLPathPrefix");
  }

  public String getUploadPath() {
    return properties.getProperty("upload.path");
  }

  public String getCaptchaPublicKey() {
    return properties.getProperty("recaptcha.public");
  }

  public String getCaptchaPrivateKey() {
    return properties.getProperty("recaptcha.private");
  }

  public String getSecret() {
    return properties.getProperty("Secret");
  }
  public String getAdminEmailAddress() {
    return properties.getProperty("admin.emailAddress");
  }

  /**
   * Разрешено ли модераторам править чужие комментарии.
   *
   * @return true если разрешено, иначе false
   */
  public Boolean isModeratorAllowedToEditComments() {
    String property = properties.getProperty("comment.isModeratorAllowedToEdit");
    if (property == null) {
      return false;
    }
    return Boolean.valueOf(property);
  }

  /**
   * Добавление заголовков Strict-Transport-Security.
   *
   * @return true если разрешено, иначе false
   */
  public Boolean enableHsts() {
    String property = properties.getProperty("EnableHsts");
    if (property == null) {
      return false;
    }
    return Boolean.valueOf(property);
  }

  /**
   * По истечении какого времени с момента добавления комментарий нельзя будет изменять.
   *
   * @return время в минутах
   */
  public Integer getCommentExpireMinutesForEdit() {
    String property = properties.getProperty("comment.expireMinutesForEdit");
    if (property == null) {
      return null;
    }
    return Integer.valueOf(property);
  }

  /**
   * Разрешено ли редактировать комментарии, если есть ответы.
   *
   * @return true если разрешено, иначе false
   */
  public Boolean isCommentEditingAllowedIfAnswersExists() {
    String property = properties.getProperty("comment.isEditingAllowedIfAnswersExists");
    if (property == null) {
      return false;
    }
    return Boolean.valueOf(property);
  }

  /**
   * какое минимальное значение скора должно быть, чтобы пользователь мог редактировать комментарии.
   *
   * @return минимальное значение скора
   */
  public Integer getCommentScoreValueForEditing() {
    String property = properties.getProperty("comment.scoreValueForEditing");
    if (property == null) {
      return null;
    }
    return Integer.valueOf(property);
  }
  
  /**
   * Полное удаление аккаунта, с переносом сообщений к Delete.
   *
   * @return true если разрешено, иначе false
   */
  public Boolean isUserFullDelete() {
    String property = properties.getProperty("user.FullDelete");
    if (property == null) {
      return false;
    }
    return Boolean.valueOf(property);
  }
}
