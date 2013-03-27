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

package ru.org.linux.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.org.linux.site.DefaultProfile;
import ru.org.linux.spring.Configuration;
import ru.org.linux.storage.FileStorage;
import ru.org.linux.storage.Storage;
import ru.org.linux.storage.StorageException;
import ru.org.linux.util.ProfileHashtable;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Repository
public class ProfileDao {
  private static final Logger logger = LoggerFactory.getLogger(ProfileDao.class);

  @Autowired
  private Configuration configuration;

  @Nonnull
  public Profile readProfile(@NotNull User user) {
    Storage storage = new FileStorage(configuration.getPathPrefix() + "linux-storage/");
    InputStream df = null;
    Map<String, Object> userProfile = null;
    try {
      df = storage.getReadStream("profile", user.getNick());
      ObjectInputStream dof = null;
      try {
        dof = new ObjectInputStream(df);
        userProfile = (Map<String, Object>) dof.readObject();
        dof.close();
        df.close();
      } catch (IOException e) {
        logger.info("Bad profile for user " + user.getNick());
      } finally {
        if (dof != null) {
          try {
            dof.close();
          } catch (IOException e) {
            logger.info("Bad profile for user " + user.getNick());
          }
        }
      }
    } catch (StorageException | ClassNotFoundException e) {
      logger.info("Bad profile for user " + user.getNick());
    } finally {
      if (df != null) {
        try {
          df.close();
        } catch (IOException e) {
          logger.info("Bad profile for user " + user.getNick());
        }
      }
    }
    Profile properties;
    if (userProfile != null) {
      properties = new Profile(new ProfileHashtable(DefaultProfile.getDefaultProfile(), userProfile));
    } else {
      properties = new Profile(new ProfileHashtable(DefaultProfile.getDefaultProfile(), new HashMap<String, Object>()));
    }
    return properties;
  }

  public void writeProfile(@Nonnull User user, @Nonnull Profile profile) throws IOException, StorageException {
    ProfileHashtable profileHashtable = profile.getHashtable();

    profileHashtable.setObject(Profile.TIMESTAMP_PROPERTY, System.currentTimeMillis());

    Storage storage = new FileStorage(configuration.getPathPrefix() + "linux-storage/");
    OutputStream df = storage.getWriteStream("profile", user.getNick());

    try (ObjectOutputStream dof = new ObjectOutputStream(df)) {
      dof.writeObject(profileHashtable.getSettings());
      dof.close();
    }
  }
}
