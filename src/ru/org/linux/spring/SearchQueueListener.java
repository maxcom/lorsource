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
import com.sun.istack.internal.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
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
      Message msg = new Message(db, msgUpdate.getMsgid());

      if (!msg.isDeleted()) {
        updateMessage(msg);
      } else {
        LorSearchSource.delete(solrServer, msg.getId());
      }
      
      if (msgUpdate.isWithComments()) {
        CommentList commentList = CommentList.getCommentList(db, msg, false);

        if (!msg.isDeleted()) {
          reindexComments(db, msg, commentList);
        } else {
          List<String> msgids = Lists.transform(commentList.getList(), new Function<Comment, String>() {
            @Override
            public String apply(@Nullable Comment comment) {
              return Integer.toString(comment.getId());
            }
          });

          delete(msgids);
        }
      }
    } finally {
      JdbcUtils.closeConnection(db);
    }
  }

  public void handleMessage(SearchQueueSender.UpdateComment msgUpdate) throws SQLException, MessageNotFoundException, IOException, SolrServerException {
    logger.info("Indexing "+msgUpdate.getMsgid());

    Connection db = LorDataSource.getConnection();
    PreparedStatement pst = null;

    try {
      Comment comment = new Comment(db, msgUpdate.getMsgid());
      Message topic = new Message(db, comment.getTopic());
      pst = db.prepareStatement("SELECT message FROM msgbase WHERE id=?");
      pst.setInt(1, comment.getId());
      ResultSet rs = pst.executeQuery();
      if (!rs.next()) {
        throw new RuntimeException("Can't load message text for "+comment.getId());
      }

      String message = rs.getString(1);

      updateComment(comment, topic, comment.getId(), message);
    } finally {
      JdbcUtils.closeStatement(pst);
      JdbcUtils.closeConnection(db);
    }
  }

  private void updateMessage(Message topic) throws IOException, SolrServerException {
    UpdateRequest updateRequest = new UpdateRequest();
    updateRequest.setAction(AbstractUpdateRequest.ACTION.COMMIT, false, false);

    SolrInputDocument doc = new SolrInputDocument();

    doc.addField("id", topic.getId());

    doc.addField("section_id", topic.getId());
    doc.addField("user_id", topic.getUid());
    doc.addField("topic_id", topic.getMessageId());

    doc.addField("title", topic.getTitle());
    doc.addField("message", topic.getMessage());
    Date postdate = topic.getPostdate();
    doc.addField("postdate", new Timestamp(postdate.getTime()));

    doc.addField("is_comment", false);

    updateRequest.add(doc);

    updateRequest.process(solrServer);
  }

  private void reindexComments(Connection db, Message topic, CommentList comments) throws IOException, SolrServerException, SQLException {
    Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
    UpdateRequest updateRequest = new UpdateRequest();
    updateRequest.setAction(AbstractUpdateRequest.ACTION.COMMIT, false, false);

    PreparedStatement pst = db.prepareStatement("SELECT message FROM msgbase WHERE id=?");

    try {
      for (Comment comment : comments.getList()) {
        pst.setInt(1, comment.getId());
        ResultSet rs = pst.executeQuery();
        if (!rs.next()) {
          throw new RuntimeException("Can't load message text for "+comment.getId());
        }

        String message = rs.getString(1);

        rs.close();

        updateComment(comment, topic, comment.getId(), message);
      }
    } finally {
      JdbcUtils.closeStatement(pst);
    }

    updateRequest.add(docs);
    updateRequest.process(solrServer);
  }

  private void updateComment(Comment comment, Message topic, int msgid, String message) throws IOException, SolrServerException {
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

    updateRequest.process(solrServer);
  }

  private void delete(List<String> msgids) throws IOException, SolrServerException {
    solrServer.deleteById(msgids);
    solrServer.commit();
  }
}
