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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*
 *@author bvn13 at 2018-11-11
 */
@Service
@Qualifier("flexmark")
public class FlexmarkMarkdownFormatter implements MarkdownFormatter {

    private MutableDataSet options;
    private Parser parser;
    private HtmlRenderer renderer;

//    private static final Pattern PATTERN_ESCAPE_SCRIPT_TAG = Pattern.compile("(<script)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
//    private static final Pattern PATTERN_ESCAPE_SCRIPT_CLOSE_TAG = Pattern.compile("(</script)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    public FlexmarkMarkdownFormatter() {
        options = new MutableDataSet();

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
        options.set(Parser.HTML_BLOCK_PARSER, false);
        options.set(HtmlRenderer.SUPPRESS_HTML, true);

        // uncomment to convert soft-breaks to hard breaks
        //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }

    @Override
    public String renderToHtml(String content) {
        //escape script blocks in cause of vulnerability

//        Matcher matcherOpenTag = PATTERN_ESCAPE_SCRIPT_TAG.matcher(content);
//        if (matcherOpenTag.find()) {
//            content = matcherOpenTag.replaceAll("&lt;script");
//        }
//
//        Matcher matcherCloseTag = PATTERN_ESCAPE_SCRIPT_CLOSE_TAG.matcher(content);
//        if (matcherCloseTag.find()) {
//            content = matcherCloseTag.replaceAll("&lt;/script");
//        }

        // You can re-use parser and renderer instances
        Node document = parser.parse(content);
        String html = renderer.render(document);  // "<p>This is <em>Sparta</em></p>\n"

        return html;
    }

}
