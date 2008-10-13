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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * @author
 * @since 18/01/2005
 */
public interface RegexTag {
  /**
   * @return tag name
   */
  String getTagName();

  /**
   * @return opening tag replace
   */
  Pattern getRegex();

  /**
   * @return closing tag replace
   */
  String getReplacement();

  void substitute(Connection db, CharSequence from, StringBuffer to, RegexTag regex, String replacement) throws SQLException;
}
