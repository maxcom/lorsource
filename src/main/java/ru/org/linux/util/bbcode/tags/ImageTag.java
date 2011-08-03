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

import ru.org.linux.util.bbcode.Parser;
import ru.org.linux.util.bbcode.nodes.Node;
import ru.org.linux.util.bbcode.nodes.TextNode;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 7/13/11
 * Time: 3:42 PM
 */
public class ImageTag extends Tag {
  public static final Pattern IMG_URL_REGEXP = Pattern.compile("(\\w+)://([\\w\\d]+)/?(.*)?");

  public ImageTag(String name, Set<String> allowedChildren, String implicitTag, Parser parser) {
    super(name, allowedChildren, implicitTag, parser);
  }

  @Override
  public String renderNodeXhtml(Node node) {
    if (node.lengthChildren() == 0) {
      return "";
    }
    TextNode txtNode = (TextNode) node.getChildren().iterator().next();
    String imageUrl = Parser.escape(txtNode.getText()).trim();
    return String.format("<img src=\"%s\" />", imageUrl);
    // TODO надо отладить
/*        Matcher matcher = IMG_URL_REGEXP.matcher(imageUrl);
        try {
            if(matcher.find()){
                String scheme = matcher.group(1);
                String domain = matcher.group(2);
                String param = matcher.group(3);
                URI uri = new URI(scheme, domain, param);
                return String.format("<img src=\"http://%s\" />", uri.toASCIIString());
            }else{
                return "";
            }
        } catch (Exception ex){
            return "";
        }*/
  }
}
