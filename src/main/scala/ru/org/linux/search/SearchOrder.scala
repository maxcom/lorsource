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

package ru.org.linux.search

import java.util

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.sort.{ScoreSortDefinition, SortDefinition, SortOrder}

import scala.collection.JavaConverters._

sealed trait SearchOrder {
  def name: String
  def id: String

  def order: SortDefinition
}

object SearchOrder {
  case object Relevance extends SearchOrder {
    override val name = "по релевантности"
    override val id = "RELEVANCE"

    override def order = ScoreSortDefinition(SortOrder.DESC)
  }

  case object Date extends SearchOrder {
    override val name = "по дате: от новых к старым"
    override val id = "DATE"

    override def order = fieldSort("postdate") order SortOrder.DESC
  }

  case object DateReverse extends SearchOrder {
    override val name = "по дате: от старых к новым"
    override val id = "DATE_OLD_TO_NEW"

    override def order = fieldSort("postdate") order SortOrder.ASC
  }

  val values: Seq[SearchOrder] = Seq(Relevance, Date, DateReverse)

  def jvalues: util.List[SearchOrder] = values.asJava

  def valueOf(str: String): Object =
    values.find(_.id == str).getOrElse(new IllegalArgumentException(s"bad order $str"))
}
