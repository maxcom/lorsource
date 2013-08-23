/*
 * Copyright 1998-2013 Linux.org.ru
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

@Service
public class FeedPinger {
  private static final Log logger = LogFactory.getLog(FeedPinger.class);

  @Autowired
  private XmlRpcClientConfigImpl config;

  @Autowired
  private Configuration configuration;

  public void pingFeedburner() {
    try {
      config.setServerURL(new URL("http://ping.feedburner.com/"));

      XmlRpcClient client = new XmlRpcClient();

      client.setConfig(config);

      Object[] params = {"Linux.org.ru", configuration.getMainUrl()};

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

  public void setConfig(XmlRpcClientConfigImpl config) {
    this.config = config;
  }
}
