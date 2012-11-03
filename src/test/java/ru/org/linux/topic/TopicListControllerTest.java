/*
 * Copyright 1998-2012 Linux.org.ru
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

// TODO: Для запуска этого теста необходимы интерфейсы сервисов, используемых в topicListController.
// TODO: Интерфейсы нужны для создания Stub-объектов; если же делать Mock-объекты, то придётся следовать
// TODO: за @Autowired в каждом сервисе, который имитируем, и объекты по автоваредам тоже нужно будет делать mock'ами.

package ru.org.linux.topic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@ContextConfiguration("unit-tests-context.xml")
public class TopicListControllerTest  extends AbstractTestNGSpringContextTests {

  @Autowired
  private ApplicationContext applicationContext;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

//  @Autowired
  private TopicListController topicListController;

  @BeforeMethod
  public void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  @Test
  public void mainTopicsFeedHandler() {
/*
    ModelAndView modelAndView = topicListController.mainTopicsFeedHandler(
      null, null, null, null, null, null, request, response
      );

    assertEquals("view-news", modelAndView.getViewName());

    Map<String, Object> model = modelAndView.getModel();

    assertTrue(model.get("year") == null);
    assertTrue(model.get("month") == null);
    assertTrue(model.get("section") == null);
    assertTrue(model.get("tag") == null);
    assertTrue(model.get("group") == null);

    assertEquals("", model.get("params"));
    assertEquals("bla-bla", model.get("ptitle"));
    assertEquals("bla-bla", model.get("nav"));
*/
  }

}
