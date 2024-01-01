/*
 * Copyright 1998-2024 Linux.org.ru
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
import org.apache.commons.httpclient.URIException;
import ru.org.linux.util.URLUtil;
import ru.org.linux.util.bbcode.Parser;
import ru.org.linux.util.bbcode.ParserParameters;
import ru.org.linux.util.bbcode.nodes.Node;
import ru.org.linux.util.bbcode.nodes.RootNode;
import ru.org.linux.util.bbcode.nodes.TagNode;
import ru.org.linux.util.bbcode.nodes.TextNode;
import ru.org.linux.util.formatter.ToHtmlFormatter;

public class UrlTag extends Tag {
  public UrlTag(ImmutableSet<String> allowedChildren, ParserParameters parserParameters) {
    super("url", allowedChildren, "p", parserParameters);
  }

  @Override
  public String renderNodeXhtml(Node node) {
    StringBuilder ret = new StringBuilder();
    if (node.lengthChildren() == 0) {
      return "";
    }
    // Внутри [url] только текст
    TextNode txtNode = (TextNode) node.getChildren().iterator().next();
    String url;
    if (node.isParameter()) {
      // сюда никогда не должна попасть, обрабатывается специально в Parser
      url = node.getParameter().trim();
    } else {
      url = txtNode.getText().trim();
    }
    String linkText = txtNode.getText().trim();
    if (linkText.isEmpty()) {
      linkText = url;
    }
    String escapedUrl = URLUtil.fixURL(url);
    TagNode tagNode = (TagNode)node;
    RootNode rootNode = tagNode.getRootNode();
    ToHtmlFormatter formatter = rootNode.getToHtmlFormatter();

   if (formatter != null) {
      StringBuilder out = new StringBuilder();

      try {
        formatter.processUrl(
                rootNode.isNofollow(),
                out,
                escapedUrl,
                linkText
        );

        ret.append(out);
      } catch (URIException e) {
        ret.append("<s>");
        ret.append(Parser.escape(url));
        ret.append("</s>");
      }
    } else {
      String formattedText = Parser.escape(linkText);

      if (URLUtil.isUrl(escapedUrl)) {
        ret.append("<a href=\"");
        ret.append(escapedUrl);

        ret.append('\"');

        if (rootNode.isNofollow()) {
          ret.append(" rel=nofollow");
        }

        ret.append(">");
        ret.append(formattedText);
        ret.append("</a>");
      } else {
        ret.append("<s>");
        ret.append(Parser.escape(url));
        ret.append("</s>");
      }
    }

    return ret.toString();
  }
}
