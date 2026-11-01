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

import munit.FunSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.ShardStatistics
import org.opensearch.client.opensearch._types.aggregations.{Aggregate, SignificantStringTermsBucket, StringTermsBucket}
import org.opensearch.client.opensearch.core.{SearchRequest, SearchResponse}
import org.opensearch.client.opensearch.core.search.{HitsMetadata, TotalHits, TotalHitsRelation}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.{redirectedUrl, status}
import org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup
import org.springframework.web.context.WebApplicationContext
import ru.org.linux.PekkoConfiguration
import ru.org.linux.auth.{IpBlockInfo, UserDetailsImpl}
import ru.org.linux.group.GroupService
import ru.org.linux.section.SectionService
import ru.org.linux.test.SpringTestSupport
import ru.org.linux.topic.TopicIntegrationTestConfiguration
import ru.org.linux.user.{Profile, User, UserService}

import java.time.ZoneId

@WebAppConfiguration
@ContextConfiguration(classes = Array(
  classOf[TopicIntegrationTestConfiguration],
  classOf[PekkoConfiguration],
  classOf[SearchControllerIntegrationTestConfiguration]))
class SearchControllerIntegrationTest extends FunSuite with SpringTestSupport:
  @Autowired
  var wac: WebApplicationContext = scala.compiletime.uninitialized

  @Autowired
  var userService: UserService = scala.compiletime.uninitialized

  @Autowired
  var elastic: OpenSearchClient = scala.compiletime.uninitialized

  private var mockMvc: MockMvc = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)

    val defaultReq = get("/")
    defaultReq.requestAttr("currentUser", userService.getAnonymous)
    defaultReq.requestAttr("ipBlockInfo", IpBlockInfo("127.0.0.1"))
    defaultReq.requestAttr("timezone", ZoneId.systemDefault())

    val builder = webAppContextSetup(wac)
    builder.defaultRequest(defaultReq)
    mockMvc = builder.build()

  override def afterEach(context: AfterEach): Unit =
    SecurityContextHolder.clearContext()
    super.afterEach(context)

  private def loginAs(user: User): Unit =
    val authorities = java.util.List.of[GrantedAuthority](new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
    val details = new UserDetailsImpl(user, authorities, Profile.DEFAULT)

    SecurityContextHolder
      .getContext
      .setAuthentication(new UsernamePasswordAuthenticationToken(details, null, authorities))

  private def stubEmptySearch(): Unit =
    val emptyResponse = new SearchResponse.Builder[MessageIndexDocument]()
      .took(1L)
      .timedOut(false)
      .shards(new ShardStatistics.Builder().total(1).successful(1).failed(0).build())
      .hits(
        new HitsMetadata.Builder[MessageIndexDocument]()
          .total(new TotalHits.Builder().value(0L).relation(TotalHitsRelation.Eq).build())
          .hits(java.util.List.of())
          .build())
      .aggregations(
        "sections",
        Aggregate.of(a => a.sterms(s => s.buckets(b => b.array(java.util.Collections.emptyList[StringTermsBucket]())))))
      .aggregations(
        "tags",
        Aggregate.of(a =>
          a.sigsterms(s => s.buckets(b => b.array(java.util.Collections.emptyList[SignificantStringTermsBucket]())))))
      .build()

    when(elastic.search(any[SearchRequest](), any[Class[MessageIndexDocument]]())).thenReturn(emptyResponse)

  test("anonymous deep search offset redirects to login"):
    mockMvc
      .perform(get("/search.jsp").param("q", "test").param("offset", "525"))
      .andExpect(status.isFound)
      .andExpect(redirectedUrl("/login.jsp"))

    // поисковый бэкенд не должен дёргаться вообще
    verify(elastic, never()).search(any[SearchRequest](), any[Class[MessageIndexDocument]]())

  test("anonymous search offset at boundary allowed"):
    stubEmptySearch()

    mockMvc.perform(get("/search.jsp").param("q", "test").param("offset", "500")).andExpect(status.isOk)

  test("anonymous first search page allowed"):
    stubEmptySearch()

    mockMvc.perform(get("/search.jsp").param("q", "test")).andExpect(status.isOk)

  test("authorized deep search offset allowed"):
    stubEmptySearch()
    loginAs(userService.getUser("edo"))

    mockMvc.perform(get("/search.jsp").param("q", "test").param("offset", "2500")).andExpect(status.isOk)

end SearchControllerIntegrationTest

@Configuration
class SearchControllerIntegrationTestConfiguration:
  @Bean
  def searchController(
      sectionService: SectionService,
      userService: UserService,
      groupService: GroupService,
      searchService: SearchService): SearchController =
    new SearchController(sectionService, userService, groupService, searchService)
