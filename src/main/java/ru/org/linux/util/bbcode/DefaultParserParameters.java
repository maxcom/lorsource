/*
 * Copyright 1998-2012 Linux.org.ru
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

package ru.org.linux.util.bbcode;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import ru.org.linux.util.bbcode.tags.*;

import java.util.*;

public class DefaultParserParameters implements ParserParameters{
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
   * Тэги в нутри которых не работает двойной перевод строк
   */
  private final ImmutableSet<String> disallowedParagraphTags;

  /**
   * Тэги внутри которых двойной перенос не работает и остается
   * двойным переносом
   */
  private final ImmutableSet<String> paragraphedTags;

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

  public DefaultParserParameters() {
    allowedListParameters = ImmutableSet.of("A", "a", "I", "i", "1");
    inlineTags = ImmutableSet.of("b", "i", "u", "s", "em", "strong", "url", "url2", "user", "br", "text", "img", "softbr");
    urlTags = ImmutableSet.of("b", "i", "u", "s", "strong", "text");
    blockLevelTags = ImmutableSet.of("p", "quote", "list", "pre", "code", "div", "cut");
    autoLinkTags = ImmutableSet.of("b", "i", "u", "s", "em", "strong", "p", "quote", "div", "cut", "pre", "*");
    disallowedParagraphTags = ImmutableSet.of("pre", "url", "user", "code");
    paragraphedTags = ImmutableSet.of("pre", "code");
    flowTags = new Builder<String>()
            .addAll(inlineTags)
            .addAll(blockLevelTags)
            .build();

    otherTags = ImmutableSet.of("*");

    allTags = new ArrayList<>();
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
      CutTag tag = new CutTag("cut", blockLevelTags, "div", this);
      tag.setHtmlEquiv("div");
      allTags.add(tag);
    }
    { //  <li>
      LiTag tag = new LiTag("*", flowTags, "list", this);
      allTags.add(tag);
    }

    allTagsDict = new HashMap<>();
    for (Tag tag : allTags) {
      if (!"text".equals(tag.getName())) {
        allTagsDict.put(tag.getName(), tag);
      }
    }
    Builder<String> allTagsBuilder = new Builder<>();
    for (Tag tag : allTags) {
      allTagsBuilder.add(tag.getName());
    }
    allTagsNames = allTagsBuilder.build();
  }

  @Override
  public Set<String> getAllowedListParameters() {
    return allowedListParameters;
  }

  @Override
  public Set<String> getBlockLevelTags() {
    return blockLevelTags;
  }

  @Override
  public Map<String, Tag> getAllTagsDict() {
    return allTagsDict;
  }

  @Override
  public Set<String> getAllTagsNames() {
    return allTagsNames;
  }

  @Override
  public Set<String> getAutoLinkTags() {
    return autoLinkTags;
  }

  @Override
  public Set<String> getDisallowedParagraphTags() {
    return disallowedParagraphTags;
  }

  @Override
  public Set<String> getParagraphedTags() {
    return paragraphedTags;
  }
}
