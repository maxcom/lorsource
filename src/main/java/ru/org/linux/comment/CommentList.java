/*
 * Copyright 1998-2013 Linux.org.ru
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
import com.google.common.collect.ImmutableMap;
import ru.org.linux.user.Profile;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentList implements Serializable {
  private final ImmutableList<Comment> comments;
  private final CommentNode root = new CommentNode();
  private final ImmutableMap<Integer, CommentNode> nodeIndex;

  private final long lastmod;

  public CommentList(List<Comment> comments, long lastmod) {
    this.lastmod = lastmod;

    this.comments = ImmutableList.copyOf(comments);

    Map<Integer, CommentNode> indexBuilder = new HashMap<>(this.comments.size());

    /* build tree */
    for (Comment comment : this.comments) {
      CommentNode node = new CommentNode(comment);

      indexBuilder.put(comment.getId(), node);

      if (comment.getReplyTo()==0) {
        root.addChild(node);
      } else {
        CommentNode parentNode = indexBuilder.get(comment.getReplyTo());
        if (parentNode!=null) {
          parentNode.addChild(node);
        } else {
          root.addChild(node);
        }
      }
    }

    nodeIndex = ImmutableMap.copyOf(indexBuilder);
  }

  @Nonnull
  public ImmutableList<Comment> getList() {
    return comments;
  }

  public CommentNode getRoot() {
    return root;
  }

  public CommentNode getNode(int msgid) {
    return nodeIndex.get(msgid);
  }

  private int getCommentPage(@Nonnull Comment comment, int messages, boolean reverse) {
    int index = comments.indexOf(comment);

    if (reverse) {
      return (comments.size()-index)/messages;
    } else {
      return index/messages;
    }
  }

  public int getCommentPage(@Nonnull Comment comment, @Nonnull Profile profile) {
    int messages = profile.getMessages();
    boolean reverse = profile.isShowNewFirst();

    return getCommentPage(comment, messages, reverse);
  }

  public long getLastmod() {
    return lastmod;
  }
}
