/*
 * Copyright 1998-2018 Linux.org.ru
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

package ru.org.linux.util.markdown;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension;
import com.vladsch.flexmark.ext.youtube.embedded.YouTubeLinkExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.superscript.SuperscriptExtension;
import com.vladsch.flexmark.util.options.MutableDataSet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/*
 *@author bvn13 at 2018-11-11
 */
@Service
@Qualifier("flexmark")
public class FlexmarkMarkdownFormatter implements MarkdownFormatter {

  private final Parser parser;
  private final HtmlRenderer renderer;

  public FlexmarkMarkdownFormatter() {
    MutableDataSet options = new MutableDataSet();

    // uncomment to set optional extensions
    options.set(Parser.EXTENSIONS, Arrays.asList(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            YouTubeLinkExtension.create(),
            WikiLinkExtension.create(),
            SuperscriptExtension.create()
                /*,
                EnumeratedReferenceExtension.create()*/
    ));

    options.set(HtmlRenderer.SUPPRESSED_LINKS, "javascript:.*");
    //options.set(Parser.HTML_BLOCK_PARSER, false);
    options.set(HtmlRenderer.SUPPRESS_HTML, true);

    // uncomment to convert soft-breaks to hard breaks
    //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

    parser = Parser.builder(options).build();
    renderer = HtmlRenderer.builder(options).build();
  }

  @Override
  public String renderToHtml(String content) {
    // You can re-use parser and renderer instances
    Node document = parser.parse(content);

    return renderer.render(document);
  }

}
