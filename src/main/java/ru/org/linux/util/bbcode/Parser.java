/*
 * Copyright 1998-2016 Linux.org.ru
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

package ru.org.linux.util.bbcode;

import org.apache.commons.lang3.StringUtils;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.bbcode.nodes.*;
import ru.org.linux.util.bbcode.tags.Tag;
import ru.org.linux.util.formatter.RuTypoChanger;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Основной класс преобразования LORCODE в html
 */
public class Parser {
  public static final Parser DEFAULT_PARSER = new Parser(new DefaultParserParameters());

  /**
   * Регулярное выражение поиска тэга
   */
  private static final Pattern BBTAG_REGEXP = Pattern.compile("\\[\\[?/?([A-Za-z\\*]+)(:[a-f0-9]+)?(=[^\\]]+)?\\]?\\]");

  /**
   * Регулярное выражения поиска двойного перевода строки
   */
  private static final Pattern P_REGEXP = Pattern.compile("(\r?\n){2,}");

  private final ParserParameters parserParameters;

  /**
   * Конструктор по умолчанию.
   *
   * @param parserParameters параметры парсера
   */
  public Parser(ParserParameters parserParameters) {
    this.parserParameters = parserParameters;
  }

  public static String escape(String html) {
    return StringUtil.escapeHtml(html);
  }

  public RootNode createRootNode() {
    return new RootNode(parserParameters);
  }

  /**
   * Точка входа для разбора LORCODE
   *
   * @param rootNode корневой узел нового дерева
   * @param bbcode   обрабатываемы LORCODE
   * @return возвращает инвалидный html
   */
  public RootNode parseRoot(RootNode rootNode, String bbcode) {
    Node currentNode = rootNode;
    ParserAutomatonState automatonState = new ParserAutomatonState(rootNode, parserParameters);

    while (automatonState.getPos() < bbcode.length()) {
      Matcher match = BBTAG_REGEXP.matcher(bbcode).region(automatonState.getPos(), bbcode.length());
      if (match.find()) {
        if (!automatonState.isFirstCode()) {
          currentNode = pushTextNode(automatonState, currentNode, bbcode.substring(automatonState.getPos(), match.start()));
        } else {
          currentNode = trimNewLine(automatonState, currentNode, bbcode, match);
        }
        automatonState.processTagMatcher(match);

        if (automatonState.isTagEscaped()) {
          currentNode = processEscapedTag(currentNode, automatonState);
        } else {
          if (automatonState.getAllTagsNames().contains(automatonState.getTagname())) {
            currentNode = processKnownTag(currentNode, automatonState);
          } else {
            currentNode = pushTextNode(automatonState, currentNode, automatonState.getWholematch());
          }
        }
        automatonState.setPos(match.end());
      } else {
        currentNode = pushTextNode(automatonState, currentNode, bbcode.substring(automatonState.getPos()));
        automatonState.setPos(bbcode.length());
      }
    }
    return automatonState.getRootNode();
  }

  /**
   * Добавление текстового узда
   *
   * @param automatonState текущее состояние автомата
   * @param currentNode    текущий узел
   * @param text           текст
   * @return возвращает новый текущий узел
   */
  private Node pushTextNode(ParserAutomatonState automatonState, Node currentNode, String text) {
    if (text.trim().isEmpty() && !currentNode.allows("text")) {
      return currentNode;
    }

    while (!currentNode.allows("text")) {
      if (currentNode.allows("p")) {
        TagNode node = new TagNode(currentNode, parserParameters, "p", "", automatonState.getRootNode());
        currentNode.addChildren(node);
        currentNode = node;
      } else if (currentNode.allows("div")) {
        TagNode node = new TagNode(currentNode, parserParameters, "div", "", automatonState.getRootNode());
        currentNode.addChildren(node);
        currentNode = node;
      } else {
        currentNode = currentNode.getParent();
      }
    }

    boolean isParagraph = false;
    boolean isAllow = true;
    boolean isParagraphed = false;

    if (TagNode.class.isInstance(currentNode)) {
      TagNode tempNode = (TagNode) currentNode;
      Set<String> disallowedParagraphTags = parserParameters.getDisallowedParagraphTags();
      Set<String> paragraphedTags = parserParameters.getParagraphedTags();
      if (disallowedParagraphTags.contains(tempNode.getBbtag().getName())) {
        isAllow = false;
      }
      if (paragraphedTags.contains(tempNode.getBbtag().getName())) {
        isParagraphed = true;
      }
      if ("p".equals(tempNode.getBbtag().getName())) {
        isParagraph = true;
      }
    }

    /**
     * Если мы находим двойной перенос строки и в тексте
     * и в текущем тэге разрешена вставка нового тэга p -
     * вставляем p
     * за исключеним, если текущий тэг p, тогда поднимаемся на уровень
     * выше в дереве и вставляем p с текстом
     */
    Matcher matcher = P_REGEXP.matcher(text);

    if (isAllow && matcher.find()) {
      String head = text.substring(0, matcher.start());
      String tail = text.substring(matcher.end());

      if (!head.isEmpty()) {
        currentNode.addChildren(rawPushTextNode(automatonState, currentNode, head));
      }
      if (isParagraph) {
        currentNode = currentNode.getParent();
      }
      if (!tail.isEmpty()) {
        TagNode node = new TagNode(currentNode, parserParameters, "p", " ", automatonState.getRootNode());
        currentNode.addChildren(node);
        currentNode = node;
        currentNode = pushTextNode(automatonState, currentNode, tail);
      }
    } else {
      if (isParagraphed) {
        currentNode.addChildren(rawPushTextNode(automatonState, currentNode, text));
      } else {
        currentNode.addChildren(rawPushTextNode(automatonState, currentNode, matcher.replaceAll("")));
      }
    }

    return currentNode;
  }

