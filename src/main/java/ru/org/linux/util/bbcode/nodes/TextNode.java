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

package ru.org.linux.util.bbcode.nodes;

import ru.org.linux.util.bbcode.Parser;
import ru.org.linux.util.bbcode.ParserParameters;
import ru.org.linux.util.formatter.ToHtmlFormatter;

/**
 * Узел дерева разбора LORCODE с текстом
 */
public class TextNode extends Node {
  final String text;
  final RootNode rootNode;

  public TextNode(Node parent, ParserParameters parserParameters, String text, RootNode rootNode) {
    super(parent, parserParameters);
    this.text = text;
    this.rootNode = rootNode;
  }

  public String getText() {
    return text;
  }

  @Override
  public String renderXHtml() {
    if (TagNode.class.isInstance(parent)) {
      TagNode tagNode = (TagNode) parent;
      if (parserParameters.getAutoLinkTags().contains(tagNode.bbtag.getName())) {
        ToHtmlFormatter toHtmlFormatter = rootNode.getToHtmlFormatter();
        if(toHtmlFormatter != null) {
          return toHtmlFormatter.format(text, rootNode.isSecure());
        }
      }
    }
    return Parser.escape(text);
  }

  @Override
  public String renderBBCode() {
    return text;
  }

  @Override
  public boolean allows(String tagname) {
    return false;
  }
}
