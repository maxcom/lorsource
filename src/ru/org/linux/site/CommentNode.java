package ru.org.linux.site;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CommentNode implements Serializable {
  private final LinkedList<CommentNode> childs = new LinkedList<CommentNode>();
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

  public void hideAnonymous(Set<Integer> hideSet) {
    if (comment!=null) {
      if (comment.isAnonymous())
        hideNode(hideSet);
    }

    if (comment==null || !hideSet.contains(comment.getMessageId())) {
      for (CommentNode child : childs) {
        child.hideAnonymous(hideSet);
      }
    }
  }

  public void buildList(List<Comment> list) {
    if (comment!=null) list.add(comment);

    for (CommentNode child : childs) {
      child.buildList(list);
    }
  }

  public void hideNode(Set<Integer> hideSet) {
    if (comment!=null) {
      hideSet.add(comment.getMessageId());
    }

    for (CommentNode child : childs) {
      child.hideNode(hideSet);
    }
  }
}