  private TextNode rawPushTextNode(ParserAutomatonState automatonState, Node currentNode, String text) {
    if (!automatonState.isCode()) {
      return new TextNode(currentNode, parserParameters, text, automatonState);
    } else {
      return new TextCodeNode(currentNode, parserParameters, text, automatonState);
    }
  }

  /**
   * Добавление в дерево нового узла с тэгом
   *
   * @param automatonState текущее состояние автомата
   * @param currentNode    текущий узел
   * @param name           название тэга
   * @param parameter      параметры тэга
   * @return возвращает новый текущий узел дерева
   */
  private Node pushTagNode(ParserAutomatonState automatonState, Node currentNode, String name, String parameter) {
    if (!currentNode.allows(name)) {
      Map<String, Tag> allTagsDict = parserParameters.getAllTagsDict();
      Set<String> blockLevelTags = parserParameters.getBlockLevelTags();
      Tag newTag = allTagsDict.get(name);

      if (newTag.isDiscardable()) {
        return currentNode;
      } else if (currentNode == automatonState.getRootNode()
              || blockLevelTags.contains(((TagNode) currentNode).getBbtag().getName()) && newTag.getImplicitTag() != null) {
        if (currentNode != automatonState.getRootNode() && TagNode.class.isInstance(currentNode)) {
          TagNode currentTagNode = (TagNode) currentNode;
          if ("p".equals(currentTagNode.getBbtag().getName())) {
            currentNode = currentNode.getParent();
            return pushTagNode(automatonState, currentNode, name, parameter);
          }
        }
        currentNode = pushTagNode(automatonState, currentNode, newTag.getImplicitTag(), "");
        currentNode = pushTagNode(automatonState, currentNode, name, parameter);
      } else {
        currentNode = currentNode.getParent();
        currentNode = pushTagNode(automatonState, currentNode, name, parameter);
      }
    } else {
      TagNode node = new TagNode(currentNode, parserParameters, name, parameter, automatonState.getRootNode());
      currentNode.addChildren(node);
      if (!node.getBbtag().isSelfClosing()) {
        currentNode = node;
      }
    }
    return currentNode;
  }

