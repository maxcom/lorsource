package ru.org.linux.site;

import java.net.MalformedURLException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

import ru.org.linux.site.Comment;


public class LorSearchSource {
  private LorSearchSource(){
  }
  public static SolrServer getConnection(){
    SolrServer solrServer = null;
    try{
      solrServer = new CommonsHttpSolrServer("http://stress.vyborg.ru/solr");
    }catch(MalformedURLException ex){
      throw new RuntimeException(ex);
    }
    return solrServer;
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
      throw new RuntimeException(ex);
    }catch(java.io.IOException ex){
      throw new RuntimeException(ex);
    }
  }
}
