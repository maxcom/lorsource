package ru.org.linux.site;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Logger;

import com.danga.MemCached.MemCachedClient;

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
            "comments.title, topic, postdate, users.id as userid, comments.id as msgid, " +
            "replyto, deleted, message, user_agents.name AS useragent, comments.postip " +
            "FROM comments " +
            "INNER JOIN users ON (users.id=comments.userid) " +
            "INNER JOIN msgbase ON (msgbase.id=comments.id) " + 
            "LEFT JOIN user_agents ON (user_agents.id=comments.ua_id) " +
            "WHERE topic=" + topicId + ' ' + delq + ' ' +
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

  public int getCommentPage(Comment comment, int messages) {
    int index = comments.indexOf(comment);

    return index / messages;
  }

  public int getCommentPage(Comment comment, Template tmpl)  {
    int messages = tmpl.getProf().getInt("messages");

    return getCommentPage(comment, messages);
  }

  public static CommentList getCommentList(Connection db, Message topic, boolean showDeleted) throws SQLException {
    MemCachedClient mcc = MemCachedSettings.getClient();

    String shortCacheId = "commentList?msgid="+topic.getMessageId()+"&showDeleted="+showDeleted;

    String cacheId = MemCachedSettings.getId(shortCacheId);

    CommentList res = (CommentList) mcc.get(cacheId);

    if (res==null || res.lastmod !=topic.getLastModified().getTime()) {
      res = new CommentList(db, topic.getMessageId(), topic.getLastModified().getTime(), showDeleted);
      mcc.set(cacheId, res);
    }

    return res;
  }

  public static Set<Integer> makeHideSet(Connection db, CommentList comments, int filterChain, String nick) throws SQLException, UserNotFoundException {
    if (filterChain == CommentViewer.FILTER_NONE) {
      return null;
    }

    Set<Integer> hideSet = new HashSet<Integer>();

    /* hide anonymous */
    if ((filterChain & CommentViewer.FILTER_ANONYMOUS) > 0) {
      comments.root.hideAnonymous(db, hideSet);
    }

    /* hide ignored */
    if ((filterChain & CommentViewer.FILTER_IGNORED) > 0 && nick != null && !"".equals(nick)) {
      Map<Integer, String> ignoreList = IgnoreList.getIgnoreListHash(db, nick);
      if (ignoreList != null && !ignoreList.isEmpty()) {
        comments.root.hideIgnored(hideSet, ignoreList);
      }
    }
    
    return hideSet;
  }
}
