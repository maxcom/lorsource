/*
 * Copyright 1998-2014 Linux.org.ru
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

/*
 * Copyright (c) 2005-2006, Luke Plant
 * All rights reserved.
 * E-mail: <L.Plant.98@cantab.net>
 * Web: http://lukeplant.me.uk/
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *
 *      * The name of Luke Plant may not be used to endorse or promote
 *        products derived from this software without specific prior
 *        written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Rewrite with Java language and modified for lorsource by Ildar Hizbulin 2011
 * E-mail: <hizel@vyborg.ru>
 */

package ru.org.linux.util.bbcode.tags;

import com.google.common.collect.ImmutableSet;
import ru.org.linux.util.bbcode.ParserParameters;
import ru.org.linux.util.bbcode.nodes.Node;

import java.util.Set;

abstract public class Tag {
  final String name;
  private final ImmutableSet<String> allowedChildren;
  private final String implicitTag;
  boolean selfClosing = false;
  private final ImmutableSet<String> prohibitedElements;
  private boolean discardable = false;
  protected final ParserParameters parserParameters;

  public Tag(
          String name,
          ImmutableSet<String> allowedChildren,
          String implicitTag,
          ParserParameters parserParameters,
          ImmutableSet<String> prohibitedElements
  ) {
    this.name = name;
    this.implicitTag = implicitTag;
    this.allowedChildren = allowedChildren;
    this.parserParameters = parserParameters;
    this.prohibitedElements = prohibitedElements;
  }

  public Tag(String name, ImmutableSet<String> allowedChildren, String implicitTag, ParserParameters parserParameters) {
    this(name, allowedChildren, implicitTag, parserParameters, ImmutableSet.<String>of());
  }

  public void setSelfClosing(boolean selfClosing) {
    this.selfClosing = selfClosing;
  }

  public void setDiscardable(boolean discardable) {
    this.discardable = discardable;
  }

  abstract public String renderNodeXhtml(Node node);

  public String renderNodeBBCode(Node node) {
    StringBuilder opening = new StringBuilder(name);
    StringBuilder render = new StringBuilder();
    if (node.isParameter()) {
      opening.append('=');
      opening.append(node.getParameter());
    }
    if (selfClosing) {
      render.append('[')
              .append(opening)
              .append("/]");
    } else {
      render.append('[')
              .append(opening).append(']')
              .append(node.renderChildrenBBCode())
              .append("[/")
              .append(name)
              .append(']');
    }
    return render.toString();
  }

  public String renderOg(Node node) {
    return node.renderChildrenOg();
  }

  public Set<String> getAllowedChildren() {
    return allowedChildren;
  }

  public Set<String> getProhibitedElements() {
    return prohibitedElements;
  }

  public String getName() {
    return name;
  }

  public String getImplicitTag() {
    return implicitTag;
  }

  public boolean isSelfClosing() {
    return selfClosing;
  }

  public boolean isDiscardable() {
    return discardable;
  }
}
