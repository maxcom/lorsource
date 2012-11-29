/*
 * Copyright 1998-2012 Linux.org.ru
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

import com.google.common.collect.ImmutableSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.org.linux.site.Template;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

public class CommentList implements Serializable {
  private static final Log logger = LogFactory.getLog(CommentList.class);

  private final List<Comment> comments = new ArrayList<Comment>(CommentFilter.COMMENTS_INITIAL_BUFSIZE);
  private final CommentNode root = new CommentNode();
  private final Map<Integer, CommentNode> treeHash = new HashMap<Integer, CommentNode>(CommentFilter.COMMENTS_INITIAL_BUFSIZE);

  private final long lastmod;

  public CommentList(List<Comment> comments, long lastmod) {
    this.lastmod = lastmod;
    this.comments.addAll(comments);
    logger.debug("Read list size = " +comments.size());
    buildTree();
  }

  @Nonnull
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

  public int getCommentPage(@Nonnull Comment comment, int messages, boolean reverse) {
    int index = comments.indexOf(comment);

    if (reverse) {
      return (comments.size()-index)/messages;
    } else {
      return index/messages;
    }
  }

  public int getCommentPage(@Nonnull Comment comment, @Nonnull Template tmpl) {
    int messages = tmpl.getProf().getMessages();
    boolean reverse = tmpl.getProf().isShowNewFirst();

    return getCommentPage(comment, messages, reverse);
  }

  @Nonnull
  public static Set<Integer> makeHideSet(UserDao userDao, CommentList comments, int filterChain, Set<Integer> ignoreList) throws SQLException, UserNotFoundException {
    if (filterChain == CommentFilter.FILTER_NONE) {
      return ImmutableSet.of();
    }

    Set<Integer> hideSet = new HashSet<Integer>();

    /* hide anonymous */
    if ((filterChain & CommentFilter.FILTER_ANONYMOUS) > 0) {
      comments.root.hideAnonymous(userDao, hideSet);
    }

    /* hide ignored */
    if ((filterChain & CommentFilter.FILTER_IGNORED) > 0) {
      if (ignoreList != null && !ignoreList.isEmpty()) {
        comments.root.hideIgnored(hideSet, ignoreList);
      }
    }

    return hideSet;
  }

  public long getLastmod() {
    return lastmod;
  }
}
