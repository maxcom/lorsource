/*
 * Copyright 1998-2026 Linux.org.ru
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
package ru.org.linux.util.bbcode

import munit.FunSuite
import ru.org.linux.util.bbcode.nodes.RootNode

/** Тесты для [[Parser]]. */
class ParserTest extends FunSuite:
  private val data: List[(String, String)] = List(
    "[list][*]fdfdddfd[/list][[raw]]" -> "<ul><li>fdfdddfd</li></ul><p>[[raw]]</p>",
    "[list][*]fdfdddfd[list][[raw]]" -> "<ul><li>fdfdddfd[[raw]]</li></ul>",
    "[code][list][*]fdfdddfd[list][[raw]][/code][/code]" ->
      "<div class=\"code\"><pre class=\"no-highlight\"><code>[list][*]fdfdddfd[list][[raw]]</code></pre></div>"
  )

  for (input, expected) <- data do
    test(s"parse: $input"):
      val parser = new Parser(new DefaultParserParameters)
      val rootNode = new RootNode(new DefaultParserParameters)
      parser.parseRoot(rootNode, input)
      assertEquals(expected, rootNode.renderXHtml)