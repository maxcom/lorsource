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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.org.linux.util.BadURLException;
import ru.org.linux.util.HTMLFormatter;
import ru.org.linux.util.URLUtil;

/**
 * $Id: BBCodeProcessor.java,v 1.1 2005/02/07 03:16:07 ronaldtm Exp $
 *
 * @author Ronald Tetsuo Miura
 */
public class BBCodeProcessor implements Serializable {
  private static final String CR_LF = "(?:\r\n|\r|\n)?";

  private static final RegexTag[] REGEX_TAGS = new RegexTag[]{
      new SimpleRegexTag("", "(\r\n\r\n|\n\r\n\r|\n\n|\r\r)", "<br>", false),
//        new SimpleRegexTag("color",
//            "\\[color=['\"]?(.*?[^'\"])['\"]?\\](.*?)\\[/color\\]",
//            "<span style='color:$1px'>$2</span>"),
//        new SimpleRegexTag("size",
//            "\\[size=['\"]?([0-9]|[1-2][0-9])['\"]?\\](.*?)\\[/size\\]",
//            "<span style='font-size:$1px'>$2</span>"),
      new SimpleRegexTag("b", "\\[b\\](.*?)\\[/b\\]", "<b>$1</b>", false),
      new SimpleRegexTag("u", "\\[u\\](.*?)\\[/u\\]", "<u>$1</u>", false),
      new SimpleRegexTag("i", "\\[i\\](.*?)\\[/i\\]", "<i>$1</i>", false),
//        new SimpleRegexTag("img", "\\[img\\](.*?)\\[/img\\]", "<img src='$1' border='0' alt=''>"),
      new SimpleRegexTag("url", "\\[url\\](.*?)\\[/url\\]", "<a href='$1'>$1</a>", true),
      new SimpleRegexTag("user", "\\[user\\](.*?)\\[/user\\]", "<img src=\"http://www.linux.org.ru/favicon.ico\"><a style=\"text-decoration: none\" href='http://www.linux.org.ru/whois.jsp?nick=$1'>$1</a>", false),
      new SimpleRegexTag("url",
          "\\[url=['\"]?(.*?[^'\"])['\"]?\\](.*?)\\[/url\\]",
          "<a href=\"$1\" target=\"_new\">$2</a>", true),
      new SimpleRegexTag("email", "\\[email\\](.*?)\\[/email\\]", "<a href='mailto:$1'>$1</a>", true)};

  /** */
  private boolean acceptHTML = false;

  /** */
  private boolean acceptBBCode = true;

  /**
   * @return acceptBBCode
   */
  public boolean isAcceptBBCode() {
    return acceptBBCode;
  }

  /**
   * @param acceptBBCode the new acceptBBCode value
   */
  public void setAcceptBBCode(boolean acceptBBCode) {
    this.acceptBBCode = acceptBBCode;
  }

  /**
   * @return htmlAccepted
   */
  public boolean isAcceptHTML() {
    return acceptHTML;
  }

  /**
   * @param acceptHTML the new acceptHTML value
   */
  public void setAcceptHTML(boolean acceptHTML) {
    this.acceptHTML = acceptHTML;
  }

  /**
   * @param texto
   * @return TODO unuseful parameters.
   */
  public String preparePostText(String texto) throws BadURLException {
    if (!isAcceptHTML()) {
      texto = HTMLFormatter.htmlSpecialChars(texto);
    }
    if (isAcceptBBCode()) {
      texto = process(texto);
    }
    return texto;
  }

  /**
   * @param string
   * @return HTML-formated message
   */
  private String process(String string) throws BadURLException {
    StringBuffer buffer = new StringBuffer(string);
    new CodeTag().processContent(buffer);

    processNestedTags(buffer,
        "quote",
        "<div class=\"quote\"><h3>{BBCODE_PARAM}</h3>",
        "</div>",
        "<div class=\"quote\"><h3>Цитата</h3>",
        "</div>",
        "[*]",
        false,
        true,
        true);

    processNestedTags(buffer,
        "list",
        "<ol type=\"{BBCODE_PARAM}\">",
        "</ol>",
        "<ul>",
        "</ul>",
        "<li>",
        true,
        true,
        true);

    StringBuffer sb1 = buffer;
    StringBuffer sb2 = new StringBuffer((int) (buffer.length() * 1.5));

    for (RegexTag tag : REGEX_TAGS) {
      substitute(sb1, sb2, tag, tag.getReplacement());
      StringBuffer temp = sb1;
      sb1 = sb2;
      sb2 = temp;
    }

    return sb1.toString();
  }

  /**
   * @param from
   * @param to
   * @param regex       TODO
   * @param replacement TODO
   */
  private void substitute(CharSequence from, StringBuffer to, RegexTag regex, String replacement) throws BadURLException {
    to.setLength(0);

    Pattern p = regex.getRegex();
    Matcher m = p.matcher(from);
    while (m.find()) {
      String value = m.group(1);
      if (regex.isUrl()) {
        if (!URLUtil.isUrl(value)) {
          throw new BadURLException(value);
        }
      }

      m.appendReplacement(to, replacement);
    }
    m.appendTail(to);
  }

