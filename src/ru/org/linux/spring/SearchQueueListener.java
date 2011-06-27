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

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import ru.org.linux.site.*;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Component;

@Component
public class SearchQueueListener {
  private static final Log logger = LogFactory.getLog(SearchQueueListener.class);
  
  private SolrServer solrServer;

  @Autowired
  @Required
  public void setSolrServer(SolrServer solrServer) {
    this.solrServer = solrServer;
  }

  public void handleMessage(SearchQueueSender.UpdateMessage msgUpdate) throws SQLException, MessageNotFoundException, IOException, SolrServerException {
    logger.info("Indexing "+msgUpdate.getMsgid());

    Connection db = LorDataSource.getConnection();

    try {
      reindexMessage(db, msgUpdate.getMsgid(), msgUpdate.isWithComments());
      solrServer.commit();
    } finally {
      JdbcUtils.closeConnection(db);
    }
  }

  private void reindexMessage(Connection db, int msgid, boolean withComments) throws IOException, SolrServerException, SQLException, MessageNotFoundException {
    Message msg = new Message(db, msgid);

    if (!msg.isDeleted()) {
      updateMessage(msg);
    } else {
      //logger.info("Deleting message "+msgid+" from solr");      
      solrServer.deleteById((Integer.toString(msg.getId())));
    }

    if (withComments) {
      CommentList commentList = CommentList.getCommentList(db, msg, true);

      if (!msg.isDeleted()) {
        reindexComments(db, msg, commentList);
      } else {
        List<String> msgids = Lists.transform(commentList.getList(), new Function<Comment, String>() {
          @Override
          public String apply(Comment comment) {
            return Integer.toString(comment.getId());
          }
        });

        if (!msgids.isEmpty()) {
          solrServer.deleteById(msgids);
        }
      }
    }
  }

  public void handleMessage(SearchQueueSender.UpdateComments msgUpdate) throws SQLException, MessageNotFoundException, IOException, SolrServerException {
    logger.info("Indexing "+msgUpdate.getMsgids());

    Connection db = LorDataSource.getConnection();
    PreparedStatement pst = null;

    try {
      pst = db.prepareStatement("SELECT message FROM msgbase WHERE id=?");

      for (Integer msgid : msgUpdate.getMsgids()) {
        Comment comment = new Comment(db, msgid);

        if (comment.isDeleted()) {
          logger.info("Deleting comment "+comment.getId()+" from solr");
          solrServer.deleteById(Integer.toString(comment.getId()));
        } else {
          // комментарии могут быть из разного топика в функция массового удаления
          // возможно для скорости нужен какой-то кеш топиков, т.к. чаще бывает что все
          // комментарии из одного топика
          Message topic = new Message(db, comment.getTopic());

          pst.setInt(1, comment.getId());
          ResultSet rs = pst.executeQuery();
          if (!rs.next()) {
            throw new RuntimeException("Can't load message text for " + comment.getId());
          }

          String message = rs.getString(1);

          rs.close();

          solrServer.add(processComment(topic, comment, message));
        }
      }

      solrServer.commit();
    } finally {
      JdbcUtils.closeStatement(pst);
      JdbcUtils.closeConnection(db);
    }
  }

  public void handleMessage(SearchQueueSender.UpdateMonth msgUpdate) throws SQLException, MessageNotFoundException, IOException, SolrServerException {
    int month = msgUpdate.getMonth();
    int year = msgUpdate.getYear();

    logger.info("Indexing month "+ year + '/' + month);

    Connection db = LorDataSource.getConnection();

    try {
      long startTime = System.nanoTime();

      Statement st = db.createStatement();

      ResultSet rs = st.executeQuery("SELECT id FROM topics WHERE postdate>='" + year + '-' + month + "-01'::timestamp AND (postdate<'" + year + '-' + month + "-01'::timestamp+'1 month'::interval)");

      while (rs.next()) {
        reindexMessage(db, rs.getInt(1), true);
      }

      solrServer.commit();

      long endTime = System.nanoTime();

      logger.info("Reindex month "+year+'/'+month+" done, "+(endTime-startTime)/1000000+" millis");
    } finally {
      JdbcUtils.closeConnection(db);
    }
  }

  private void updateMessage(Message topic) throws IOException, SolrServerException {
    SolrInputDocument doc = new SolrInputDocument();

    doc.addField("id", topic.getId());

    doc.addField("section_id", topic.getSectionId());
    doc.addField("user_id", topic.getUid());
    doc.addField("topic_user_id", topic.getUid());
    doc.addField("topic_id", topic.getMessageId());
    doc.addField("group_id", topic.getGroupId());

    doc.addField("title", topic.getTitle());
    doc.addField("message", topic.getMessage());
    Date postdate = topic.getPostdate();
    doc.addField("postdate", new Timestamp(postdate.getTime()));

    doc.addField("is_comment", false);

    solrServer.add(doc);
  }

  private void reindexComments(Connection db, Message topic, CommentList comments) throws IOException, SolrServerException, SQLException {
    Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
    List<String> delete = new ArrayList<String>();

    PreparedStatement pst = db.prepareStatement("SELECT message FROM msgbase WHERE id=?");

    try {
      for (Comment comment : comments.getList()) {
        if (comment.isDeleted()) {
          delete.add(Integer.toString(comment.getId()));
        }

        pst.setInt(1, comment.getId());
        ResultSet rs = pst.executeQuery();
        if (!rs.next()) {
          throw new RuntimeException("Can't load message text for "+comment.getId());
        }

        String message = rs.getString(1);

        rs.close();

        docs.add(processComment(topic, comment, message));
      }
    } finally {
      JdbcUtils.closeStatement(pst);
    }

    if (!docs.isEmpty()) {
      solrServer.add(docs);
    }
    if (!delete.isEmpty()) {
      //logger.info("Deleting comments: "+delete);
      solrServer.deleteById(delete);
    }
  }

  private static SolrInputDocument processComment(Message topic, Comment comment, String message) {
    SolrInputDocument doc = new SolrInputDocument();

    doc.addField("id", comment.getId());

    doc.addField("section_id", topic.getSectionId());
    doc.addField("user_id", comment.getUserid());
    doc.addField("topic_user_id", topic.getUid());
    doc.addField("topic_id", comment.getTopic());
    doc.addField("group_id", topic.getGroupId());
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

    return doc;
  }
}
