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

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import java.util.LinkedList;

public class CommentNodeBuilder {
  private final LinkedList<CommentNodeBuilder> childs = new LinkedList<>();
  private final Comment comment;

  public CommentNodeBuilder() {
    comment = null;
  }

  public CommentNodeBuilder(Comment comment) {
    this.comment = comment;
  }

  public void addChild(CommentNodeBuilder child) {
    childs.add(child);
  }

  public CommentNode build() {
    return new CommentNode(comment,
            Lists.transform(childs, new Function<CommentNodeBuilder, CommentNode>() {
              @Override
              public CommentNode apply(CommentNodeBuilder input) {
                return input.build();
              }
            }));
  }
}
