/*
 * Copyright 1998-2015 Linux.org.ru
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

package ru.org.linux.util;

import java.util.Map;

public class ProfileHashtable {
  private final Map<String, Object> defaults;
  private final Map<String, String> settings;

  public ProfileHashtable(Map<String, Object> defaults, Map<String, String> settings) {
    this.defaults = defaults;
    this.settings = settings;

    if (this.settings == null || this.defaults == null) {
      throw new NullPointerException();
    }
  }

  public String getString(String prop) {
    if (settings.get(prop) != null) {
      return settings.get(prop);
    } else {
      return (String) defaults.get(prop);
    }
  }

  public boolean getBoolean(String prop) {
    if (settings.get(prop) != null) {
      return Boolean.valueOf(settings.get(prop));
    } else {
      Boolean value = (Boolean) defaults.get(prop);
      if (value == null) {
        throw new RuntimeException("unknown property '"+prop+"'; no default value");
      }
      return value;
    }
  }

  public int getInt(String prop) {
    if (settings.get(prop) != null) {
      return Integer.parseInt(settings.get(prop));
    } else {
      return (Integer) defaults.get(prop);
    }
  }

  public void setInt(String prop, Integer value) {
    if (value != null && value != getInt(prop)) {
      settings.put(prop, Integer.toString(value));
    }
  }

  public void setString(String prop, String value) {
    if (value != null && !value.equals(getString(prop))) {
      settings.put(prop, value);
    }
  }

  public void setBoolean(String prop, boolean value) {
    if (value != getBoolean(prop)) {
      settings.put(prop, Boolean.toString(value));
    }
  }

  public Map<String, String> getSettings() {
    return settings;
  }
}
