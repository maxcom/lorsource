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

package ru.org.linux.spring;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Service;

import ru.org.linux.site.Template;

@Service
public class FeedPinger extends ApplicationObjectSupport {
  @Autowired(required = true)
  private Properties properties;

  @Autowired(required = true)
  private XmlRpcClientConfigImpl config;

  public void pingFeedburner() {
    try {
      config.setServerURL(new URL("http://ping.feedburner.com/"));

      XmlRpcClient client = new XmlRpcClient();

      client.setConfig(config);

      Object[] params = new Object[]{"Linux.org.ru", properties.getProperty(Template.PROPERTY_MAIN_URL)};

      Map r = (Map) client.execute("weblogUpdates.ping", params);

      if ((Boolean) r.get("flerror")) {
        logger.warn("Feedburner ping failed: "+r.get("message"));
      } else {
        logger.info("Feedburner ping ok: "+r.get("message"));
      }
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    } catch (XmlRpcException e) {
      logger.warn("Feedburner ping failed", e);
    }
  }

  public void setProperties(Properties properties) {
    this.properties = properties;
  }

  public void setConfig(XmlRpcClientConfigImpl config) {
    this.config = config;
  }
}
