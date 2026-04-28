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

import org.opensearch.client.opensearch._types.analysis.SnowballLanguage.{English, Russian}
import org.opensearch.client.opensearch._types.analysis.*
import org.opensearch.client.opensearch._types.mapping.TermVectorOption.WithPositionsOffsets
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis

import scala.jdk.CollectionConverters.MapHasAsJava

object MessageIndex {
  private val textAnalyzer: CustomAnalyzer =
    CustomAnalyzer.of(
      _.tokenizer("text_tokenizer")
        .filter("m_long_word", "lowercase", "m_my_snow_ru", "m_my_snow_en")
        .charFilter("html_strip", "m_ee"))

  private val exactAnalyzer: CustomAnalyzer =
    CustomAnalyzer.of(_.tokenizer("text_tokenizer").filter("m_long_word", "lowercase").charFilter("html_strip", "m_ee"))
  private val textTokenizer: StandardTokenizer = StandardTokenizer.of(t => t)
  private val longWord: LengthTokenFilter = LengthTokenFilter.of(_.max(100))
  private val russianSnowball: SnowballTokenFilter = SnowballTokenFilter.of(_.language(Russian))
  private val englishSnowball: SnowballTokenFilter = SnowballTokenFilter.of(_.language(English))
  private val ee: MappingCharFilter = MappingCharFilter.of(_.mappings("ё => е", "Ё => Е"))

  val analysis: IndexSettingsAnalysis =
    IndexSettingsAnalysis.of(
      _.analyzer(Map("text_analyzer" -> textAnalyzer.toAnalyzer, "exact_analyzer" -> exactAnalyzer.toAnalyzer).asJava)
        .tokenizer("text_tokenizer", _.definition(textTokenizer.toTokenizerDefinition))
        .filter("m_long_word", _.definition(longWord.toTokenFilterDefinition))
        .filter("m_my_snow_ru", _.definition(russianSnowball.toTokenFilterDefinition))
        .filter("m_my_snow_en", _.definition(englishSnowball.toTokenFilterDefinition))
        .charFilter("m_ee", _.definition(ee.toCharFilterDefinition)))

  val mappings: TypeMapping =
    TypeMapping.of(
      _.properties("group", _.keyword(k => k))
        .properties("section", _.keyword(k => k))
        .properties("is_comment", _.boolean_(b => b))
        .properties("postdate", _.date(d => d))
        .properties("author", _.keyword(k => k))
        .properties("tag", _.keyword(k => k))
        .properties("topic_author", _.keyword(k => k))
        .properties("topic_id", _.long_(l => l))
        .properties("topic_title", _.text(t => t.index(false)))
        .properties("title", _.text(t => t.analyzer("text_analyzer")))
        .properties(
          "message",
          _.text(t =>
            t.analyzer("text_analyzer")
              .fields("raw", _.text(t => t.analyzer("exact_analyzer").termVector(WithPositionsOffsets)))
              .termVector(WithPositionsOffsets))
        )
        .properties(OpenSearchIndexService.COLUMN_TOPIC_AWAITS_COMMIT, _.boolean_(b => b)))
}
