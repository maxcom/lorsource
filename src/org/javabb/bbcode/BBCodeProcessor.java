/*
 * Copyright 1998-2009 Linux.org.ru
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

package org.javabb.bbcode;

/*
 * Copyright 2004 JavaFree.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.org.linux.util.HTMLFormatter;

/**
 * $Id: BBCodeProcessor.java,v 1.1 2005/02/07 03:16:07 ronaldtm Exp $
 *
 * @author Ronald Tetsuo Miura
 */
public class BBCodeProcessor implements Serializable {
  private static final String CR_LF = "(?:\r\n|\r|\n)?";

  private static final RegexTag[] REGEX_TAGS = new RegexTag[]{
      new SimpleRegexTag("", "(\r\n\r\n|\n\r\n\r|\n\n|\r\r)", "<p>"),
//        new SimpleRegexTag("color",
//            "\\[color=['\"]?(.*?[^'\"])['\"]?\\](.*?)\\[/color\\]",
//            "<span style='color:$1px'>$2</span>"),
//        new SimpleRegexTag("size",
//            "\\[size=['\"]?([0-9]|[1-2][0-9])['\"]?\\](.*?)\\[/size\\]",
//            "<span style='font-size:$1px'>$2</span>"),
      new SimpleRegexTag("b", "\\[b\\](.*?)\\[/b\\]", "<b>$1</b>"),
      new SimpleRegexTag("em", "\\[em\\](.*?)\\[/em\\]", "<em>$1</em>"),
      new SimpleRegexTag("s", "\\[s\\](.*?)\\[/s\\]", "<del>$1</del>"),
      new SimpleRegexTag("strong", "\\[strong\\](.*?)\\[/strong\\]", "<strong>$1</strong>"),
      new SimpleRegexTag("u", "\\[u\\](.*?)\\[/u\\]", "<u>$1</u>"),
      new SimpleRegexTag("i", "\\[i\\](.*?)\\[/i\\]", "<i>$1</i>"),
//        new SimpleRegexTag("img", "\\[img\\](.*?)\\[/img\\]", "<img src='$1' border='0' alt=''>"),
      new URLTag("url", "\\[url\\](.*?)\\[/url\\]", "<a href=\"$1\">$1</a>"),
      new UserTag("user", "\\[user\\](.*?)\\[/user\\]"),
      new URLTag("url",
          "\\[url=['\"]?(.*?[^'\"])['\"]?\\](.*?)\\[/url\\]",
          "<a href=\"$1\">$2</a>")//,
//      new SimpleRegexTag("email", "\\[email\\](.*?)\\[/email\\]", "<a href='mailto:$1'>$1</a>", true)
    };


  /**
   * @param texto
   * @return TODO unuseful parameters.
   */
  public String preparePostText(Connection db, String texto) throws SQLException {
    texto = HTMLFormatter.htmlSpecialChars(texto);

    return process(db, texto).toString();
  }

  /**
   * @param string
   * @return HTML-formated message
   */
  private static CharSequence process(Connection db, String string) throws SQLException {
    StringBuffer buffer = new StringBuffer("<p>"+string);
    new CodeTag().processContent(buffer);

    CharSequence data = processNestedTags(buffer,
        "quote",
        "<div class=\"quote\"><h3>{BBCODE_PARAM}</h3><p>",
        "</div>",
        "<div class=\"quote\"><h3>Цитата</h3><p>",
        "</div>",
        "[*]",
        false,
        true,
        true);

    data = processNestedTags(data,
        "list",
        "<ol type=\"{BBCODE_PARAM}\">",
        "</ol>",
        "<ul>",
        "</ul>",
        "<li>",
        true,
        true,
        true);

    for (RegexTag tag : REGEX_TAGS) {
      StringBuffer sb2 = new StringBuffer((int) (buffer.length() * 1.5));

      tag.substitute(db, data, sb2, tag, tag.getReplacement());
      data = sb2;
    }

    return data;
  }

