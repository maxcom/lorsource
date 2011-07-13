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

package ru.org.linux.util.bbcode;

import ru.org.linux.util.bbcode.nodes.*;
import ru.org.linux.util.bbcode.tags.*;

import java.sql.Connection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by IntelliJ IDEA.
 * User: hizel
 * Date: 6/30/11
 * Time: 1:18 PM
 */
public class Parser {

    // флаги для конструктора
    public static final int flagRenderAll = 0;
    public static final int flagIgnoreCut = 1;
    public static final int flagIgnoreImg = 2;

    // регулярное выражения поиска bbcode тэга
    public static final Pattern BBTAG_REGEXP = Pattern.compile("\\[\\[?/?([A-Za-z\\*]+)(:[a-f0-9]+)?(=[^\\]]+)?\\]?\\]");
    // регулярное выражения поиска перевода строк два и более раз подряд
    // TODO не надо ли учитывать проеблы между ними? :-|
    public static final Pattern P_REGEXP = Pattern.compile("(\r?\n){2,}");


    private Set<String> INLINE_TAGS;
    private Set<String> BLOCK_LEVEL_TAGS;
    private Set<String> FLOW_TAGS;
    private Set<String> OTHER_TAGS;
    private Set<String> ANCHOR_TAGS;

    private Set<String> ALLOWED_LIST_TYPE;

    private List<Tag> TAGS;
    private Map<String,Tag> TAG_DICT;
    private Set<String> TAG_NAMES;


