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

import com.google.common.collect.ImmutableMap;
import ru.org.linux.util.bbcode.ParserParameters;
import ru.org.linux.util.bbcode.nodes.Node;
import ru.org.linux.util.bbcode.nodes.TextNode;

import java.util.Set;

public class CodeTag extends Tag {
  private static final ImmutableMap<String, String> langHash =
          ImmutableMap.<String, String>builder().
                  put("bash", "language-bash")
                  .put("coffeescript", "language-coffeescript")
                  .put("shell", "language-bash")
                  .put("cpp", "language-cpp")
                  .put("cxx", "language-cpp")
                  .put("cc", "language-cpp")
                  .put("c", "language-cpp")
                  .put("diff", "language-diff")
                  .put("patch", "language-diff")
                  .put("java", "language-java")
                  .put("js", "language-javascript")
                  .put("javascript", "language-javascript")
                  .put("perl", "language-perl")
                  .put("php", "language-php")
                  .put("plain", "no-highlight")
                  .put("python", "language-python")
                  .put("css", "language-css")
                  .put("delphi", "language-delphi")
                  .put("pascal", "language-delphi")
                  .put("html", "language-html")
                  .put("xml", "language-xml")
                  .put("lisp", "language-lisp")
                  .put("scheme", "language-lisp")
                  .put("ruby", "language-ruby")
                  .put("cs", "language-cs").put("c#", "language-cs")
                  .put("sql", "language-sql")
                  .put("ini", "language-ini")
                  .put("cmake", "language-cmake")
                  .put("erlang", "language-erlang")
                  .put("objectivec", "language-objectivec").put("objc", "language-objectivec")
                  .put("scala", "language-scala")
                  .put("vhdl", "language-vhdl")
                  .put("lua", "language-lua")
                  .put("smalltalk", "language-smalltalk")
                  .put("vala", "language-vala")
                  .put("go", "language-go")
                  .put("tex", "language-tex")
                  .put("haskell", "language-haskell")
                  .build();

  public CodeTag(String name, Set<String> allowedChildren, String implicitTag, ParserParameters parserParameters) {
    super(name, allowedChildren, implicitTag, parserParameters);
  }

  @Override
  public String renderNodeOg(Node node) {
    return "";
  }

  @Override
  public String renderNodeXhtml(Node node) {
    if (node.lengthChildren() == 0) {
      return "";
    } else {
      // обработка пустого тэга
      if (node.lengthChildren() == 1) {
        Node child = node.getChildren().iterator().next();
        if (TextNode.class.isInstance(child) && ((TextNode) child).getText().trim().isEmpty()) {
          return "";
        }
      }
    }
    StringBuilder ret = new StringBuilder();
    if (node.isParameter()) {
      String lang = node.getParameter().trim();
      if (langHash.containsKey(lang)) {
        ret.append("<div class=\"code\"><pre class=\"").append(langHash.get(lang)).append("\"><code>");
      } else {
        ret.append("<div class=\"code\"><pre class=\"no-highlight\"><code>");
      }
    } else {
      ret.append("<div class=\"code\"><pre class=\"no-highlight\"><code>");
    }
    ret.append(node.renderChildrenXHtml());
    ret.append("</code></pre></div>");
    return ret.toString();
  }
}
