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

package org.javabb.bbcode;

/**
 * @author
 * @since 18/01/2005
 */
public class SimpleRegexTag implements RegexTag {
    private String _tagName;
    private String _regex;
    private String _replacement;

    /**
     * @param tagName
     * @param regex
     * @param replacement
     */
    public SimpleRegexTag(String tagName, String regex, String replacement) {
      _tagName = tagName;
      _regex = regex;
      _replacement = replacement;

    }

    /**
     * @return tag name
     */
    public String getTagName() {
        return _tagName;
    }

    /**
     * @return opening tag replace
     */
    public String getRegex() {
        return _regex;
    }

    /**
     * @return closing tag replace
     */
    public String getReplacement() {
        return _replacement;
    }

    /**
     * @param tagName
     */
    public void setTagName(String tagName) {
      _tagName = tagName;
    }

    /**
     * @param regex
     */
    public void setRegex(String regex) {
      _regex = regex;
    }

    /**
     * @param replacement
     */
    public void setReplacement(String replacement) {
      _replacement = replacement;
    }
}