    public Parser(int flags){
        ALLOWED_LIST_TYPE = new HashSet<String>();
        ALLOWED_LIST_TYPE.add("A");
        ALLOWED_LIST_TYPE.add("a");
        ALLOWED_LIST_TYPE.add("I");
        ALLOWED_LIST_TYPE.add("i");
        ALLOWED_LIST_TYPE.add("1");

        // Простые тэги, в детях им подобные и текст
        INLINE_TAGS = new HashSet<String>();
        INLINE_TAGS.add("b");
        INLINE_TAGS.add("i");
        INLINE_TAGS.add("u");
        INLINE_TAGS.add("s");
        INLINE_TAGS.add("em");
        INLINE_TAGS.add("strong");
        INLINE_TAGS.add("url");
        INLINE_TAGS.add("user");
        INLINE_TAGS.add("br");
        INLINE_TAGS.add("text");
        INLINE_TAGS.add("img");
        INLINE_TAGS.add("softbr");

        //Блочные тэги
        BLOCK_LEVEL_TAGS = new HashSet<String>();
        BLOCK_LEVEL_TAGS.add("p");
        BLOCK_LEVEL_TAGS.add("quote");
        BLOCK_LEVEL_TAGS.add("list");
        BLOCK_LEVEL_TAGS.add("pre");
        BLOCK_LEVEL_TAGS.add("code");
        BLOCK_LEVEL_TAGS.add("div");
        BLOCK_LEVEL_TAGS.add("cut");

        // Все тэги кроме специальных
        FLOW_TAGS = new HashSet<String>();
        FLOW_TAGS.addAll(INLINE_TAGS);
        FLOW_TAGS.addAll(BLOCK_LEVEL_TAGS);

        // специальный дурацкий тэг
        OTHER_TAGS = new HashSet<String>();
        OTHER_TAGS.add("*");

        ANCHOR_TAGS = new HashSet<String>();
        ANCHOR_TAGS.add("url");

        TAGS = new ArrayList<Tag>();
        { // <br/>
            HtmlEquivTag tag = new HtmlEquivTag("br", new HashSet<String>(), "div", this);
            tag.setSelfClosing(true);
            //tag.setDiscardable(true);
            tag.setHtmlEquiv("br");
            TAGS.add(tag);
        }
        { // <br/>, but can adapt during render ?
            Set<String> children = new HashSet<String>();
            SoftBrTag tag = new SoftBrTag("softbr", children, "div", this);
            tag.setSelfClosing(true);
            tag.setDiscardable(true);
            TAGS.add(tag);
        }
        { // <b>
            HtmlEquivTag tag = new HtmlEquivTag("b", INLINE_TAGS, "div", this);
            tag.setHtmlEquiv("b");
            TAGS.add(tag);
        }
        { // <i>
            HtmlEquivTag tag = new HtmlEquivTag("i", INLINE_TAGS, "div", this);
            tag.setHtmlEquiv("i");
            TAGS.add(tag);
        }
        { // <u> TODO Allert: The U tag has been deprecated in favor of the text-decoration style property.
            HtmlEquivTag tag = new HtmlEquivTag("u", INLINE_TAGS, "div", this);
            tag.setHtmlEquiv("u");
            TAGS.add(tag);
        }
        { // <s> TODO Allert: The S tag has been deprecated in favor of the text-decoration style property.
            HtmlEquivTag tag = new HtmlEquivTag("s", INLINE_TAGS, "div", this);
            tag.setHtmlEquiv("s");
            TAGS.add(tag);
        }
        { // <em>
            HtmlEquivTag tag = new HtmlEquivTag("em", INLINE_TAGS, "div", this);
            tag.setHtmlEquiv("em");
            TAGS.add(tag);
        }
        { // <strong>
            HtmlEquivTag tag = new HtmlEquivTag("strong", INLINE_TAGS, "div", this);
            tag.setHtmlEquiv("strong");
            TAGS.add(tag);
        }
        { // <a>
            Set<String> el = new HashSet<String>();
            el.add("text");
            UrlTag tag = new UrlTag("url", el, "div", this);
            TAGS.add(tag);
        }
        { // <a> member
            Set<String> el = new HashSet<String>();
            el.add("text");
            MemberTag tag = new MemberTag("user", el, "div", this);
            TAGS.add(tag);
        }
        { // <p>
            HtmlEquivTag tag = new HtmlEquivTag("p", INLINE_TAGS, null, this);
            tag.setHtmlEquiv("p");
            TAGS.add(tag);
        }
        { // <div>
            HtmlEquivTag tag = new HtmlEquivTag("div", FLOW_TAGS, null, this);
            tag.setHtmlEquiv("");
            TAGS.add(tag);
        }
        { // <blockquote>
            Set<String> el = new HashSet<String>();
            el.addAll(BLOCK_LEVEL_TAGS);
            el.add("softbr");
            QuoteTag tag = new QuoteTag("quote", el, "div", this);
            TAGS.add(tag);
        }
        { // <ul>
            Set<String> el = new HashSet<String>();
            el.add("*");
            el.add("softbr");
            ListTag tag = new ListTag("list", el, null, this);
            TAGS.add(tag);
        }
        { // <pre> (only img currently needed out of the prohibited elements)
            Set<String> elements = new HashSet<String>();
            elements.add("img");
            elements.add("big");
            elements.add("small");
            elements.add("sub");
            elements.add("sup");
            HtmlEquivTag tag = new HtmlEquivTag("pre", INLINE_TAGS, null, this);
            tag.setHtmlEquiv("pre");
            tag.setProhibitedElements(elements);
            TAGS.add(tag);
        }
        { // <pre class="code">
            Set<String> elements = new HashSet<String>();
            elements.add("img");
            elements.add("big");
            elements.add("small");
            elements.add("sub");
            elements.add("sup");

            CodeTag tag = new CodeTag("code", INLINE_TAGS, null, this);
            tag.setProhibitedElements(elements);
            TAGS.add(tag);
        }
        {   // [cut]
            CutTag tag = new CutTag("cut", FLOW_TAGS, null, this);
            tag.setHtmlEquiv("div");
            TAGS.add(tag);
        }
        { //  <li>
            LiTag tag = new LiTag("*", FLOW_TAGS, "list", this);
            TAGS.add(tag);
        }

        TAG_DICT = new HashMap<String, Tag>();
        for(Tag tag : TAGS){
            if(!"text".equals(tag.getName())){
                TAG_DICT.put(tag.getName(), tag);
            }
        }
        TAG_NAMES = new HashSet<String>();
        for(Tag tag : TAGS){
            TAG_NAMES.add(tag.getName());
        }
    }

