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

import ru.org.linux.auth.AuthUtil;
import ru.org.linux.util.ProfileHashtable;

import java.util.HashMap;

public class Profile {
  private final ProfileProperties properties;

  private final boolean isdefault;

  public Profile(ProfileProperties profileProperties, boolean isdefault) {
    properties = profileProperties;
    this.isdefault = isdefault;
  }

  public Profile() {
    properties = new ProfileProperties(new ProfileHashtable(AuthUtil.getDefaults(), new HashMap<String, Object>()));

    isdefault = true;
  }

  public boolean isDefault() {
    return isdefault;
  }

  public ProfileProperties getProperties() {
    return properties;
  }
}
