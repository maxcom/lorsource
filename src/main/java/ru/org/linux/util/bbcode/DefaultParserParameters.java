/*
 * Copyright 1998-2014 Linux.org.ru
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import ru.org.linux.util.bbcode.tags.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultParserParameters implements ParserParameters{
  /**
   * Множество тэгов которое содержат или текст или себе подобных
   */
  private final static ImmutableSet<String> INLINE_TAGS = ImmutableSet.of(
          "b", "i", "u", "s", "em", "strong", "url", "url2", "user", "br", "text", "softbr", "inline"
  );

  /**
   * Множество тэгов которым разрешено присутствовать в тэге url с параметром, вида [url=http://some]....[/url]
   */
  private final static ImmutableSet<String> URL_TAGS = ImmutableSet.of("b", "i", "u", "s", "strong", "text");

  /**
   * Множество тэгов которые могут содержать любые тэги
   */
  private final static ImmutableSet<String> BLOCK_LEVEL_TAGS = ImmutableSet.of(
          "p", "quote", "list", "pre", "code", "div", "cut"
  );

  /**
   * Все тэги из INLINE_TAGS и BLOCK_LEVEL_TAGS
   */
  private final static ImmutableSet<String> FLOW_TAGS = new Builder<String>()
          .addAll(INLINE_TAGS)
          .addAll(BLOCK_LEVEL_TAGS)
          .build();

  /**
   * Тэги внутри которых работает автовыделение ссылок
   */
  private final static ImmutableSet<String> AUTO_LINK_TAGS = ImmutableSet.of(
          "b", "i", "u", "s", "em", "strong", "p", "quote", "div", "cut", "pre", "*"
  );

  /**
   * Разрешенные параметры для тэга list
   */
  private final static ImmutableSet<String> ALLOWED_LIST_PARAMS = ImmutableSet.of("A", "a", "I", "i", "1");

  /**
   * Тэги внутри которых не работает двойной перевод строк
   */
  private final static ImmutableSet<String> DISALLOWED_PARAGRAPH_TAGS = ImmutableSet.of("pre", "url", "user", "code");

  /**
   * Тэги внутри которых двойной перенос не работает и остается
   * двойным переносом
   */
  private final static ImmutableSet<String> PARAGRAPHED_TAGS = ImmutableSet.of("pre", "code");

  /**
   * Хэш соответствия имя тэга -> класс тэга
   */
  private final ImmutableMap<String, Tag> allTagsDict;

  /**
   * Множество всех имен тэгов
   */
  private final ImmutableSet<String> allTagsNames;

  public DefaultParserParameters() {
    List<Tag> allTags = new ArrayList<>();
    { // <br/>
      HtmlEquivTag tag = new HtmlEquivTag("br", ImmutableSet.<String>of(), "p", this, "br");
      tag.setSelfClosing(true);
      //tag.setDiscardable(true);
      allTags.add(tag);
    }
    { // <br/>, but can adapt during render ?
      SoftBrTag tag = new SoftBrTag(ImmutableSet.<String>of(), this);
      tag.setSelfClosing(true);
      tag.setDiscardable(true);
      allTags.add(tag);
    }
    { // <b>
      HtmlEquivTag tag = new HtmlEquivTag("b", INLINE_TAGS, "p", this, "b");
      allTags.add(tag);
    }
    { // <i>
      HtmlEquivTag tag = new HtmlEquivTag("i", INLINE_TAGS, "p", this, "i");
      allTags.add(tag);
    }
    { // <u> TODO Allert: The U tag has been deprecated in favor of the text-decoration style property.
      HtmlEquivTag tag = new HtmlEquivTag("u", INLINE_TAGS, "p", this, "u");
      allTags.add(tag);
    }
    { // <s> TODO Allert: The S tag has been deprecated in favor of the text-decoration style property.
      HtmlEquivTag tag = new HtmlEquivTag("s", INLINE_TAGS, "p", this, "s");
      allTags.add(tag);
    }
    { // <em>
      HtmlEquivTag tag = new HtmlEquivTag("em", INLINE_TAGS, "p", this, "em");
      allTags.add(tag);
    }
    { // <strong>
      HtmlEquivTag tag = new HtmlEquivTag("strong", INLINE_TAGS, "p", this, "strong");
      allTags.add(tag);
    }
    { // <a>
      UrlTag tag = new UrlTag(ImmutableSet.of("text"), this);
      allTags.add(tag);
    }
    { // <a> специальный случай с парамтром
      UrlWithParamTag tag = new UrlWithParamTag(URL_TAGS, this);
      allTags.add(tag);
    }
    { // <a> member
      MemberTag tag = new MemberTag(ImmutableSet.of("text"), this);
      allTags.add(tag);
    }
    { // <p>
      HtmlEquivTag tag = new HtmlEquivTag("p", FLOW_TAGS, null, this, "p", ImmutableSet.of("div", "list", "quote", "cut"));
      allTags.add(tag);
    }
    { // <div>
      HtmlEquivTag tag = new HtmlEquivTag("div", BLOCK_LEVEL_TAGS, null, this, "");
      allTags.add(tag);
    }
    { // <blockquote>
      QuoteTag tag = new QuoteTag(BLOCK_LEVEL_TAGS, this);
      allTags.add(tag);
    }
    { // <ul>
      ListTag tag = new ListTag(ImmutableSet.of("*", "softbr"), this);
      allTags.add(tag);
    }
    { // <pre>
      HtmlEquivTag tag = new HtmlEquivTag("pre", INLINE_TAGS, "div", this, "pre");
      allTags.add(tag);
    }
    { // <pre class="code">
      CodeTag tag = new CodeTag("code", INLINE_TAGS, "div", this);
      allTags.add(tag);
    }
    {
      InlineTag tag = new InlineTag(INLINE_TAGS, this);
      allTags.add(tag);
    }
    {   // [cut]
      CutTag tag = new CutTag(BLOCK_LEVEL_TAGS, this);
      allTags.add(tag);
    }
    { //  <li>
      LiTag tag = new LiTag(FLOW_TAGS, this);
      allTags.add(tag);
    }

    ImmutableMap.Builder<String, Tag> dictBuilder = ImmutableMap.builder();
    for (Tag tag : allTags) {
      if (!"text".equals(tag.getName())) {
        dictBuilder.put(tag.getName(), tag);
      }
    }
    allTagsDict = dictBuilder.build();

    Builder<String> allTagsBuilder = new Builder<>();
    for (Tag tag : allTags) {
      allTagsBuilder.add(tag.getName());
    }
    allTagsNames = allTagsBuilder.build();
  }

  @Override
  public Set<String> getAllowedListParameters() {
    return ALLOWED_LIST_PARAMS;
  }

  @Override
  public Set<String> getBlockLevelTags() {
    return BLOCK_LEVEL_TAGS;
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
    return AUTO_LINK_TAGS;
  }

  @Override
  public Set<String> getDisallowedParagraphTags() {
    return DISALLOWED_PARAGRAPH_TAGS;
  }

  @Override
  public Set<String> getParagraphedTags() {
    return PARAGRAPHED_TAGS;
  }
}
