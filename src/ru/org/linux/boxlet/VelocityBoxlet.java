package ru.org.linux.boxlet;

import java.util.Properties;

import org.apache.velocity.app.VelocityEngine;

/**
 * User: sreentenko
 * Date: 30.04.2009
 * Time: 1:51:04
 */
public abstract class VelocityBoxlet extends Boxlet{
  protected VelocityEngine getEngine() throws Exception {
    Properties p = new Properties();
    p.setProperty("resource.loader", "class");
    p.setProperty("class.resource.loader.class",
          "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    VelocityEngine ve = new VelocityEngine();
    ve.init(p);
    return ve;
  }
}
