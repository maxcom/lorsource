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

import ru.org.linux.dao.UserDao;
import ru.org.linux.dto.CommentDto;
import ru.org.linux.dto.UserDto;
import ru.org.linux.exception.UserNotFoundException;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

public class CommentNode implements Serializable {
  private final LinkedList<CommentNode> childs = new LinkedList<CommentNode>();
  private CommentDto comment = null;

  public CommentNode() {
  }

  public CommentNode(CommentDto commentDto) {
    this.comment = commentDto;
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

  public void hideAnonymous(UserDao userDao, Set<Integer> hideSet) throws SQLException, UserNotFoundException {
    if (comment!=null) {
      UserDto commentAuthor = userDao.getUserCached(comment.getUserid());

      if (commentAuthor.isAnonymousScore()) {
        hideNode(hideSet);
      }
    }

    if (comment==null || !hideSet.contains(comment.getMessageId())) {
      for (CommentNode child : childs) {
        child.hideAnonymous(userDao, hideSet);
      }
    }
  }

  public void hideIgnored(Set<Integer> hideSet, Set<Integer> ignoreList) {
    if (comment != null) {
      if (comment.isIgnored(ignoreList)) {
        hideNode(hideSet);
      }
    }

    if (comment==null || !hideSet.contains(comment.getMessageId())) {
      for (CommentNode child : childs) {
        child.hideIgnored(hideSet, ignoreList);
      }
    }
  }

  public void buildList(List<CommentDto> list) {
    if (comment!=null) {
      list.add(comment);
    }

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

  public CommentDto getComment() {
    return comment;
  }
}
