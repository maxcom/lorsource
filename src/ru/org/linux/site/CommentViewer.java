package ru.org.linux.site;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import ru.org.linux.util.UtilException;

public class CommentViewer {
  private static final int COMMENTS_INITIAL_BUFSIZE = 50;

  private final Template tmpl;
  private final ResultSet rs;
  private final Connection db;
  private final String urladd;
  private String mainUrl;

  public CommentViewer(Template t, ResultSet r, Connection conn, String u) {
    tmpl=t;
    rs=r;
    db=conn;
    urladd=u;
    mainUrl ="";
  }

  public void setMainUrl(String url) {
    mainUrl =url;
  }

  private void showCommentList(StringBuffer buf, List comments, boolean reverse)
      throws IOException, SQLException, UtilException {
    if (reverse) {
      Collections.reverse(comments);
    }

    for (Iterator i=comments.iterator(); i.hasNext(); ) {
      Comment comment = (Comment) i.next();

      if (comment.isShowable()) {
        buf.append(comment.printMessage(tmpl, db, true, false, urladd, mainUrl));
      }
    }
  }

  public String showAll() throws IOException, SQLException, UtilException {
    StringBuffer buf=new StringBuffer();
    List comments = new ArrayList(COMMENTS_INITIAL_BUFSIZE);

    while (rs.next()) {
      comments.add(new Comment(rs));
    }

    showCommentList(buf, comments, false);

    return buf.toString();
  }

  public String showThreaded() throws IOException, SQLException, UtilException {
    StringBuffer buf=new StringBuffer();
    List comments = new ArrayList(COMMENTS_INITIAL_BUFSIZE);
    CommentNode root = new CommentNode();
    Map treeHash = new HashMap(COMMENTS_INITIAL_BUFSIZE);

    /* build tree */
    while (rs.next()) {
      Comment comment = new Comment(rs);
      comments.add(comment);

      CommentNode node = new CommentNode(comment);

      treeHash.put(new Integer(comment.getMessageId()), node);

      if (comment.getReplyTo()==0) {
        root.addChild(node);
      } else {
        CommentNode parentNode = (CommentNode) treeHash.get(new Integer(comment.getReplyTo()));
        if (parentNode!=null) {
          parentNode.addChild(node);
        } else {
          root.addChild(node);
        }
      }
    }

    /* hide anonymous */
    root.hideAnonymous();

    /* display comments */
    showCommentList(buf, comments, tmpl.getProf().getBoolean("newfirst"));

    return buf.toString();
  }

  public String showSubtree(int parentId) throws IOException, SQLException, UtilException, MessageNotFoundException {
    StringBuffer buf=new StringBuffer();
    CommentNode root = new CommentNode();
    Map treeHash = new HashMap(COMMENTS_INITIAL_BUFSIZE);

    /* build tree */
    while (rs.next()) {
      Comment comment = new Comment(rs);

      CommentNode node = new CommentNode(comment);

      treeHash.put(new Integer(comment.getMessageId()), node);

      if (comment.getReplyTo()==0) {
        root.addChild(node);
      } else {
        CommentNode parentNode = (CommentNode) treeHash.get(new Integer(comment.getReplyTo()));
        if (parentNode!=null) {
          parentNode.addChild(node);
        } else {
          root.addChild(node);
        }
      }
    }

    CommentNode parentNode = (CommentNode) treeHash.get(new Integer(parentId));

    if (parentNode==null) {
      throw new MessageNotFoundException(parentId);
    }

    List parentList = new ArrayList();
    parentNode.buildList(parentList);

    /* display comments */
    showCommentList(buf, parentList, false);

    return buf.toString();
  }
}
