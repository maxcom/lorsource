/*
 * Copyright 1998-2010 Linux.org.ru
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

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ru.org.linux.util.ProfileHashtable;

import com.google.common.collect.ImmutableMap;

public class Profile {
  private final ProfileHashtable profileHashtable;

  private final boolean isdefault;
  public static final String SYSTEM_TIMESTAMP = "system.timestamp";

  public Profile(InputStream df) throws IOException, ClassNotFoundException {
    if (df==null) {
      throw new NullPointerException();
    }

    ObjectInputStream dof = null;
    try {
      dof = new ObjectInputStream(df);
      Map<String, Object> userProfile = (Map<String, Object>) dof.readObject();
      dof.close();
      df.close();

      profileHashtable = new ProfileHashtable(getDefaults(), userProfile);

      isdefault = false;
    } finally {
      if (dof!=null) {
        dof.close();
      }
    }
  }

  public Profile() {
    profileHashtable = new ProfileHashtable(getDefaults(), new HashMap<String, Object>());

    isdefault = true;
  }

  public boolean isDefault() {
    return isdefault;
  }

  public ProfileHashtable getHashtable() {
    return profileHashtable;
  }

  public static ImmutableMap<String, Object> getDefaults() {
    return DefaultProfile.getDefaultProfile();
  }

  public void write(OutputStream df) throws IOException {
    profileHashtable.setObject(SYSTEM_TIMESTAMP, new Date().getTime());

    ObjectOutputStream dof = null;
    try {
      dof = new ObjectOutputStream(df);
      dof.writeObject(profileHashtable.getSettings());
      dof.close();
    } finally {
      if (dof!=null) {
        dof.close();
      }
    }
  }
}
