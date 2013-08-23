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

package ru.org.linux.search;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

public class SearchHttpClientFactoryBean implements FactoryBean<HttpClient>, InitializingBean {
  private HttpClient httpClient;
  private HttpConnectionManager connectionManager;
  private String username;
  private String password;
  private String host;
  private int port;

  @Override
  public HttpClient getObject() throws Exception {
    return httpClient;
  }

  @Override
  public Class<?> getObjectType() {
    return HttpClient.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Required
  public void setConnectionManager(HttpConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  @Required
  public void setUsername(String username) {
    this.username = username;
  }

  @Required
  public void setPassword(String password) {
    this.password = password;
  }

  @Required
  public void setHost(String host) {
    this.host = host;
  }

  @Required
  public void setPort(int port) {
    this.port = port;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    httpClient = new HttpClient(connectionManager);
    Credentials credentials = new UsernamePasswordCredentials(username, password);
    httpClient.getState().setCredentials(new AuthScope(host, port), credentials);
    httpClient.getParams().setAuthenticationPreemptive(true);
  }
}