  /**
   * Обрабатывает закрытие тэга
   *
   * @param rootNode    корневой узел
   * @param currentNode текущий узел
   * @param name        имя закрываемого тэга
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
        if (tagName.equals(name) || ("url".equals(name) && "url2".equals(tagName))) {
          currentNode = tempNode;
          currentNode = currentNode.getParent();
          break;
        }
      }
      tempNode = tempNode.getParent();
    }
    return currentNode;
  }

  private Node processKnownTag(Node currentNode, ParserAutomatonState automatonState) {
    if (automatonState.getWholematch().startsWith("[[")) {
      currentNode = pushTextNode(automatonState, currentNode, "[");
    }

    boolean tagNameIsCode = "code".equals(automatonState.getTagname()) || "inline".equals(automatonState.getTagname());

    if (automatonState.isCloseTag()) {
      currentNode = processCloseTag(automatonState, currentNode, tagNameIsCode);
    } else {
      currentNode = processTag(automatonState, currentNode, tagNameIsCode);
    }

    if (automatonState.getWholematch().endsWith("]]")) {
      currentNode = pushTextNode(automatonState, currentNode, "]");
    }

    return currentNode;
  }

  private Node processTag(ParserAutomatonState automatonState, Node currentNode, boolean tagNameIsCode) {
    if (automatonState.isCode() && !tagNameIsCode) {
      String text = automatonState.getWholematch();

      if (text.startsWith("[[")) {
        text = text.substring(1);
      }

      if (text.endsWith("]]")) {
        text = text.substring(0, text.length()-1);
      }

      currentNode = pushTextNode(automatonState, currentNode, text);
    } else if (tagNameIsCode) {
      automatonState.setCode(true);
      automatonState.setFirstCode(true);
      currentNode = pushTagNode(automatonState, currentNode, automatonState.getTagname(), automatonState.getParameter());
    } else {
      if ("url".equals(automatonState.getTagname()) && ! StringUtils.isEmpty(automatonState.getParameter())) {
        // специальная проверка для [url] с параметром
        currentNode = pushTagNode(automatonState, currentNode, "url2", automatonState.getParameter());
      } else {
        currentNode = pushTagNode(automatonState, currentNode, automatonState.getTagname(), automatonState.getParameter());
      }
    }
    return currentNode;
  }

  private Node processEscapedTag(Node currentNode, ParserAutomatonState automatonState) {
    String textNode;
    if (automatonState.getAllTagsNames().contains(automatonState.getTagname()) && !automatonState.isCode()) {
      textNode = automatonState.getWholematch().substring(1, automatonState.getWholematch().length() - 1);
    } else {
      textNode = automatonState.getWholematch();
    }
    currentNode = pushTextNode(automatonState, currentNode, textNode);
    return currentNode;
  }

  private Node processCloseTag(ParserAutomatonState automatonState, Node currentNode, boolean tagNameIsCode) {
    if (!automatonState.isCode() || tagNameIsCode) {
      currentNode = closeTagNode(automatonState.getRootNode(), currentNode, automatonState.getTagname());
    } else {
      currentNode = pushTextNode(automatonState, currentNode, automatonState.getWholematch());
    }
    if (tagNameIsCode) {
      automatonState.setCode(false);
    }
    return currentNode;
  }

  private Node trimNewLine(ParserAutomatonState automatonState, Node currentNode, String bbcode, Matcher match) {
    String fixWhole = bbcode.substring(automatonState.getPos(), match.start());
    if (fixWhole.startsWith("\n")) {
      fixWhole = fixWhole.substring(1); // откусить ведущий перевод строки
    } else if (fixWhole.startsWith("\r\n")) {
      fixWhole = fixWhole.substring(2); // откусить ведущий перевод строки
    }
    automatonState.setFirstCode(false);
    return pushTextNode(automatonState, currentNode, fixWhole);
  }

  public class ParserAutomatonState {
    private final RootNode rootNode;
    private final Set<String> allTagsNames;

    private int pos = 0;
    private boolean isCode = false;
    private boolean firstCode = false;

    private final RuTypoChanger changer = new RuTypoChanger();

    private String tagname;
    private String parameter;
    private String wholematch;

    private ParserAutomatonState(RootNode rootNode, ParserParameters parserParameters) {
      this.rootNode = rootNode;
      allTagsNames = parserParameters.getAllTagsNames();
    }

    private void processTagMatcher(Matcher match) {
      tagname = match.group(1).toLowerCase();
      parameter = match.group(3);
      wholematch = match.group(0);

      if (!StringUtils.isEmpty(parameter)){
        parameter = parameter.substring(1);
      }
    }

    private boolean isTagEscaped() {
      return wholematch.startsWith("[[") && wholematch.endsWith("]]");
    }

    private boolean isCloseTag() {
      return wholematch.startsWith("[/") || wholematch.startsWith("[[/");
    }

    private int getPos() {
      return pos;
    }

    private void setPos(int pos) {
      this.pos = pos;
    }

    private boolean isCode() {
      return isCode;
    }

    private void setCode(boolean code) {
      isCode = code;
    }

    private boolean isFirstCode() {
      return firstCode;
    }

    private void setFirstCode(boolean firstCode) {
      this.firstCode = firstCode;
    }

    private String getTagname() {
      return tagname;
    }

    private String getParameter() {
      return parameter;
    }

    private String getWholematch() {
      return wholematch;
    }

    public RootNode getRootNode() {
      return rootNode;
    }

    private Set<String> getAllTagsNames() {
      return allTagsNames;
    }

    public RuTypoChanger getTypoChanger() {
      return changer;
    }
  }
}
