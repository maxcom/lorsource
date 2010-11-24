package ru.org.linux.site;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Timestamp;
import java.lang.Integer;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;

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

  public static void updateMessage(SolrServer server, Message topic, int msgid) throws IOException, SolrServerException {
    UpdateRequest updateRequest = new UpdateRequest();
    updateRequest.setAction(AbstractUpdateRequest.ACTION.COMMIT, false, false);

    SolrInputDocument doc = new SolrInputDocument();

    doc.addField("id", msgid);

    doc.addField("section_id", msgid);
    doc.addField("user_id", topic.getUid());
    doc.addField("topic_id", topic.getMessageId());

    doc.addField("title", topic.getTitle());
    doc.addField("message", topic.getMessage());
    Date postdate = topic.getPostdate();
    doc.addField("postdate", new Timestamp(postdate.getTime()));

    doc.addField("is_comment", false);

    updateRequest.add(doc);

    updateRequest.process(server);
  }

  public static void updateComment(SolrServer server, Comment comment, Message topic, int msgid, String message) throws IOException, SolrServerException {
    UpdateRequest updateRequest = new UpdateRequest();
    updateRequest.setAction(AbstractUpdateRequest.ACTION.COMMIT, false, false);

    SolrInputDocument doc = new SolrInputDocument();

    doc.addField("id", msgid);

    doc.addField("section_id", topic.getSectionId());
    doc.addField("user_id", comment.getUserid());
    doc.addField("topic_id", comment.getTopic());
    String commentTitle = comment.getTitle();

    if (commentTitle == null || commentTitle.isEmpty()) {
      doc.addField("title", topic.getTitle());
    } else {
      doc.addField("title", commentTitle);
    }

    doc.addField("message", message);
    Date postdate = comment.getPostdate();
    doc.addField("postdate", new Timestamp(postdate.getTime()));

    doc.addField("is_comment", true);

    updateRequest.add(doc);

    updateRequest.process(server);
  }
  public static void delete(SolrServer server, int msgid) throws IOException, SolrServerException {
    server.deleteById((Integer.toString(msgid)));
    server.commit();
  }
  public static void delete(SolrServer server, List<String> msgids) throws IOException, SolrServerException {
    server.deleteById(msgids);
    server.commit();
  }
  
}
