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

package ru.org.linux.site;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;

public class LorSearchSource {
  private LorSearchSource(){
  }
  public static SolrServer getConnection(){
    try{
      InitialContext cxt = new InitialContext();
      String url = (String) cxt.lookup("java:/comp/env/solr/url");
      return new CommonsHttpSolrServer(url);
    } catch(MalformedURLException ex) {
      throw new RuntimeException("Connection to solr fail", ex);
    } catch (NamingException ex) {
      throw new RuntimeException("Connection to solr fail", ex);
    }
  }
}
