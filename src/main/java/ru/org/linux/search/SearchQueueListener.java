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

package ru.org.linux.search;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Component;
import ru.org.linux.site.Comment;
import ru.org.linux.site.CommentList;
import ru.org.linux.site.Topic;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.spring.dao.CommentDao;
import ru.org.linux.spring.dao.TopicDao;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Component
public class SearchQueueListener {
  private static final Log logger = LogFactory.getLog(SearchQueueListener.class);
  
  private SolrServer solrServer;
  private TopicDao messageDao;
  private CommentDao commentDao;

  @Autowired
  @Required
  public void setSolrServer(SolrServer solrServer) {
    this.solrServer = solrServer;
  }

  @Autowired
  public void setMessageDao(TopicDao messageDao) {
    this.messageDao = messageDao;
  }

  @Autowired
  public void setCommentDao(CommentDao commentDao) {
    this.commentDao = commentDao;
  }

  public void handleMessage(SearchQueueSender.UpdateMessage msgUpdate) throws MessageNotFoundException, IOException, SolrServerException {
    logger.info("Indexing "+msgUpdate.getMsgid());

    reindexMessage(msgUpdate.getMsgid(), msgUpdate.isWithComments());
    solrServer.commit();
  }

  private void reindexMessage(int msgid, boolean withComments) throws IOException, SolrServerException,  MessageNotFoundException {
    Topic msg = messageDao.getById(msgid);

    if (!msg.isDeleted()) {
      updateMessage(msg);
    } else {
      //logger.info("Deleting message "+msgid+" from solr");      
      solrServer.deleteById((Integer.toString(msg.getId())));
    }

    if (withComments) {
      CommentList commentList = commentDao.getCommentList(msg, true);

      if (!msg.isDeleted()) {
        reindexComments(msg, commentList);
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

  public void handleMessage(SearchQueueSender.UpdateComments msgUpdate) throws MessageNotFoundException, IOException, SolrServerException {
    logger.info("Indexing comments "+msgUpdate.getMsgids());

    UpdateRequest rq = new UpdateRequest();

    rq.setCommitWithin(10000);

    boolean delete = false;

    for (Integer msgid : msgUpdate.getMsgids()) {
      Comment comment = commentDao.getById(msgid);

      if (comment.isDeleted()) {
        logger.info("Deleting comment "+comment.getId()+" from solr");
        solrServer.deleteById(Integer.toString(comment.getId()));
        delete = true;
      } else {
        // комментарии могут быть из разного топика в функция массового удаления
        // возможно для скорости нужен какой-то кеш топиков, т.к. чаще бывает что все
        // комментарии из одного топика
        Topic topic = messageDao.getById(comment.getTopicId());
        String message = commentDao.getMessage(comment);
        rq.add(processComment(topic, comment, message));
      }
    }

    if (rq.getDocuments()!=null && !rq.getDocuments().isEmpty())  {
      rq.process(solrServer);
    }

    if (delete) {
      solrServer.commit();
    }
  }

  public void handleMessage(SearchQueueSender.UpdateMonth msgUpdate) throws MessageNotFoundException, IOException, SolrServerException {
    int month = msgUpdate.getMonth();
    int year = msgUpdate.getYear();

    logger.info("Indexing month "+ year + '/' + month);
    long startTime = System.nanoTime();

    List<Integer> topicIds = messageDao.getMessageForMonth(year, month);
    for(int topicId : topicIds) {
      reindexMessage(topicId, true);
    }

    solrServer.commit();
    long endTime = System.nanoTime();
    logger.info("Reindex month "+year+'/'+month+" done, "+(endTime-startTime)/1000000+" millis");
  }

  private void updateMessage(Topic topic) throws IOException, SolrServerException {
    SolrInputDocument doc = new SolrInputDocument();

    doc.addField("id", topic.getId());

    doc.addField("section_id", topic.getSectionId());
    doc.addField("user_id", topic.getUid());
    doc.addField("topic_user_id", topic.getUid());
    doc.addField("topic_id", topic.getMessageId());
    doc.addField("group_id", topic.getGroupId());

    doc.addField("title", StringEscapeUtils.unescapeHtml(topic.getTitle()));
    doc.addField("topic_title", topic.getTitle());
    doc.addField("message", topic.getMessage());
    Date postdate = topic.getPostdate();
    doc.addField("postdate", new Timestamp(postdate.getTime()));

    doc.addField("is_comment", false);

    solrServer.add(doc);
  }

  private void reindexComments(Topic topic, CommentList comments) throws IOException, SolrServerException {
    Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
    List<String> delete = new ArrayList<String>();

    for (Comment comment : comments.getList()) {
      if (comment.isDeleted()) {
        delete.add(Integer.toString(comment.getId()));
      }
      String message = commentDao.getMessage(comment);
      docs.add(processComment(topic, comment, message));
    }

    if (!docs.isEmpty()) {
      solrServer.add(docs);
    }
    if (!delete.isEmpty()) {
      //logger.info("Deleting comments: "+delete);
      solrServer.deleteById(delete);
    }
  }

  private static SolrInputDocument processComment(Topic topic, Comment comment, String message) {
    SolrInputDocument doc = new SolrInputDocument();

    doc.addField("id", comment.getId());

    doc.addField("section_id", topic.getSectionId());
    doc.addField("user_id", comment.getUserid());
    doc.addField("topic_user_id", topic.getUid());
    doc.addField("topic_id", comment.getTopicId());
    doc.addField("group_id", topic.getGroupId());
    String topicTitle = topic.getTitle();
    doc.addField("topic_title", StringEscapeUtils.unescapeHtml(topicTitle));
    
    String commentTitle = comment.getTitle();

    if (commentTitle != null &&
        !commentTitle.isEmpty() &&
        !commentTitle.equals(topicTitle) &&
        !commentTitle.startsWith("Re:")) {
      doc.addField("title", StringEscapeUtils.unescapeHtml(commentTitle));
    }

    doc.addField("message", message);
    Date postdate = comment.getPostdate();
    doc.addField("postdate", new Timestamp(postdate.getTime()));

    doc.addField("is_comment", true);

    return doc;
  }
}
