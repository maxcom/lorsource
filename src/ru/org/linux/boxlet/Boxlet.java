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

package ru.org.linux.boxlet;

import java.util.Date;

import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.UtilException;

public abstract class Boxlet {
  protected Object config;

  public Boxlet() {
  }

  public String getContent(Object Config, ProfileHashtable profile) throws Exception {
    config = Config;
    return getContentImpl(profile);
  }

  public abstract String getContentImpl(ProfileHashtable profile) throws Exception;

  public abstract String getInfo();

  public String getVariantID(ProfileHashtable profile) throws UtilException {
//		return "ProfileName="+profile.getString("ProfileName");
    return "";
  }

  public Date getExpire() {
    return new Date(new Date().getTime() + 30*1000);
  }
}
