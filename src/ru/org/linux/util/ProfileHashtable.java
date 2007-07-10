package ru.org.linux.util;

import java.util.Map;

public class ProfileHashtable {
  private final Map Defaults;
  private final Map settings;

  public ProfileHashtable(Map defaults, Map settings) {
    Defaults = defaults;
    this.settings = settings;

    if (this.settings == null || Defaults == null) {
      throw new NullPointerException();
    }
  }

  public String getString(String prop) {
    if (settings.get(prop) != null) {
      return (String) settings.get(prop);
    } else {
      return (String) Defaults.get(prop);
    }
  }

  public boolean getBoolean(String prop) throws UtilException {
    if (settings.get(prop) != null) {
      return ((Boolean) settings.get(prop)).booleanValue();
    } else {
      Boolean value = (Boolean) Defaults.get(prop);
      if (value == null) {
        throw new UtilKeyNotFoundException(prop);
      }
      return value.booleanValue();
    }
  }

  public Object getObject(String prop) {
    if (settings.get(prop) != null) {
      return settings.get(prop);
    } else {
      return Defaults.get(prop);
    }
  }

  public String getBooleanPropertyHTML(String prop) throws UtilException {
    return getBoolean(prop) ? "checked" : "";
  }

  public int getInt(String prop) {
    if (settings.get(prop) != null) {
      return ((Integer) settings.get(prop)).intValue();
    } else {
      return ((Integer) Defaults.get(prop)).intValue();
    }
  }

  public long getLong(String prop) {
    if (settings.get(prop) != null) {
      return ((Long) settings.get(prop)).longValue();
    } else {
      return ((Long) Defaults.get(prop)).longValue();
    }
  }

  public boolean setInt(String prop, Integer value) {
    if (value != null && value.intValue() != getInt(prop)) {
      settings.put(prop, value);
      return true;
    } else {
      return false;
    }
  }

  public boolean setString(String prop, String value) {
    if (value != null && !value.equals(getString(prop))) {
      settings.put(prop, value);
      return true;
    } else {
      return false;
    }
  }

  public boolean setObject(String prop, Object value) {
    if (value != null) {
      settings.put(prop, value);
      return true;
    } else {
      return false;
    }
  }

  public boolean setBoolean(String prop, Boolean value) throws UtilException {
    if (value != null && value.booleanValue() != getBoolean(prop)) {
      settings.put(prop, value);
      return true;
    } else {
      return false;
    }
  }

  public boolean setBoolean(String prop, boolean value) throws UtilException {
    if (value != getBoolean(prop)) {
      settings.put(prop, Boolean.valueOf(value));
      return true;
    } else {
      return false;
    }
  }

  public boolean setBoolean(String prop, String value) throws UtilException {
    if (value == null) {
      return setBoolean(prop, Boolean.FALSE);
    }

    if ("on".equals(value)) {
      return setBoolean(prop, Boolean.TRUE);
    }

    return false;
  }

  public void addBoolean(String prop, boolean value) {
    settings.put(prop, Boolean.valueOf(value));
  }

  public void addObject(String prop, Object value) {
    settings.put(prop, value);
  }

  public void removeObject(String key) {
    settings.remove(key);
  }

  public Map getSettings() {
    return settings;
  }
}
