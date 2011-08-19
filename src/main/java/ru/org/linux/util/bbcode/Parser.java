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
 * Основной класс преобразования LORCODE в html
 */
public class Parser {

  /**
   * Флаги для конструктора
   */
  public enum ParserFlags {
    ENABLE_IMG_TAG,
    IGNORE_CUT_TAG
  }

  /**
   * Регулярное выражение поиска тэга
   */
  public static final Pattern BBTAG_REGEXP = Pattern.compile("\\[\\[?/?([A-Za-z\\*]+)(:[a-f0-9]+)?(=[^\\]]+)?\\]?\\]");

  /**
   * Регулярное выражения поиска двойного перевода строки
   */
  public static final Pattern P_REGEXP = Pattern.compile("(\r?\n){2,}");

  /**
   * Множество тэгов которое содержат или текст или себе подобных
   */
  private final ImmutableSet<String> inlineTags;

  /**
   * Множество тэгов которым разрешено присутствовать в тэге url с параметром, вида [url=http://some]....[/url]
   */
  private final ImmutableSet<String> urlTags;

  /**
   * Множество тэгов которые могут содержать любые тэги
   */
  private final ImmutableSet<String> blockLevelTags;

  /**
   * Все тэги из inlineTags и blockLevelTags
   */
  private final ImmutableSet<String> flowTags;

  /**
   * Тэг списка :-|
   */
  private final ImmutableSet<String> otherTags;

  /**
   * Тэги внутри которых работает автовыделение ссылок
   */
  private final ImmutableSet<String> autoLinkTags;

  /**
   * Разрешенные параметры для тэга list
   */
  private final ImmutableSet<String> allowedListParameters;

  /**
   * Список всех тэгов
   */
  private final List<Tag> allTags;

  /**
   * Хэш соответствия имя тэга -> класс тэга
   */
  private final Map<String, Tag> allTagsDict;

  /**
   * Множество всех имен тэгов
   */
  private final ImmutableSet<String> allTagsNames;

