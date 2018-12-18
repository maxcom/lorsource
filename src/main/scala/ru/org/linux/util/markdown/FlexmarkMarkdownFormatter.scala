/*
 * Copyright 1998-2018 Linux.org.ru
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
package ru.org.linux.util.markdown

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.youtube.embedded.YouTubeLinkExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import ru.org.linux.comment.CommentDao
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.TopicDao

import scala.collection.JavaConverters._

@Service
@Qualifier("flexmark")
class FlexmarkMarkdownFormatter(siteConfig: SiteConfig, topicDao: TopicDao, commentDao: CommentDao) extends MarkdownFormatter {
  private def options(nofollow: Boolean) = {
    val options = new MutableDataSet

    val extensions = Seq(TablesExtension.create, StrikethroughExtension.create,
      AutolinkExtension.create(), new SuppressImagesExtension, new LorLinkExtension(siteConfig, topicDao, commentDao),
      YouTubeLinkExtension.create())

    val allExtensions = (if (nofollow) {
      extensions :+ new NofollowExtension
    } else {
      extensions
    }).asJava

    options.set(Parser.EXTENSIONS, allExtensions)

    options.set(HtmlRenderer.SUPPRESSED_LINKS, "javascript:.*")
    options.set(HtmlRenderer.SUPPRESS_HTML, Boolean.box(true))

    // uncomment to convert soft-breaks to hard breaks
    //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

    options.toImmutable
  }

  private val parser = Parser.builder(options(nofollow = false)).build
  private val renderer = HtmlRenderer.builder(options(nofollow = false)).build
  private val rendererNofollow = HtmlRenderer.builder(options(nofollow = true)).build

  override def renderToHtml(content: String, nofollow: Boolean): String = {
    // You can re-use parser and renderer instances
    val document = parser.parse(content)

    (if (nofollow) {
      rendererNofollow
    } else {
      renderer
    }).render(document)
  }
}