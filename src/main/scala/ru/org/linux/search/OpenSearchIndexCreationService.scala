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

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.analysis.{Analysis, CustomAnalyzer, LengthTokenFilter, MappingCharFilter, SnowballTokenFilter, StandardTokenizer}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.requests.mappings.{MappingDefinition, TermVector}
import com.typesafe.scalalogging.StrictLogging
import org.springframework.stereotype.Service

object OpenSearchIndexCreationService {
  private val Mapping: MappingDefinition = properties(
    keywordField("group"),
    keywordField("section"),
    booleanField("is_comment"),
    dateField("postdate"),
    keywordField("author"),
    keywordField("tag"),
    keywordField("topic_author"),
    longField("topic_id"),
    textField("topic_title").index(false),
    textField("title").analyzer("text_analyzer"),
    textField("message").analyzer("text_analyzer").termVector(TermVector.WithPositionsOffsets).fields {
      textField("raw").termVector(TermVector.WithPositionsOffsets).analyzer("exact_analyzer")
    },
    booleanField(OpenSearchIndexService.COLUMN_TOPIC_AWAITS_COMMIT))

  private val Analyzers = Analysis(
    analyzers = List(
      CustomAnalyzer(
        name = "text_analyzer",
        tokenizer = "text_tokenizer",
        tokenFilters = List("m_long_word", "lowercase", "m_my_snow_ru", "m_my_snow_en"),
        charFilters = List("html_strip", "m_ee")),
      CustomAnalyzer(
        name = "exact_analyzer",
        tokenizer = "text_tokenizer",
        tokenFilters = List("m_long_word", "lowercase"),
        charFilters = List("html_strip", "m_ee"))),
    tokenizers = List(
      StandardTokenizer("text_tokenizer")
    ),
    tokenFilters = List(
      LengthTokenFilter("m_long_word").max(100),
      SnowballTokenFilter("m_my_snow_ru", "Russian"),
      SnowballTokenFilter("m_my_snow_en", "English")
    ),
    charFilters = List(
      MappingCharFilter("m_ee", Map("ё" -> "е", "Ё" -> "Е"))
    )
  )
}

@Service
class OpenSearchIndexCreationService(elastic: ElasticClient) extends StrictLogging {
  import OpenSearchIndexCreationService.*

  def createIndexIfNeeded(): Unit = {
    val indexExistsResult = elastic.execute {
      indexExists(OpenSearchIndexService.MessageIndex)
    }.await

    if (!indexExistsResult.result.isExists) {
      logger.info("Creating index {}", OpenSearchIndexService.MessageIndex)

      elastic.execute {
        createIndex(OpenSearchIndexService.MessageIndex).mapping(Mapping).analysis(Analyzers)
      }.await.result
    }
  }
}
