package ru.org.linux.boxlet;

import java.util.Date;
import java.util.Properties;

import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.UtilException;

public abstract class Boxlet {
  protected Object config;

  protected Properties request;

  public Boxlet() {
  }

  public String getContent(Object Config, ProfileHashtable profile) throws Exception {
    config = Config;
    return getContentImpl(profile);
  }

  public abstract String getContentImpl(ProfileHashtable profile) throws Exception;

  public abstract String getInfo();

  public String getVariantID(ProfileHashtable profile, Properties request) throws UtilException {
//		return "ProfileName="+profile.getStringProperty("ProfileName");
    return "";
  }

  public long getVersionID(ProfileHashtable profile, Properties request) {
    return new Date().getTime();
  }
}