    public static String escape(String html){
        return html
                .replace("&", "&amp;")
                .replace("<","&lt;")
                .replace(">","&gt;")
                .replace("\"", "&quot;");
    }

    protected boolean rootAllowsInline;

    private Node pushTextNode(Node currentNode, String text, boolean escaped){
        if(!currentNode.allows("text")){
            if(text.trim().length() == 0){
                if(escaped){
                    currentNode.getChildren().add(new EscapedTextNode(currentNode, this, text));
                }else{
                    currentNode.getChildren().add(new TextNode(currentNode, this, text));
                }
            }else{
                if(currentNode.allows("div")){
                    currentNode.getChildren().add(new TagNode(currentNode, this, "div", ""));
                    currentNode = descend(currentNode);
                }else{
                    currentNode = ascend(currentNode);
                }
                currentNode = pushTextNode(currentNode, text, false);
            }
        }else{
            Matcher matcher = P_REGEXP.matcher(text);

            boolean isCode = false;
            if(TagNode.class.isInstance(currentNode)){
                TagNode tempNode  = (TagNode)currentNode;
                if(CodeTag.class.isInstance(tempNode.getBbtag())){
                    isCode = true;
                }
            }

            if(matcher.find() && !isCode){
				currentNode = pushTextNode(currentNode, text.substring(0, matcher.start()), false);
				currentNode = ascend(currentNode);
                currentNode.getChildren().add(new TagNode(currentNode, this, "p", " "));
                currentNode = descend(currentNode);
                currentNode = pushTextNode(currentNode, text.substring(matcher.end()), false);
            }else{
                if(escaped){
                    currentNode.getChildren().add(new EscapedTextNode(currentNode, this, text));
                }else{
                    currentNode.getChildren().add(new TextNode(currentNode, this, text));
                }
            }
        }
        return currentNode;
    }

    private Node descend(Node currentNode){
        return currentNode.getChildren().get(currentNode.getChildren().size()-1);
    }

    private Node ascend(Node currentNode){
        return currentNode.getParent();
    }

    private Node pushTagNode(RootNode rootNode, Node currentNode, String name, String parameter, boolean renderCut, String cutUrl){
        if(!currentNode.allows(name)){
            Tag newTag = TAG_DICT.get(name);

            if(newTag.isDiscardable()){
                return currentNode;
            }else if(currentNode == rootNode || BLOCK_LEVEL_TAGS.contains(((TagNode)currentNode).getBbtag().getName()) && newTag.getImplicitTag() != null){
                currentNode = pushTagNode(rootNode, currentNode, newTag.getImplicitTag(), "", renderCut, cutUrl);
                currentNode = pushTagNode(rootNode, currentNode, name, parameter, renderCut, cutUrl);
            }else{
                currentNode = currentNode.getParent();
                currentNode = pushTagNode(rootNode, currentNode, name, parameter, renderCut, cutUrl);
            }
        }else{
            TagNode node = new TagNode(currentNode, this, name, parameter);
            if("cut".equals(name)){
                ((CutTag)(node.getBbtag())).setRenderOptions(renderCut, cutUrl);
            }
            currentNode.getChildren().add(node);
            if(!node.getBbtag().isSelfClosing()){
                currentNode = descend(currentNode);
            }
        }
        return currentNode;
    }

