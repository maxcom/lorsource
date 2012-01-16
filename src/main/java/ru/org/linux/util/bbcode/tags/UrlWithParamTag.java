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

import ru.org.linux.util.StringUtil;
import ru.org.linux.util.URLUtil;
import ru.org.linux.util.bbcode.Parser;
import ru.org.linux.util.bbcode.ParserParameters;
import ru.org.linux.util.bbcode.nodes.Node;
import ru.org.linux.util.bbcode.nodes.TextNode;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 7/26/11
 * Time: 12:09 PM
 */
public class UrlWithParamTag extends Tag {
  public UrlWithParamTag(String name, Set<String> allowedChildren, String implicitTag, ParserParameters parserParameters) {
    super(name, allowedChildren, implicitTag, parserParameters);
  }

  @Override
  public String renderNodeXhtml(Node node) {
    StringBuilder ret = new StringBuilder();
    String url = "";
    if (node.isParameter()) {
      url = node.getParameter().trim();
      if(url.startsWith("\"")) {
        url = url.substring(1);
        if(url.endsWith("\"")) {
          url = url.substring(0, url.length()-1);
        }
      } else if(url.startsWith("'")) {
        url = url.substring(1);
        if(url.endsWith("\'")) {
          url = url.substring(0, url.length()-1);
        }
      }
    }

    TextNode textChild = null;

    if(node.lengthChildren() == 1){
      Node child = node.getChildren().iterator().next();
      if(TextNode.class.isInstance(child)){
        textChild = (TextNode)child;
      }
    }

    String escapedUrl = URLUtil.fixURL(url);

    if (node.lengthChildren() == 0 || (textChild != null && textChild.getText().trim().isEmpty())){
      if(URLUtil.isUrl(escapedUrl)) {
        ret.append("<a href=\"")
                .append(escapedUrl)
                .append("\">")
                .append(escapedUrl)
                .append("</a>");
      } else {
        ret.append("<s title=\"")
                .append(StringUtil.escapeHtml(escapedUrl))
                .append("\">")
                .append(Parser.escape(url))
                .append("</s>");
      }
    } else {
      if(URLUtil.isUrl(escapedUrl)) {
        ret.append("<a href=\"")
                .append(escapedUrl)
                .append("\">")
                .append(node.renderChildrenXHtml())
                .append("</a>");
      } else {
        ret.append("<s title=\"")
            .append(StringUtil.escapeHtml(escapedUrl))
            .append("\">")
            .append(node.renderChildrenXHtml())
            .append("</s>");
      }
    }

    return ret.toString();
  }
}
