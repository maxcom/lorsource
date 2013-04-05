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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = TopicIntegrationTestConfiguration.class)
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
