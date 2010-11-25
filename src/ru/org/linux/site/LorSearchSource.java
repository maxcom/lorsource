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

  public static void delete(SolrServer server, int msgid) throws IOException, SolrServerException {
    server.deleteById((Integer.toString(msgid)));
    server.commit();
  }
}
