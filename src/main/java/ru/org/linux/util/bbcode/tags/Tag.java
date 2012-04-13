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

import org.apache.commons.lang.NotImplementedException;
import ru.org.linux.util.bbcode.ParserParameters;
import ru.org.linux.util.bbcode.nodes.Node;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 6/29/11
 * Time: 11:20 PM
 */
public class Tag {
  final String name;
  private final Set<String> allowedChildren;
  private final String implicitTag;
  boolean selfClosing = false;
  private Set<String> prohibitedElements;
  private boolean discardable = false;
  protected final ParserParameters parserParameters;

  public Tag(String name, Set<String> allowedChildren, String implicitTag, ParserParameters parserParameters) {
    this.name = name;
    this.implicitTag = implicitTag;
    this.allowedChildren = allowedChildren;
    this.parserParameters = parserParameters;
  }

  public void setProhibitedElements(Set<String> prohibitedElements) {
    this.prohibitedElements = prohibitedElements;
  }

  public void setSelfClosing(boolean selfClosing) {
    this.selfClosing = selfClosing;
  }

  public void setDiscardable(boolean discardable) {
    this.discardable = discardable;
  }

  public String renderNodeOg(Node node) {
    StringBuilder ret = new StringBuilder();
    ret.append(node.renderChildrenOg());
    return ret.toString();
  }

  public String renderNodeXhtml(Node node) {
    throw new NotImplementedException();
  }

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
