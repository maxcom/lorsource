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

import ru.org.linux.util.StringUtil;
import ru.org.linux.util.bbcode.nodes.Node;
import ru.org.linux.util.bbcode.nodes.RootNode;
import ru.org.linux.util.bbcode.nodes.TagNode;
import ru.org.linux.util.bbcode.nodes.TextNode;
import ru.org.linux.util.bbcode.tags.Tag;

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


  public Parser(ParserParameters parserParameters) {
    this.parserParameters = parserParameters;
  }

  public static String escape(String html) {
    return StringUtil.escapeHtml(html);
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
        //currentNode.getChildren().add(new TextNode(currentNode, this, text));
      } else {
        if (currentNode.allows("p")) {
          currentNode.getChildren().add(new TagNode(currentNode, parserParameters, "p", "", rootNode));
          currentNode = descend(currentNode);
        } else if (currentNode.allows("div")) {
          currentNode.getChildren().add(new TagNode(currentNode, parserParameters, "div", "", rootNode));
          currentNode = descend(currentNode);
        } else {
          currentNode = ascend(currentNode);
        }
        currentNode = pushTextNode(rootNode, currentNode, text);
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
        if(isAllow){
          if(matcher.start() != 0){
            currentNode = pushTextNode(rootNode, currentNode, text.substring(0, matcher.start()));
          }
          if (isParagraph) {
            currentNode = ascend(currentNode);
          }
          if(matcher.end() != text.length()){
            currentNode.getChildren().add(new TagNode(currentNode, parserParameters, "p", " ", rootNode));
            currentNode = descend(currentNode);
            currentNode = pushTextNode(rootNode, currentNode, text.substring(matcher.end()));
          }
        } else if (!isParagraphed) {
          if(matcher.start() != 0){
            currentNode.getChildren().add(new TextNode(currentNode, parserParameters, text.substring(0, matcher.start()), rootNode));
          }
          if(matcher.end() != text.length()){
            currentNode.getChildren().add(new TextNode(currentNode, parserParameters, text.substring(matcher.end()), rootNode));
          }

        }else{
          currentNode.getChildren().add(new TextNode(currentNode, parserParameters, text, rootNode));
        }
      } else {
        currentNode.getChildren().add(new TextNode(currentNode, parserParameters, text, rootNode));
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
      Map<String, Tag> allTagsDict = parserParameters.getAllTagsDict();
      Set<String> blockLevelTags = parserParameters.getBlockLevelTags();
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
      TagNode node = new TagNode(currentNode, parserParameters, name, parameter, rootNode);
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

  public RootNode getRootNode() {
    return new RootNode(parserParameters);
  }

  /**
   * Точка входа для разбора LORCODE
   *
   * @param rootNode корневой узел новго дерева
   * @param text обрабатываемы LORCODE
   * @return возвращает инвалидный html
   */
  public RootNode parseRoot(RootNode rootNode, String text) {
    return parse(rootNode, text);
  }

  private RootNode parse(RootNode rootNode, String bbcode) {
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
          Set<String> allTagsNames = parserParameters.getAllTagsNames();
          if (allTagsNames.contains(tagname)) {
            if (wholematch.startsWith("[[")) {
              currentNode = pushTextNode(rootNode, currentNode, "[");
            }


            if (wholematch.startsWith("[/") || wholematch.startsWith("[[/")) {
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
}
