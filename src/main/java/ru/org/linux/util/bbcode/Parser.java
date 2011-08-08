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

import com.google.common.collect.ImmutableSet;
import ru.org.linux.util.HTMLFormatter;
import ru.org.linux.util.bbcode.nodes.*;
import ru.org.linux.util.bbcode.tags.*;

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

  public enum ParserFlags {
    ENABLE_IMG_TAG,
    IGNORE_CUT_TAG
  }

  // регулярное выражения поиска bbcode тэга
  public static final Pattern BBTAG_REGEXP = Pattern.compile("\\[\\[?/?([A-Za-z\\*]+)(:[a-f0-9]+)?(=[^\\]]+)?\\]?\\]");
  // регулярное выражения поиска перевода строк два и более раз подряд
  // TODO не надо ли учитывать проеблы между ними? :-|
  public static final Pattern P_REGEXP = Pattern.compile("(\r?\n){2,}");


  private final ImmutableSet<String> inlineTags;
  private final ImmutableSet<String> urlTags;
  private final ImmutableSet<String> blockLevelTags;
  private final ImmutableSet<String> flowTags;
  private final ImmutableSet<String> otherTags;
  private final ImmutableSet<String> anchorTags;
  private final ImmutableSet<String> autoLinkTags;

  private final ImmutableSet<String> allowedListParameters;

  private final List<Tag> allTags;
  private final Map<String, Tag> allTagsDict;
  private final ImmutableSet<String> allTagsNames;


  public Parser(EnumSet<ParserFlags> flags) {
    // разрешенные параметры для [list]
    allowedListParameters = ImmutableSet.of("A", "a", "I", "i", "1");

    // Простые тэги, в детях им подобные и текст
    inlineTags = ImmutableSet.of("b", "i", "u", "s", "em", "strong", "url", "url2", "user", "br", "text", "img", "softbr");

    // Тэги разрешенные внутри [url]
    urlTags = ImmutableSet.of("b", "i", "u", "s", "strong", "text");

    //Блочные тэги
    blockLevelTags = ImmutableSet.of("p", "quote", "list", "pre", "code", "div", "cut");

    //Тэги в которых разрешен автоматическое выделение ссылок
    autoLinkTags = ImmutableSet.of("b", "i", "u", "s", "em", "strong", "p", "quote", "div", "cut", "pre");

    // Все тэги кроме специальных
    flowTags = new ImmutableSet.Builder<String>()
            .addAll(inlineTags)
            .addAll(blockLevelTags)
            .build();

    // специальный дурацкий тэг
    otherTags = ImmutableSet.of("*");

    // незнаю зачем этот тэг выделен
    anchorTags = ImmutableSet.of("url");

    allTags = new ArrayList<Tag>();
    { // <br/>
      HtmlEquivTag tag = new HtmlEquivTag("br", ImmutableSet.<String>of(), "p", this);
      tag.setSelfClosing(true);
      //tag.setDiscardable(true);
      tag.setHtmlEquiv("br");
      allTags.add(tag);
    }
    { // <br/>, but can adapt during render ?
      SoftBrTag tag = new SoftBrTag("softbr", ImmutableSet.<String>of(), "p", this);
      tag.setSelfClosing(true);
      tag.setDiscardable(true);
      allTags.add(tag);
    }
    { // <b>
      HtmlEquivTag tag = new HtmlEquivTag("b", inlineTags, "p", this);
      tag.setHtmlEquiv("b");
      allTags.add(tag);
    }
    { // <i>
      HtmlEquivTag tag = new HtmlEquivTag("i", inlineTags, "p", this);
      tag.setHtmlEquiv("i");
      allTags.add(tag);
    }
    { // <u> TODO Allert: The U tag has been deprecated in favor of the text-decoration style property.
      HtmlEquivTag tag = new HtmlEquivTag("u", inlineTags, "p", this);
      tag.setHtmlEquiv("u");
      allTags.add(tag);
    }
    { // <s> TODO Allert: The S tag has been deprecated in favor of the text-decoration style property.
      HtmlEquivTag tag = new HtmlEquivTag("s", inlineTags, "p", this);
      tag.setHtmlEquiv("s");
      allTags.add(tag);
    }
    { // <em>
      HtmlEquivTag tag = new HtmlEquivTag("em", inlineTags, "p", this);
      tag.setHtmlEquiv("em");
      allTags.add(tag);
    }
    { // <strong>
      HtmlEquivTag tag = new HtmlEquivTag("strong", inlineTags, "p", this);
      tag.setHtmlEquiv("strong");
      allTags.add(tag);
    }
    { // <a>
      UrlTag tag = new UrlTag("url", ImmutableSet.<String>of("text"), "p", this);
      allTags.add(tag);
    }
    { // <a> специальный случай с парамтром
      UrlWithParamTag tag = new UrlWithParamTag("url2", urlTags, "p", this);
      allTags.add(tag);
    }
    { // <a> member
      MemberTag tag = new MemberTag("user", ImmutableSet.<String>of("text"), "p", this);
      allTags.add(tag);
    } // <img>
    if (flags.contains(ParserFlags.ENABLE_IMG_TAG)) {
      ImageTag tag = new ImageTag("img", ImmutableSet.<String>of("text"), "p", this);
      allTags.add(tag);
    }
    { // <p>
      HtmlEquivTag tag = new HtmlEquivTag("p", flowTags, null, this);
      tag.setHtmlEquiv("p");
      tag.setProhibitedElements(ImmutableSet.<String>of("div", "list", "quote", "cut"));
      allTags.add(tag);
    }
    { // <div>
      HtmlEquivTag tag = new HtmlEquivTag("div", blockLevelTags, null, this);
      tag.setHtmlEquiv("");
      allTags.add(tag);
    }
    { // <blockquote>
      QuoteTag tag = new QuoteTag("quote", blockLevelTags, "div", this);
      allTags.add(tag);
    }
    { // <ul>
      ListTag tag = new ListTag("list", ImmutableSet.<String>of("*", "softbr"), "div", this);
      allTags.add(tag);
    }
    { // <pre> (only img currently needed out of the prohibited elements)
      HtmlEquivTag tag = new HtmlEquivTag("pre", inlineTags, "div", this);
      tag.setHtmlEquiv("pre");
      tag.setProhibitedElements(ImmutableSet.<String>of("img"));
      allTags.add(tag);
    }
    { // <pre class="code">
      CodeTag tag = new CodeTag("code", inlineTags, "div", this);
      tag.setProhibitedElements(ImmutableSet.<String>of("img"));
      allTags.add(tag);
    }
    {   // [cut]
      CutTag tag = new CutTag("cut", flowTags, "div", this);
      tag.setHtmlEquiv("div");
      allTags.add(tag);
    }
    { //  <li>
      LiTag tag = new LiTag("*", flowTags, "list", this);
      allTags.add(tag);
    }

    allTagsDict = new HashMap<String, Tag>();
    for (Tag tag : allTags) {
      if (!"text".equals(tag.getName())) {
        allTagsDict.put(tag.getName(), tag);
      }
    }
    ImmutableSet.Builder<String> allTagsBuilder = new ImmutableSet.Builder<String>();
    for (Tag tag : allTags) {
      allTagsBuilder.add(tag.getName());
    }
    allTagsNames = allTagsBuilder.build();
  }

  public static String escape(String html) {
    return HTMLFormatter.htmlSpecialChars(html);
  }

  private boolean rootAllowsInline;

  private Node pushTextNode(RootNode rootNode, Node currentNode, String text, boolean escaped) {
    if (!currentNode.allows("text")) {
      if (text.trim().length() == 0) {
        if (escaped) {
          currentNode.getChildren().add(new EscapedTextNode(currentNode, this, text));
        } else {
          currentNode.getChildren().add(new TextNode(currentNode, this, text));
        }
      } else {
        if (currentNode.allows("p")) {
          currentNode.getChildren().add(new TagNode(currentNode, this, "p", ""));
          currentNode = descend(currentNode);
        } else if (currentNode.allows("div")) {
          currentNode.getChildren().add(new TagNode(currentNode, this, "div", ""));
          currentNode = descend(currentNode);
        } else {
          currentNode = ascend(currentNode);
        }
        currentNode = pushTextNode(rootNode, currentNode, text, false);
      }
    } else {
      Matcher matcher = P_REGEXP.matcher(text);

      boolean isCode = false;
      boolean isPre = false;
      boolean isP = false;
      if (TagNode.class.isInstance(currentNode)) {
        TagNode tempNode = (TagNode) currentNode;
        if (CodeTag.class.isInstance(tempNode.getBbtag())) {
          isCode = true;
        }
        if ("pre".equals(tempNode.getBbtag().getName())) {
          isPre = true;
        }
        if ("p".equals(tempNode.getBbtag().getName())) {
          isP = true;
        }
      }

      if (matcher.find() && !isCode && !isPre) {
        if(matcher.start() != 0){
          currentNode = pushTextNode(rootNode, currentNode, text.substring(0, matcher.start()), false);
        }
        if (isP) {
          currentNode = ascend(currentNode);
        }
        if(matcher.end() != text.length()){
          currentNode.getChildren().add(new TagNode(currentNode, this, "p", " "));
          currentNode = descend(currentNode);
          currentNode = pushTextNode(rootNode, currentNode, text.substring(matcher.end()), false);
        }
      } else {
        if (escaped) {
          currentNode.getChildren().add(new EscapedTextNode(currentNode, this, text));
        } else {
          currentNode.getChildren().add(new TextNode(currentNode, this, text));
        }
      }
    }
    return currentNode;
  }

  private static Node descend(Node currentNode) {
    return currentNode.getChildren().get(currentNode.getChildren().size() - 1);
  }

  private static Node ascend(Node currentNode) {
    return currentNode.getParent();
  }

  private Node pushTagNode(RootNode rootNode, Node currentNode, String name, String parameter) {
    if (!currentNode.allows(name)) {
      Tag newTag = allTagsDict.get(name);

      if (newTag.isDiscardable()) {
        return currentNode;
      } else if (currentNode == rootNode || blockLevelTags.contains(((TagNode) currentNode).getBbtag().getName()) && newTag.getImplicitTag() != null) {
        currentNode = pushTagNode(rootNode, currentNode, newTag.getImplicitTag(), "");
        currentNode = pushTagNode(rootNode, currentNode, name, parameter);
      } else {
        currentNode = currentNode.getParent();
        currentNode = pushTagNode(rootNode, currentNode, name, parameter);
      }
    } else {
      TagNode node = new TagNode(currentNode, this, name, parameter);
      if ("cut".equals(name)) {
        CutTag cutTag = ((CutTag) (node.getBbtag()));
        cutTag.setRenderOptions(
                rootNode.isRenderCut(),
                rootNode.isCleanCut(),
                rootNode.getCutUrl()
        );
        cutTag.setCutId(rootNode.getCutCount());
        rootNode.incCutCount();
      }
      currentNode.getChildren().add(node);
      if (!node.getBbtag().isSelfClosing()) {
        currentNode = descend(currentNode);
      }
    }
    return currentNode;
  }

  private static Node closeTagNode(RootNode rootNode, Node currentNode, String name) {
    Node tempNode = currentNode;
    while (true) {
      if (tempNode == rootNode) {
        break;
      }
      if (TagNode.class.isInstance(tempNode)) {
        TagNode node = (TagNode) tempNode;
        String tagName = node.getBbtag().getName();
        if (tagName.equals(name) || ("url".equals(name) && tagName.equals("url2"))) {
          currentNode = tempNode;
          currentNode = ascend(currentNode);
          break;
        }
      }
      tempNode = tempNode.getParent();
    }
    return currentNode;
  }

  protected String prepare(String bbcode) {
    return bbcode.replaceAll("\r\n", "\n").replaceAll("\n\n", "[softbr]");
  }

  public RootNode parse(String rawbbcode) {
    RootNode rootNode = new RootNode(this);
    rootNode.setRenderOptions(true, true, "");
    return parse(rootNode, rawbbcode);
  }

  /**
   * Основная функция
   *
   * @param bbcode сырой bbcode
   * @return возвращает инвалидный html
   */

  public RootNode parse(RootNode rootNode, String bbcode) {
    Node currentNode = rootNode;
    int pos = 0;
    boolean isCode = false;
    while (pos < bbcode.length()) {
      Matcher match = BBTAG_REGEXP.matcher(bbcode).region(pos, bbcode.length());
      if (match.find()) {
        currentNode = pushTextNode(rootNode, currentNode, bbcode.substring(pos, match.start()), false);
        String tagname = match.group(1);
        String parameter = match.group(3);
        String wholematch = match.group(0);

        if (wholematch.startsWith("[[") && wholematch.endsWith("]]")) {
          currentNode = pushTextNode(rootNode, currentNode, wholematch.substring(1, wholematch.length() - 1), true);
        } else {
          if (parameter != null && parameter.length() > 0) {
            parameter = parameter.substring(1);
          }
          if (allTagsNames.contains(tagname)) {
            if (wholematch.startsWith("[[")) {
              currentNode = pushTextNode(rootNode, currentNode, "[", false);
            }


            if (wholematch.startsWith("[/")) {
              if (!isCode || "code".equals(tagname)) {
                currentNode = closeTagNode(rootNode, currentNode, tagname);
              } else {
                currentNode = pushTextNode(rootNode, currentNode, wholematch, false);
              }
              if ("code".equals(tagname)) {
                isCode = false;
              }
            } else {
              if (isCode && !"code".equals(tagname)) {
                currentNode = pushTextNode(rootNode, currentNode, wholematch, false);
              } else if ("code".equals(tagname)) {
                isCode = true;
                currentNode = pushTagNode(rootNode, currentNode, tagname, parameter);
              } else {
                if ("url".equals(tagname) && parameter != null && parameter.length() > 0) {
                  // специальная проверка для [url] с параметром
                  currentNode = pushTagNode(rootNode, currentNode, "url2", parameter);
                } else {
                  currentNode = pushTagNode(rootNode, currentNode, tagname, parameter);
                }
              }
            }

            if (wholematch.endsWith("]]")) {
              currentNode = pushTextNode(rootNode, currentNode, "]", false);
            }
          } else {
            currentNode = pushTextNode(rootNode, currentNode, wholematch, false);
          }
        }
        pos = match.end();
      } else {
        currentNode = pushTextNode(rootNode, currentNode, bbcode.substring(pos), false);
        pos = bbcode.length();
      }
    }
    return rootNode;
  }

  public Set<String> getAllowedListParameters() {
    return allowedListParameters;
  }

  public Set<String> getInlineTags() {
    return inlineTags;
  }

  public Set<String> getBlockLevelTags() {
    return blockLevelTags;
  }

  public Set<String> getFlowTags() {
    return flowTags;
  }

  public Set<String> getOtherTags() {
    return otherTags;
  }

  public List<Tag> getAllTags() {
    return allTags;
  }

  public Map<String, Tag> getAllTagsDict() {
    return allTagsDict;
  }

  public Set<String> getAllTagsNames() {
    return allTagsNames;
  }

  public ImmutableSet<String> getAutoLinkTags() {
    return autoLinkTags;
  }
}
