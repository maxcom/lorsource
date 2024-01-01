/*
 * Copyright 1998-2024 Linux.org.ru
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

package ru.org.linux.topic;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import ru.org.linux.AkkaConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {TopicIntegrationTestConfiguration.class,
        AkkaConfiguration.class})
public class TopicControllerIntegrationTest {
  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @Before
  public void setup() {
    mockMvc = webAppContextSetup(wac).build();
  }

  @Test
  public void testJumpToComment() throws Exception {
    mockMvc.perform(
            get("/forum/talks/1920001?cid=1920019")
    ).andExpect(redirectedUrl("/forum/talks/1920001#comment-1920019"));
  }

  @Test
  public void testLoadBase() throws Exception {
    mockMvc.perform(
            get("/forum/talks/1920001")
    ).andExpect(status().isOk());
  }

  @Test
  public void testLoadBaseZeroComments() throws Exception {
    mockMvc.perform(
            get("/polls/polls/98075")
    ).andExpect(status().isOk());
  }

  @Test
  public void testWrongPage() throws Exception {
    mockMvc.perform(
            get("/forum/talks/1920001/page10")
    ).andExpect(redirectedUrl("/forum/talks/1920001"));
  }

  @Test
  public void testZeroCommentsWrongPage() throws Exception {
    mockMvc.perform(
            get("/polls/polls/98075/page10")
    ).andExpect(redirectedUrl("/polls/polls/98075"));
  }
}
