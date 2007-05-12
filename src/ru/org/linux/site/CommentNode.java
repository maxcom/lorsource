package ru.org.linux.site;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class CommentNode {
  private final LinkedList childs = new LinkedList();
  private Comment comment = null;

  public CommentNode() {
  }

  public CommentNode(Comment comment) {
    this.comment = comment;
  }

  public void addChild(CommentNode child) {
    childs.add(child);
  }

  public List getChilds() {
    return Collections.unmodifiableList(childs);
  }

  public int getMessageId() {
    return comment==null?0:comment.getMessageId();
  }

  public void hideAnonymous() {
    if (comment!=null) {
      if (comment.isAnonymous())
        hideNode();
    }

    if (comment==null || comment.isShowable()) {
      for (Iterator i=childs.iterator(); i.hasNext(); ) {
        CommentNode node = (CommentNode) i.next();

        node.hideAnonymous();
      }
    }
  }

  public void buildList(List list) {
    if (comment!=null) list.add(comment);

    for (Iterator i=childs.iterator(); i.hasNext(); ) {
      CommentNode node = (CommentNode) i.next();

      node.buildList(list);
    }
  }

  public void hideNode() {
    if (comment!=null)
      comment.setShow(false);

    for (Iterator i=childs.iterator(); i.hasNext(); ) {
      CommentNode node = (CommentNode) i.next();

      node.hideNode();
    }
  }
}