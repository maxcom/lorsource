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

import ru.org.linux.dto.CommentDto;
import ru.org.linux.exception.MessageNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class CommentFilter {
  public static final int COMMENTS_INITIAL_BUFSIZE = 50;

  public static final int FILTER_NONE = 0;
  public static final int FILTER_ANONYMOUS = 1;
  public static final int FILTER_IGNORED = 2;

  private final CommentList comments;

  public CommentFilter(CommentList comments) {
    this.comments = comments;
  }

  private static List<CommentDto> getCommentList(List<CommentDto> commentDtos, boolean reverse, int offset, int limit, Set<Integer> hideSet) {
    List<CommentDto> out = new ArrayList<CommentDto>();

    for (ListIterator<CommentDto> i = commentDtos.listIterator(reverse? commentDtos.size():0); reverse?i.hasPrevious():i.hasNext();) {
      int index = reverse?(commentDtos.size()-i.previousIndex()):i.nextIndex();

      CommentDto commentDto = reverse?i.previous():i.next();

      if (index<offset || (limit!=0 && index>=offset+limit)) {
        continue;
      }

      if (hideSet==null || !hideSet.contains(commentDto.getMessageId())) {
        out.add(commentDto);
      }
    }

    return out;
  }

  public List<CommentDto> getComments(boolean reverse, int offset, int limit, Set<Integer> hideSet) {
    return getCommentList(comments.getList(), reverse, offset, limit,  hideSet);
  }

  public List<CommentDto> getCommentsSubtree(int parentId) throws MessageNotFoundException {
    CommentNode parentNode = comments.getNode(parentId);

    if (parentNode==null) {
      throw new MessageNotFoundException(parentId);
    }

    List<CommentDto> parentList = new ArrayList<CommentDto>();
    parentNode.buildList(parentList);

    /* display comments */
    return getCommentList(parentList, false, 0, 0, null);
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
