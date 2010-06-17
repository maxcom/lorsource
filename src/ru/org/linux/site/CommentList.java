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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.org.linux.spring.commons.CacheProvider;
import ru.org.linux.util.UtilException;

public class CommentList implements Serializable {
  private static final Log logger = LogFactory.getLog(CommentList.class);

  private final List<Comment> comments = new ArrayList<Comment>(CommentFilter.COMMENTS_INITIAL_BUFSIZE);
  private final CommentNode root = new CommentNode();
  private final Map<Integer, CommentNode> treeHash = new HashMap<Integer, CommentNode>(CommentFilter.COMMENTS_INITIAL_BUFSIZE);

  private final long lastmod;

  private CommentList(Connection db, int topicId, long lastmod, boolean deleted) throws SQLException {
    this.lastmod = lastmod;

    String delq = deleted ? "" : " AND NOT deleted ";

    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery(
        "SELECT " +
            "comments.title, topic, postdate, userid, comments.id as msgid, " +
            "replyto, deleted, message, user_agents.name AS useragent, comments.postip, bbcode " +
            "FROM comments " +
            "INNER JOIN msgbase ON (msgbase.id=comments.id) " + 
            "LEFT JOIN user_agents ON (user_agents.id=comments.ua_id) " +
            "WHERE topic=" + topicId + ' ' + delq + ' ' +
            "ORDER BY msgid ASC"
    );

    while (rs.next()) {
      comments.add(new Comment(db, rs));
    }

    rs.close();

    logger.debug("Read list size = " +comments.size());

    buildTree();
  }

  public List<Comment> getList() {
    return Collections.unmodifiableList(comments);
  }

  private void buildTree() {
    /* build tree */
    for (Comment comment : comments) {
      CommentNode node = new CommentNode(comment);

      treeHash.put(comment.getMessageId(), node);

      if (comment.getReplyTo()==0) {
        root.addChild(node);
      } else {
        CommentNode parentNode = treeHash.get(comment.getReplyTo());
        if (parentNode!=null) {
          parentNode.addChild(node);
        } else {
          root.addChild(node);
        }
      }
    }
  }

  public CommentNode getRoot() {
    return root;
  }

  public CommentNode getNode(int msgid) {
    return treeHash.get(msgid);
  }

  public long getLastModified() {
    return lastmod;
  }

  public int getCommentPage(Comment comment, int messages, boolean reverse) {
    int index = comments.indexOf(comment);

    if (reverse) {
      return (comments.size()-index)/messages;
    } else {
      return index/messages;
    }
  }

  public int getCommentPage(Comment comment, Template tmpl) throws UtilException {
    int messages = tmpl.getProf().getInt("messages");
    boolean reverse = tmpl.getProf().getBoolean("newfirst");

    return getCommentPage(comment, messages, reverse);
  }

  public static CommentList getCommentList(Connection db, Message topic, boolean showDeleted) throws SQLException {
    CacheProvider mcc = MemCachedSettings.getCache();

    String cacheId = "commentList?msgid="+topic.getMessageId()+"&showDeleted="+showDeleted;

    CommentList res = (CommentList) mcc.getFromCache(cacheId);

    if (res==null || res.lastmod !=topic.getLastModified().getTime()) {
      res = new CommentList(db, topic.getMessageId(), topic.getLastModified().getTime(), showDeleted);
      mcc.storeToCache(cacheId, res);
    }

    return res;
  }

  public static Set<Integer> makeHideSet(Connection db, CommentList comments, int filterChain, Map<Integer, String> ignoreList) throws SQLException, UserNotFoundException {
    if (filterChain == CommentFilter.FILTER_NONE) {
      return null;
    }

    Set<Integer> hideSet = new HashSet<Integer>();

    /* hide anonymous */
    if ((filterChain & CommentFilter.FILTER_ANONYMOUS) > 0) {
      comments.root.hideAnonymous(db, hideSet);
    }

    /* hide ignored */
    if ((filterChain & CommentFilter.FILTER_IGNORED) > 0) {
      if (ignoreList != null && !ignoreList.isEmpty()) {
        comments.root.hideIgnored(hideSet, ignoreList);
      }
    }
    
    return hideSet;
  }
}
