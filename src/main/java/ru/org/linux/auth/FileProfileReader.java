/*
 * Copyright 1998-2012 Linux.org.ru
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
package ru.org.linux.auth;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.org.linux.site.DefaultProfile;
import ru.org.linux.spring.Configuration;
import ru.org.linux.storage.FileStorage;
import ru.org.linux.storage.Storage;
import ru.org.linux.storage.StorageException;
import ru.org.linux.user.Profile;
import ru.org.linux.user.ProfileProperties;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class FileProfileReader implements ProfileReader {
  private static final Log logger = LogFactory.getLog(FileProfileReader.class);

  private final Configuration configuration;

  public FileProfileReader(Configuration configuration) {
    this.configuration = configuration;
  }

  public Profile readProfile(String username) {
    Storage storage = new FileStorage(configuration.getPathPrefix() + "linux-storage/");
    InputStream df = null;
    Map<String, Object> userProfile = null;
    try {
      df = storage.getReadStream("profile", username);
      ObjectInputStream dof = null;
      try {
        dof = new ObjectInputStream(df);
        userProfile = (Map<String, Object>) dof.readObject();
        dof.close();
        df.close();
      } catch (IOException e) {
        logger.info("Bad profile for user "+username, e);
      } finally {
        if (dof!=null) {
          try {
            dof.close();
          } catch (IOException e) {
            logger.info("Bad profile for user "+username, e);
          }
        }
      }
    } catch (StorageException e) {
      logger.info("Bad profile for user "+username, e);
    } catch (ClassNotFoundException e) {
      logger.info("Bad profile for user "+username, e);
    } finally {
      if (df!=null) {
        try {
          df.close();
        } catch (IOException e) {
          logger.info("Bad profile for user "+username, e);
        }
      }
    }
    ProfileProperties properties;
    if(userProfile != null) {
      properties = new ProfileProperties(new ProfileHashtable(DefaultProfile.getDefaultProfile(), userProfile));
    } else {
      properties = new ProfileProperties(new ProfileHashtable(DefaultProfile.getDefaultProfile(), new HashMap<String, Object>()));
    }
    boolean isdefault = false;
    return new Profile(properties, isdefault);
  }

  public void writeProfile(String username, Profile profile) throws IOException, AccessViolationException {
    if (username.charAt(0) == '_') {
      throw new AccessViolationException("нельзя менять специальный профиль");
    }

    if (!StringUtil.checkLoginName(username)) {
      throw new AccessViolationException("некорректное имя пользователя");
    }

    if ("anonymous".equals(username)) {
      throw new AccessViolationException("нельзя менять профиль по умолчанию");
    }

    Storage storage = new FileStorage(configuration.getPathPrefix() + "linux-storage/");
    OutputStream df = null;
    try {
      df = storage.getWriteStream("profile", username);
      profile.write(df);
    } catch (StorageException e) {
      throw new IOException(e.getMessage());
    } finally {
      if (df!=null) {
        df.close();
      }
    }

  }


}