  /**
   * Конструктор
   * @param flags флаги которые влияют на созданный объект
   */
  public Parser(EnumSet<ParserFlags> flags) {
    allowedListParameters = ImmutableSet.of("A", "a", "I", "i", "1");
    inlineTags = ImmutableSet.of("b", "i", "u", "s", "em", "strong", "url", "url2", "user", "br", "text", "img", "softbr");
    urlTags = ImmutableSet.of("b", "i", "u", "s", "strong", "text");
    blockLevelTags = ImmutableSet.of("p", "quote", "list", "pre", "code", "div", "cut");
    autoLinkTags = ImmutableSet.of("b", "i", "u", "s", "em", "strong", "p", "quote", "div", "cut", "pre");
    flowTags = new ImmutableSet.Builder<String>()
            .addAll(inlineTags)
            .addAll(blockLevelTags)
            .build();

    otherTags = ImmutableSet.of("*");

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

  /**
   * Добавление текстового узда
   * @param rootNode корневой узел
   * @param currentNode текущий узел
   * @param text текст
   * @return возвращает новй текущий узел
   */
  private Node pushTextNode(RootNode rootNode, Node currentNode, String text) {
    if (!currentNode.allows("text")) {
      if (text.trim().length() == 0) {
        currentNode.getChildren().add(new TextNode(currentNode, this, text));
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
        currentNode = pushTextNode(rootNode, currentNode, text);
      }
    } else {
      Matcher matcher = P_REGEXP.matcher(text);

      boolean isCode = false;
      boolean isP = false;
      boolean isAllow = true;
      if (TagNode.class.isInstance(currentNode)) {
        TagNode tempNode = (TagNode) currentNode;
        if (CodeTag.class.isInstance(tempNode.getBbtag())) {
          isCode = true;
        }
        if ("pre".equals(tempNode.getBbtag().getName()) ||
            "url".equals(tempNode.getBbtag().getName()) ||
            "user".equals(tempNode.getBbtag().getName())) {
          isAllow = false;
        }
        if ("p".equals(tempNode.getBbtag().getName())) {
          isP = true;
        }
      }

      if (matcher.find() && !isCode && isAllow) {
        if(matcher.start() != 0){
          currentNode = pushTextNode(rootNode, currentNode, text.substring(0, matcher.start()));
        }
        if (isP) {
          currentNode = ascend(currentNode);
        }
        if(matcher.end() != text.length()){
          currentNode.getChildren().add(new TagNode(currentNode, this, "p", " "));
          currentNode = descend(currentNode);
          currentNode = pushTextNode(rootNode, currentNode, text.substring(matcher.end()));
        }
      } else {
        currentNode.getChildren().add(new TextNode(currentNode, this, text));
      }
    }
    return currentNode;
  }

  /**
   * Сдвигает текущий узед в дереве на уровень ниже текущего узла
   * @param currentNode текщуий узел
   * @return новый текущий узел
   */
  private Node descend(Node currentNode) {
    return currentNode.getChildren().get(currentNode.getChildren().size() - 1);
  }

  /**
   * Сдвигает текущий узел на уровень выше текущего узла
   * @param currentNode текущий узел
   * @return новый текущий узел
   */
  private Node ascend(Node currentNode) {
    return currentNode.getParent();
  }

  /**
   * Добавление в дерево нового узла с тэгом
   * @param rootNode корневой узел дерева
   * @param currentNode текущий узел
   * @param name название тэга
   * @param parameter параметры тэга
   * @return возвращает новый текущий узел дерева
   */
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
      if("user".equals(name)) {
        MemberTag memberTag = ((MemberTag) (node.getBbtag()));
        memberTag.setConnection(rootNode.getConnection());
      }
      currentNode.getChildren().add(node);
      if (!node.getBbtag().isSelfClosing()) {
        currentNode = descend(currentNode);
      }
    }
    return currentNode;
  }

  /**
   * Обрабатывает закрытие тэга
   * @param rootNode корневой узел
   * @param currentNode текущий узел
   * @param name имя закрываемого тэга
   * @return новый текущий узел после закрытия тэга
   */
  private Node closeTagNode(RootNode rootNode, Node currentNode, String name) {
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

  /**
   * Точка входа для разбора LORCODE в которой rootNode создается
   * @param rawbbcode обрабатываемый LORCODE
   * @return дерево разбора
   */
  public RootNode parse(String rawbbcode) {
    RootNode rootNode = new RootNode(this);
    rootNode.setRenderOptions(true, true, "");
    return parse(rootNode, rawbbcode);
  }

  /**
   * Точка входа для разбора LORCODE
   *
   * @param rootNode корневой узел новго дерева
   * @param bbcode обрабатываемы LORCODE
   * @return возвращает инвалидный html
   */
  public RootNode parse(RootNode rootNode, String bbcode) {
    Node currentNode = rootNode;
    int pos = 0;
    boolean isCode = false;
    while (pos < bbcode.length()) {
      Matcher match = BBTAG_REGEXP.matcher(bbcode).region(pos, bbcode.length());
      if (match.find()) {
        currentNode = pushTextNode(rootNode, currentNode, bbcode.substring(pos, match.start()));
        String tagname = match.group(1);
        String parameter = match.group(3);
        String wholematch = match.group(0);

        if (wholematch.startsWith("[[") && wholematch.endsWith("]]")) {
          currentNode = pushTextNode(rootNode, currentNode, wholematch.substring(1, wholematch.length() - 1));
        } else {
          if (parameter != null && parameter.length() > 0) {
            parameter = parameter.substring(1);
          }
          if (allTagsNames.contains(tagname)) {
            if (wholematch.startsWith("[[")) {
              currentNode = pushTextNode(rootNode, currentNode, "[");
            }


            if (wholematch.startsWith("[/")) {
              if (!isCode || "code".equals(tagname)) {
                currentNode = closeTagNode(rootNode, currentNode, tagname);
              } else {
                currentNode = pushTextNode(rootNode, currentNode, wholematch);
              }
              if ("code".equals(tagname)) {
                isCode = false;
              }
            } else {
              if (isCode && !"code".equals(tagname)) {
                currentNode = pushTextNode(rootNode, currentNode, wholematch);
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
              currentNode = pushTextNode(rootNode, currentNode, "]");
            }
          } else {
            currentNode = pushTextNode(rootNode, currentNode, wholematch);
          }
        }
        pos = match.end();
      } else {
        currentNode = pushTextNode(rootNode, currentNode, bbcode.substring(pos));
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