  /**
   * @param buffer
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
  private static void processNestedTags(
      StringBuffer buffer,
      String tagName,
      String openSubstWithParam,
      String closeSubstWithParam,
      String openSubstWithoutParam,
      String closeSubstWithoutParam,
      String internalSubst,
      boolean processInternalTags,
      boolean acceptParam,
      boolean requiresQuotedParam) {
    String str = buffer.toString();

    Stack<MutableCharSequence> openStack = new Stack<MutableCharSequence>();
    Set<MutableCharSequence> subsOpen = new HashSet<MutableCharSequence>();
    Set<MutableCharSequence> subsClose = new HashSet<MutableCharSequence>();
    Set<MutableCharSequence> subsInternal = new HashSet<MutableCharSequence>();

    String openTag = CR_LF + "\\["
        + tagName
        + (acceptParam ? (requiresQuotedParam ? "(?:=\\&quot;(.*?)\\&quot;)?" : "(?:=?:(\\&quot;)?(.*?)?:(\\&quot;)?)?") : "")
        + "\\]"
        + CR_LF;
    String closeTag = CR_LF + "\\[/" + tagName + "\\]" + CR_LF;

    String patternString = "(" + openTag + ")|(" + closeTag + ")";

    if (processInternalTags) {
      String internTag = CR_LF + "\\[\\*\\]" + CR_LF;patternString += "|(" + internTag + ")";
    }

    Pattern tagsPattern = Pattern.compile(patternString);
    Matcher matcher = tagsPattern.matcher(str);

    int openTagGroup;
    int paramGroup;
    int closeTagGroup;
    int internalTagGroup;

    if (acceptParam) {
      openTagGroup = 1;
      paramGroup = 2;
      closeTagGroup = 3;
      internalTagGroup = 4;
    } else {
      openTagGroup = 1;
      paramGroup = -1; // INFO
      closeTagGroup = 2;
      internalTagGroup = 3;
    }

    while (matcher.find()) {
      int length = matcher.end() - matcher.start();
      MutableCharSequence matchedSeq = new MutableCharSequence(str, matcher.start(), length);

      // test opening tags
      if (matcher.group(openTagGroup) != null) {
        if (acceptParam && (matcher.group(paramGroup) != null)) {
          matchedSeq.param = matcher.group(paramGroup);
        }

        openStack.push(matchedSeq);

        // test closing tags
      } else if ((matcher.group(closeTagGroup) != null) && !openStack.isEmpty()) {
        MutableCharSequence openSeq = openStack.pop();

        if (acceptParam) {
          matchedSeq.param = openSeq.param;
        }

        subsOpen.add(openSeq);
        subsClose.add(matchedSeq);

        // test internal tags
      } else if (processInternalTags && (matcher.group(internalTagGroup) != null)
          && (!openStack.isEmpty())) {
        subsInternal.add(matchedSeq);
      } else {
        // assert (false);
      }
    }

    LinkedList<MutableCharSequence> subst = new LinkedList<MutableCharSequence>();
    subst.addAll(subsOpen);
    subst.addAll(subsClose);
    subst.addAll(subsInternal);

    Collections.sort(subst, new Comparator<MutableCharSequence>() {
      public int compare(MutableCharSequence o1, MutableCharSequence o2) {
        return -(o1.start - o2.start);
      }
    });

    buffer.delete(0, buffer.length());

    int start = 0;

    while (!subst.isEmpty()) {
      MutableCharSequence seq = subst.removeLast();
      buffer.append(str.substring(start, seq.start));

      if (subsClose.contains(seq)) {
        if (seq.param != null) {
          buffer.append(closeSubstWithParam);
        } else {
          buffer.append(closeSubstWithoutParam);
        }
      } else if (subsInternal.contains(seq)) {
        buffer.append(internalSubst);
      } else if (subsOpen.contains(seq)) {
        Matcher m = Pattern.compile(openTag).matcher(str.substring(seq.start,
            seq.start + seq.length));

        if (m.matches()) {
          if (acceptParam && (seq.param != null)) {
            buffer.append( //
                openSubstWithParam.replaceAll("\\{BBCODE_PARAM\\}", seq.param));
          } else {
            buffer.append(openSubstWithoutParam);
          }
        }
      }

      start = seq.start + seq.length;
    }

    buffer.append(str.substring(start));
  }

  private static class MutableCharSequence implements CharSequence {
    /** */
    public CharSequence base;

    /** */
    public int start;

    /** */
    public int length;

    /** */
    private String param = null;

    /**
     */
    public MutableCharSequence() {
      //
    }

    /**
     * @param base
     * @param start
     * @param length
     */
    public MutableCharSequence(CharSequence base, int start, int length) {
      reset(base, start, length);
    }

    /**
     * @see CharSequence#length()
     */
    public int length() {
      return length;
    }

    /**
     * @see CharSequence#charAt(int)
     */
    public char charAt(int index) {
      return base.charAt(start + index);
    }

    /**
     * @see CharSequence#subSequence(int, int)
     */
    public CharSequence subSequence(int pStart, int end) {
      return new MutableCharSequence(base,
          start + pStart,
          start + (end - pStart));
    }

    /**
     * @param pBase
     * @param pStart
     * @param pLength
     * @return -
     */
    public void reset(CharSequence pBase, int pStart, int pLength) {
      base = pBase;
      start = pStart;
      length = pLength;
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();

      for (int i = start; i < (start + length); i++) {
        sb.append(base.charAt(i));
      }

      return sb.toString();
    }

  }
}
