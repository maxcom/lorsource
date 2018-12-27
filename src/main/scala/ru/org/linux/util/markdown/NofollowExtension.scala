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

import com.vladsch.flexmark.ast.{AutoLink, Link}
import com.vladsch.flexmark.html.renderer.{AttributablePart, LinkResolverContext}
import com.vladsch.flexmark.html.{AttributeProvider, HtmlRenderer, IndependentAttributeProviderFactory}
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.html.Attributes
import com.vladsch.flexmark.util.options.MutableDataHolder

class NofollowExtension extends HtmlRenderer.HtmlRendererExtension {
  override def rendererOptions(options: MutableDataHolder): Unit = {}

  override def extend(rendererBuilder: HtmlRenderer.Builder, rendererType: String): Unit = {
    rendererBuilder.attributeProviderFactory(new IndependentAttributeProviderFactory {
      override def create(context: LinkResolverContext): AttributeProvider =
        new NofollowAttributeProvider
    })
  }
}

class NofollowAttributeProvider extends AttributeProvider {
  override def setAttributes(node: Node, part: AttributablePart, attributes: Attributes): Unit = {
    node match {
      case _: Link if part == AttributablePart.LINK ⇒
        attributes.addValue("rel", "nofollow")
      case _: AutoLink if part == AttributablePart.LINK ⇒
        attributes.addValue("rel", "nofollow")
      case _ ⇒
    }
  }
}