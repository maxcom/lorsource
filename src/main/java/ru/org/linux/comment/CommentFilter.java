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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import ru.org.linux.site.*;

import javax.annotation.Nonnull;

public class CommentFilter {
  public static final int COMMENTS_INITIAL_BUFSIZE = 50;

  public static final int FILTER_NONE = 0;
  public static final int FILTER_ANONYMOUS = 1;
  public static final int FILTER_IGNORED = 2;

  private final CommentList comments;

  public CommentFilter(CommentList comments) {
    this.comments = comments;
  }

  private static List<Comment> getCommentList(
          @Nonnull List<Comment> comments,
          boolean reverse,
          int offset,
          int limit,
          @Nonnull Set<Integer> hideSet) {
    List<Comment> out = new ArrayList<Comment>();

    for (ListIterator<Comment> i = comments.listIterator(reverse?comments.size():0); reverse?i.hasPrevious():i.hasNext();) {
      int index = reverse?(comments.size()-i.previousIndex()):i.nextIndex();

      Comment comment = reverse?i.previous():i.next();

      if (index<offset || (limit!=0 && index>=offset+limit)) {
        continue;
      }

      if (!hideSet.contains(comment.getMessageId())) {
        out.add(comment);
      }
    }

    return out;
  }

  public List<Comment> getComments(boolean reverse, int offset, int limit, @Nonnull Set<Integer> hideSet) {
    return getCommentList(comments.getList(), reverse, offset, limit, hideSet);
  }

  public List<Comment> getCommentsSubtree(int parentId) throws MessageNotFoundException {
    CommentNode parentNode = comments.getNode(parentId);

    if (parentNode==null) {
      throw new MessageNotFoundException(parentId);
    }

    List<Comment> parentList = new ArrayList<Comment>();
    parentNode.buildList(parentList);

    /* display comments */
    return getCommentList(parentList, false, 0, 0, ImmutableSet.<Integer>of());
  }

  public static int parseFilterChain(String filter) {
    if ("list".equals(filter)) {
      return FILTER_IGNORED;
    }

    if ("anonymous".equals(filter)) {
      return FILTER_ANONYMOUS;
    }

    return FILTER_NONE;
  }

  public static String toString(int filterMode) {
    switch (filterMode) {
      case FILTER_NONE: return "show";
      case FILTER_ANONYMOUS: return "anonymous";
      case FILTER_IGNORED: return "list";
      case FILTER_IGNORED+FILTER_ANONYMOUS: return "listanon";
      default: return "show";
    }
  }
}
