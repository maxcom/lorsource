/*
 * Copyright 1998-2021 Linux.org.ru
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

package ru.org.linux.comment;

import com.google.common.collect.ImmutableList;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;

import java.io.Serializable;
import java.util.Set;
import java.util.function.Consumer;

public class CommentNode implements Serializable {
  private final ImmutableList<CommentNode> childs;
  private final Comment comment;

  public CommentNode(Comment comment, Iterable<CommentNode> childs) {
    this.comment = comment;
    this.childs = ImmutableList.copyOf(childs);
  }

  public ImmutableList<CommentNode> childs() {
    return childs;
  }

  public boolean hasAnswers() {
    return !childs.isEmpty();
  }

  public void hideAnonymous(UserDao userDao, Set<Integer> hideSet) throws UserNotFoundException {
    if (comment!=null) {
      User commentAuthor = userDao.getUserCached(comment.getUserid());

      if (commentAuthor.isAnonymousScore()) {
        hideNode(hideSet);
      }
    }

    if (comment==null || !hideSet.contains(comment.getId())) {
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

    if (comment==null || !hideSet.contains(comment.getId())) {
      for (CommentNode child : childs) {
        child.hideIgnored(hideSet, ignoreList);
      }
    }
  }

  public void foreach(Consumer<Comment> consumer) {
    if (comment!=null) {
      consumer.accept(comment);
    }

    for (CommentNode child : childs) {
      child.foreach(consumer);
    }
  }

  private void hideNode(Set<Integer> hideSet) {
    foreach(c -> hideSet.add(c.getId()));
  }

  public Comment getComment() {
    return comment;
  }
}
