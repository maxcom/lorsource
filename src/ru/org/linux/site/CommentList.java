package ru.org.linux.site;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Logger;

import com.danga.MemCached.MemCachedClient;

import ru.org.linux.util.UtilException;

public class CommentList implements Serializable {
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  private final List<Comment> comments = new ArrayList<Comment>(CommentViewer.COMMENTS_INITIAL_BUFSIZE);
  private final CommentNode root = new CommentNode();
  private final Map<Integer, CommentNode> treeHash = new HashMap<Integer, CommentNode>(CommentViewer.COMMENTS_INITIAL_BUFSIZE);

  private final long lastmod;

  private CommentList(Connection db, int topicId, long lastmod, boolean deleted) throws SQLException {
    this.lastmod = lastmod;

    String delq = deleted ? "" : " AND NOT deleted ";

    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery(
        "SELECT " +
            "comments.title, topic, postdate, nick, score, max_score, comments.id as msgid, " +
            "replyto, photo, deleted, message " +
            "FROM comments,users,msgbase " +
            "WHERE comments.id=msgbase.id AND comments.userid=users.id " +
            "AND topic=" + topicId + ' ' + delq + " " +
            "ORDER BY msgid ASC"
    );

    while (rs.next()) {
      comments.add(new Comment(db, rs));
    }

    rs.close();

    logger.fine("Read list size = " +comments.size());

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

  public static CommentList getCommentList(Template tmpl, Connection db, Message topic, boolean showDeleted) throws SQLException {
    MemCachedClient mcc = MemCachedSettings.getClient();

    String shortCacheId = "commentList?msgid="+topic.getMessageId()+"&showDeleted="+showDeleted;

    String cacheId = MemCachedSettings.getId(tmpl, shortCacheId);

    CommentList res = (CommentList) mcc.get(cacheId);

    if (res==null || res.getLastModified()!=topic.getLastModified().getTime()) {
      res = new CommentList(db, topic.getMessageId(), topic.getLastModified().getTime(), showDeleted);
      mcc.set(cacheId, res);
    }

    return res;
  }
}
