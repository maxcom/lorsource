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

package ru.org.linux.search

import com.sksamuel.elastic4s.requests.searches.SearchHit
import org.jsoup.safety.Safelist
import ru.org.linux.tag.TagRef
import ru.org.linux.user.User

import java.time.Instant
import java.util.Date
import scala.beans.BeanProperty

case class SearchItem (
  @BeanProperty title: String,
  @BeanProperty postdate: Date,
  @BeanProperty user: User,
  @BeanProperty message: String,
  @BeanProperty url: String,
  @BeanProperty score: Float,
  @BeanProperty comment: Boolean,
  @BeanProperty tags: java.util.List[TagRef])

object SearchResultsService {
  def postdate(doc: java.util.Map[String, AnyRef]): Instant = Instant.parse(doc.get("postdate").asInstanceOf[String])
  def section(doc: SearchHit): String = doc.sourceAsMap("section").asInstanceOf[String]
  def group(doc: SearchHit): String = doc.sourceAsMap("group").asInstanceOf[String]

  val TextSafelist: Safelist = Safelist.relaxed().addAttributes(":all", "class")
}

case class FacetItem(@BeanProperty key:String, @BeanProperty label:String)


