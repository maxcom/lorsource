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

import org.apache.commons.lang.StringUtils;
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
  /**
   * Регулярное выражение поиска тэга
   */
  public static final Pattern BBTAG_REGEXP = Pattern.compile("\\[\\[?/?([A-Za-z\\*]+)(:[a-f0-9]+)?(=[^\\]]+)?\\]?\\]");

  /**
   * Регулярное выражения поиска двойного перевода строки
   */
  public static final Pattern P_REGEXP = Pattern.compile("(\r?\n){2,}");

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

  public RootNode getRootNode() {
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
   * @return возвращает новй текущий узел
   */
  private Node pushTextNode(ParserAutomatonState automatonState, Node currentNode, String text) {
    if (!currentNode.allows("text")) {
      if (text.trim().isEmpty()) {
        //currentNode.getChildren().add(new TextNode(currentNode, this, text));
      } else {
        if (currentNode.allows("p")) {
          currentNode.getChildren().add(new TagNode(currentNode, parserParameters, "p", "", automatonState.getRootNode()));
          currentNode = descend(currentNode);
        } else if (currentNode.allows("div")) {
          currentNode.getChildren().add(new TagNode(currentNode, parserParameters, "div", "", automatonState.getRootNode()));
          currentNode = descend(currentNode);
        } else {
          currentNode = ascend(currentNode);
        }
        currentNode = pushTextNode(automatonState, currentNode, text);
      }
    } else {
      Matcher matcher = P_REGEXP.matcher(text);

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
       * Если мы находим двойной пеернос строки и в тексте
       * и в текущем тэге разрешена вставка нового тэга p -
       * вставляем p
       * за исключеним, если текущий тэг p, тогда поднимаемся на уровень
       * выше в дереве и вставляем p с текстом
       */
      if (matcher.find()) {
        if (isAllow) {
          if (matcher.start() != 0) {
            currentNode = pushTextNode(automatonState, currentNode, text.substring(0, matcher.start()));
          }
          if (isParagraph) {
            currentNode = ascend(currentNode);
          }
          if (matcher.end() != text.length()) {
            currentNode.getChildren().add(new TagNode(currentNode, parserParameters, "p", " ", automatonState.getRootNode()));
            currentNode = descend(currentNode);
            currentNode = pushTextNode(automatonState, currentNode, text.substring(matcher.end()));
          }
        } else if (!isParagraphed) {
          if (matcher.start() != 0) {
            rawPushTextNode(automatonState, currentNode, text.substring(0, matcher.start()));
          }
          if (matcher.end() != text.length()) {
            rawPushTextNode(automatonState, currentNode, text.substring(matcher.end()));
          }
        } else {
          rawPushTextNode(automatonState, currentNode, text);
        }
      } else {
        rawPushTextNode(automatonState, currentNode, text);
      }
    }
    return currentNode;
  }

  private void rawPushTextNode(ParserAutomatonState automatonState, Node currentNode, String text) {
    if (!automatonState.isCode()) {
      text = automatonState.getTypoChanger().format(text);
      currentNode.getChildren().add(new TextNode(currentNode, parserParameters, text, automatonState.getRootNode()));
    } else {
      currentNode.getChildren().add(new TextCodeNode(currentNode, parserParameters, text, automatonState.getRootNode()));
    }
  }

  /**
   * Сдвигает текущий узед в дереве на уровень ниже текущего узла
   *
   * @param currentNode текщуий узел
   * @return новый текущий узел
   */
  private Node descend(Node currentNode) {
    return currentNode.getChildren().get(currentNode.getChildren().size() - 1);
  }

  /**
   * Сдвигает текущий узел на уровень выше текущего узла
   *
   * @param currentNode текущий узел
   * @return новый текущий узел
   */
  private Node ascend(Node currentNode) {
    return currentNode.getParent();
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
            currentNode = ascend(currentNode);
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
      currentNode.getChildren().add(node);
      if (!node.getBbtag().isSelfClosing()) {
        currentNode = descend(currentNode);
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
          currentNode = ascend(currentNode);
          break;
        }
      }
      tempNode = tempNode.getParent();
    }
    return currentNode;
  }

  /**
   * @param currentNode
   * @param automatonState
   * @return
   */
  private Node processKnownTag(Node currentNode, ParserAutomatonState automatonState) {
    if (automatonState.getWholematch().startsWith("[[")) {
      currentNode = pushTextNode(automatonState, currentNode, "[");
    }

    boolean tagNameIsCode = "code".equals(automatonState.getTagname()) || "inline".equals(automatonState.getTagname());

    if (automatonState.isCloseTag(automatonState)) {
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
      currentNode = pushTextNode(automatonState, currentNode, automatonState.getWholematch());
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

  /**
   *
   */
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

    public ParserAutomatonState(RootNode rootNode, ParserParameters parserParameters) {
      this.rootNode = rootNode;
      allTagsNames = parserParameters.getAllTagsNames();
    }

    public void processTagMatcher(Matcher match) {
      tagname = match.group(1).toLowerCase();
      parameter = match.group(3);
      wholematch = match.group(0);

      if (!StringUtils.isEmpty(parameter)){
        parameter = parameter.substring(1);
      }
    }

    public boolean isTagEscaped() {
      return wholematch.startsWith("[[") && wholematch.endsWith("]]");
    }

    public boolean isCloseTag(ParserAutomatonState automatonState) {
      return wholematch.startsWith("[/") || wholematch.startsWith("[[/");
    }

    public int getPos() {
      return pos;
    }

    public void setPos(int pos) {
      this.pos = pos;
    }

    public boolean isCode() {
      return isCode;
    }

    public void setCode(boolean code) {
      isCode = code;
    }

    public boolean isFirstCode() {
      return firstCode;
    }

    public void setFirstCode(boolean firstCode) {
      this.firstCode = firstCode;
    }

    public String getTagname() {
      return tagname;
    }

    public void setTagname(String tagname) {
      this.tagname = tagname;
    }

    public String getParameter() {
      return parameter;
    }

    public void setParameter(String parameter) {
      this.parameter = parameter;
    }

    public String getWholematch() {
      return wholematch;
    }

    public void setWholematch(String wholematch) {
      this.wholematch = wholematch;
    }

    public RootNode getRootNode() {
      return rootNode;
    }

    public Set<String> getAllTagsNames() {
      return allTagsNames;
    }

    public RuTypoChanger getTypoChanger() {
      return changer;
    }
  }
}