  /**
   * @param input
   * @param tagName
   * @param openSubstWithParam
   * @param closeSubstWithParam
   * @param openSubstWithoutParam
   * @param closeSubstWithoutParam
   * @param internalSubst
   * @param processInternalTags
   * @param acceptParam
   * @param requiresQuotedParam
   */
  private static CharSequence processNestedTags(
      CharSequence input,
      String tagName,
      String openSubstWithParam,
      String closeSubstWithParam,
      String openSubstWithoutParam,
      String closeSubstWithoutParam,
      String internalSubst,
      boolean processInternalTags,
      boolean acceptParam,
      boolean requiresQuotedParam) {
    Stack<BBChunk> openStack = new Stack<BBChunk>();
    Set<BBChunk> subsOpen = new HashSet<BBChunk>();
    Set<BBChunk> subsClose = new HashSet<BBChunk>();
    Set<BBChunk> subsInternal = new HashSet<BBChunk>();

    String openTag = CR_LF + "\\["
        + tagName
        + (acceptParam ? (requiresQuotedParam ? "(?:=\\&quot;(.*?)\\&quot;)?" : "(?:=?:(\\&quot;)?(.*?)?:(\\&quot;)?)?") : "")
        + "\\]"
        + CR_LF;
    String closeTag = CR_LF + "\\[/" + tagName + "\\]" + CR_LF;

    String patternString = '(' + openTag + ")|(" + closeTag + ')';

    if (processInternalTags) {
      String internTag = CR_LF + "\\[\\*\\]" + CR_LF;
      patternString += "|(" + internTag + ')';
    }

    Pattern tagsPattern = Pattern.compile(patternString);
    Matcher matcher = tagsPattern.matcher(input);

    int paramGroup;
    int closeTagGroup;
    int internalTagGroup;

    if (acceptParam) {
      paramGroup = 2;
      closeTagGroup = 3;
      internalTagGroup = 4;
    } else {
      paramGroup = -1; // INFO
      closeTagGroup = 2;
      internalTagGroup = 3;
    }

    int openTagGroup = 1;
    String str = input.toString();

    while (matcher.find()) {
      int length = matcher.end() - matcher.start();
      BBChunk matchedSeq = new BBChunk(matcher.start(), length);

      // test opening tags
      if (matcher.group(openTagGroup) != null) {
        if (acceptParam && (matcher.group(paramGroup) != null)) {
          matchedSeq.param = matcher.group(paramGroup);
        }

        openStack.push(matchedSeq);

        // test closing tags
      } else if ((matcher.group(closeTagGroup) != null) && !openStack.isEmpty()) {
        BBChunk openSeq = openStack.pop();

        if (acceptParam) {
          matchedSeq.param = openSeq.param;
        }

        subsOpen.add(openSeq);
        subsClose.add(matchedSeq);

        // test internal tags
      } else if (processInternalTags && (matcher.group(internalTagGroup) != null)
          && (!openStack.isEmpty())) {
        subsInternal.add(matchedSeq);
      }
    }

    List<BBChunk> subst = new LinkedList<BBChunk>();
    subst.addAll(subsOpen);
    subst.addAll(subsClose);
    subst.addAll(subsInternal);

    Collections.sort(subst, new Comparator<BBChunk>() {
      @Override
      public int compare(BBChunk o1, BBChunk o2) {
        return (o1.start - o2.start);
      }
    });

    StringBuffer output = new StringBuffer();

    int start = 0;
    boolean textAllowed = true;

    for (BBChunk seq : subst) {
      if (textAllowed) {
        output.append(str.substring(start, seq.start));
      }

      if (subsClose.contains(seq)) {
        textAllowed = true;

        if (seq.param != null) {
          output.append(closeSubstWithParam);
        } else {
          output.append(closeSubstWithoutParam);
        }
      } else if (subsInternal.contains(seq)) {
        textAllowed = true;
        output.append(internalSubst);
      } else if (subsOpen.contains(seq)) {
        textAllowed = !processInternalTags;

        Matcher m = Pattern.compile(openTag).matcher(str.substring(seq.start,
            seq.start + seq.length));

        if (m.matches()) {
          if (acceptParam && (seq.param != null)) {
            output.append(openSubstWithParam.replaceAll("\\{BBCODE_PARAM\\}", seq.param));
          } else {
            output.append(openSubstWithoutParam);
          }
        }
      }

      start = seq.start + seq.length;
    }

    output.append(str.substring(start));

    return output;
  }

  private static class BBChunk {
    private final int start;
    private final int length;
    private String param = null;

    public BBChunk(int start, int length) {
      this.start = start;
      this.length = length;
    }

    public int length() {
      return length;
    }
  }
}
