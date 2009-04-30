package ru.org.linux.spring.commons;

import java.util.Properties;

/**
 * User: sreentenko
 * Date: 01.05.2009
 * Time: 1:36:06
 */
public class PropertiesFacade {
  private Properties general;
  private Properties dist;

  public Properties getGeneral() {
    return general;
  }

  public void setGeneral(Properties general) {
    this.general = general;
  }

  public Properties getDist() {
    return dist;
  }

  public void setDist(Properties dist) {
    this.dist = dist;
  }

  public Properties getProperties(){
    if (general.isEmpty()){
      return dist;
    }
    return general;
  }
}
