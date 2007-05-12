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

  public String getStringProperty(String prop) {
    if (settings.get(prop) != null) {
      return (String) settings.get(prop);
    } else {
      return (String) Defaults.get(prop);
    }
  }

  public boolean getBooleanProperty(String prop) throws UtilException {
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

  public Object getObjectProperty(String prop) {
    if (settings.get(prop) != null) {
      return settings.get(prop);
    } else {
      return Defaults.get(prop);
    }
  }

  public String getBooleanPropertyHTML(String prop) throws UtilException {
    return getBooleanProperty(prop) ? "checked" : "";
  }

  public int getIntProperty(String prop) {
    if (settings.get(prop) != null) {
      return ((Integer) settings.get(prop)).intValue();
    } else {
      return ((Integer) Defaults.get(prop)).intValue();
    }
  }

  public long getLongProperty(String prop) {
    if (settings.get(prop) != null) {
      return ((Long) settings.get(prop)).longValue();
    } else {
      return ((Long) Defaults.get(prop)).longValue();
    }
  }

  public int getIntSystemProperty(String prop) {
    return ((Integer) Defaults.get("system." + prop)).intValue();
  }

  public long getLongSystemProperty(String prop) {
    return ((Long) Defaults.get("system." + prop)).longValue();
  }

  public String getStringSystemProperty(String prop) {
    return ((String) Defaults.get("system." + prop));
  }

  public boolean setIntProperty(String prop, Integer value) {
    if (value != null && value.intValue() != getIntProperty(prop)) {
      settings.put(prop, value);
      return true;
    } else {
      return false;
    }
  }

  public boolean setStringProperty(String prop, String value) {
    if (value != null && !value.equals(getStringProperty(prop))) {
      settings.put(prop, value);
      return true;
    } else {
      return false;
    }
  }

  public boolean setObjectProperty(String prop, Object value) {
    if (value != null) {
      settings.put(prop, value);
      return true;
    } else {
      return false;
    }
  }

  public boolean setBooleanProperty(String prop, Boolean value) throws UtilException {
    if (value != null && value.booleanValue() != getBooleanProperty(prop)) {
      settings.put(prop, value);
      return true;
    } else {
      return false;
    }
  }

  public boolean setBooleanProperty(String prop, boolean value) throws UtilException {
    if (value != getBooleanProperty(prop)) {
      settings.put(prop, Boolean.valueOf(value));
      return true;
    } else {
      return false;
    }
  }


  public boolean setBooleanProperty(String prop, String value) throws UtilException {
    if (value == null) {
      return setBooleanProperty(prop, Boolean.FALSE);
    }

    if ("on".equals(value)) {
      return setBooleanProperty(prop, Boolean.TRUE);
    }

    return false;
  }
}
