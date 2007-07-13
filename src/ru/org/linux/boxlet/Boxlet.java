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