    private Node closeTagNode(RootNode rootNode, Node currentNode, String name){
        Node tempNode = currentNode;
        while (true){
            if(tempNode == rootNode){
                break;
            }
            if(TagNode.class.isInstance(tempNode)){
                TagNode node = (TagNode)tempNode;
                if(node.getBbtag().getName().equals(name)){
                    currentNode = tempNode;
                    currentNode = ascend(currentNode);
                    break;
                }
            }
            tempNode = tempNode.getParent();
        }
        return currentNode;
    }

    protected String prepare(String bbcode){
        return bbcode.replaceAll("\r\n", "\n").replaceAll("\n\n", "[softbr]");
    }

    public RootNode parse(String rawbbcode){
        return parse(rawbbcode, true, "");

    }

    public RootNode parse(String rawbbcode, boolean renderCut, String cutUrl){
        RootNode rootNode = new RootNode(this);
        Node currentNode = rootNode;
        String bbcode = rawbbcode;
        int pos = 0;
        boolean isCode = false;
        while(pos<bbcode.length()){
            Matcher match = BBTAG_REGEXP.matcher(bbcode).region(pos,bbcode.length());
            if(match.find()){
                currentNode = pushTextNode(currentNode, bbcode.substring(pos,match.start()), false);
                String tagname = match.group(1);
                String parameter = match.group(3);
                String wholematch = match.group(0);

                if(wholematch.startsWith("[[") && wholematch.endsWith("]]")){
                    currentNode = pushTextNode(currentNode, wholematch.substring(1,wholematch.length()-1), true);
                }else{
                    if(parameter != null && parameter.length() > 0){
                        parameter = parameter.substring(1);
                    }
                    if(TAG_NAMES.contains(tagname)){
                        if(wholematch.startsWith("[[")){
                            currentNode = pushTextNode(currentNode, "[", false);
                        }


                        if(wholematch.startsWith("[/")){
                            if(!isCode || "code".equals(tagname)){
                                currentNode = closeTagNode(rootNode, currentNode, tagname);
                            }else{
                                currentNode = pushTextNode(currentNode, wholematch,false);
                            }
                            if("code".equals(tagname)){
                                isCode = false;
                            }
                        }else{
                            if(isCode && !"code".equals(tagname)){
                                currentNode = pushTextNode(currentNode, wholematch, false);
                            }else if("code".equals(tagname)){
                                isCode = true;
                                currentNode = pushTagNode(rootNode, currentNode, tagname, parameter, renderCut, cutUrl);
                            }else{
                                currentNode = pushTagNode(rootNode, currentNode, tagname, parameter, renderCut, cutUrl);
                            }
                        }

                        if(wholematch.endsWith("]]")){
                            currentNode = pushTextNode(currentNode, "]", false);
                        }
                    }else{
                        currentNode = pushTextNode(currentNode, wholematch, false);
                    }
                }
                pos = match.end();
            }else{
                currentNode = pushTextNode(currentNode, bbcode.substring(pos),false);
                pos = bbcode.length();
            }
        }
        return rootNode;
    }

    public String renderXHtml(RootNode rootNode, Connection db){
        return rootNode.renderXHtml(db);
    }

    public String renderBBCode(RootNode rootNode){
        return rootNode.renderBBCode();
    }

    public Set<String> getALLOWED_LIST_TYPE() {
        return ALLOWED_LIST_TYPE;
    }

    public Set<String> getINLINE_TAGS() {
        return INLINE_TAGS;
    }

    public Set<String> getBLOCK_LEVEL_TAGS() {
        return BLOCK_LEVEL_TAGS;
    }

    public Set<String> getFLOW_TAGS() {
        return FLOW_TAGS;
    }

    public Set<String> getOTHER_TAGS() {
        return OTHER_TAGS;
    }

    public List<Tag> getTAGS() {
        return TAGS;
    }

    public Map<String, Tag> getTAG_DICT() {
        return TAG_DICT;
    }

    public Set<String> getTAG_NAMES() {
        return TAG_NAMES;
    }
}
