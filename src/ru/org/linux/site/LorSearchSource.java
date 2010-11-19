package ru.org.linux.site;

import java.net.MalformedURLException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import org.apache.commons.logging.Log;                                                                                                                                           
import org.apache.commons.logging.LogFactory; 

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

import ru.org.linux.site.Comment;
import ru.org.linux.site.Message;


public class LorSearchSource {
  private static final Log logger = LogFactory.getLog(LorSearchSource.class);
  private LorSearchSource(){
  }
  public static SolrServer getConnection(){
    SolrServer solrServer = null;
    try{
      solrServer = new CommonsHttpSolrServer("http://stress.vyborg.ru/solr");
    }catch(MalformedURLException ex){
      logger.error("Connection to solr fail:"+ex.toString())
      throw new RuntimeException(ex);
    }
    return solrServer;
  }

  public static void updateMessage(SolrServer server, Message topic, int msgid){
    UpdateRequest updateRequest = new UpdateRequest();
    updateRequest.setAction(AbstractUpdateRequest.ACTION.COMMIT, false, false);

    SolrInputDocument doc = new SolrInputDocument();

    doc.addField("id", msgid);

    doc.addField("section_id", msgid );
    doc.addField("user_id", topic.getUid() );
    doc.addField("topic_id", topic.getMessageId() );

    doc.addField("title", topic.getTitle() );
    doc.addField("message", topic.getMessage() );
    Date postdate = topic.getPostdate();
    doc.addField("postdate", new Timestamp(postdate.getTime()));

    doc.addField("is_comment", false);

    updateRequest.add(doc);
    try{
      updateRequest.process(server);
    }catch(SolrServerException ex){
      logger.error("Update comment solr fail:"+ex.toString())
    }catch(java.io.IOException ex){
      logger.error("Update comment solr fail:"+ex.toString())
    }
  }

  public static void updateComment(SolrServer server, Comment comment, int msgid, int sectionId, String message){
    UpdateRequest updateRequest = new UpdateRequest();
    updateRequest.setAction(AbstractUpdateRequest.ACTION.COMMIT, false, false);

    SolrInputDocument doc = new SolrInputDocument();

    doc.addField("id", msgid);

    doc.addField("section_id", sectionId );
    doc.addField("user_id", comment.getUserid() );
    doc.addField("topic_id", comment.getTopic() );

    doc.addField("title", comment.getTitle() );
    doc.addField("message", message);
    Date postdate = comment.getPostdate();
    doc.addField("postdate", new Timestamp(postdate.getTime()));

    doc.addField("is_comment", true);

    updateRequest.add(doc);
    try{
      updateRequest.process(server);
    }catch(SolrServerException ex){
      logger.error("Update topic solr fail:"+ex.toString())
    }catch(java.io.IOException ex){
      logger.error("Update topic solr fail:"+ex.toString())
    }
  }
}
