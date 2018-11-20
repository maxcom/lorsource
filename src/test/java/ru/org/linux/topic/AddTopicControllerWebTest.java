/*
 * Copyright 1998-2015 Linux.org.ru
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

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.httpclient.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import ru.org.linux.csrf.CSRFProtectionService;
import ru.org.linux.section.Section;
import ru.org.linux.test.WebHelper;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

import static org.junit.Assert.*;

public class AddTopicControllerWebTest {
  private static final int TEST_GROUP = 4068;
  private static final String TEST_USER = "Shaman007";
  private static final String TEST_PASSWORD = "passwd";
  private static final String TEST_TITLE = "Test Title";
  private static final String TEST_MESSAGE = "Test message";

  private WebResource resource;

  @Before
  public void initResource() {
    Client client = new Client();

    client.setFollowRedirects(false);

    resource = client.resource(WebHelper.MAIN_URL);
  }

  @Test
  public void testPostForm() throws IOException {
    ClientResponse cr = resource
            .path("add-section.jsp")
            .queryParam("section", Integer.toString(Section.SECTION_NEWS))
            .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_OK, cr.getStatus());

    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    assertFalse("missing csrf", doc.select("input[name=csrf]").isEmpty());
  }

  @Test
  public void testPostDenied() throws IOException {
    MultivaluedMap<String, String> formData = new MultivaluedMapImpl();

    formData.add("section", Integer.toString(Section.SECTION_FORUM));
    formData.add("group", Integer.toString(TEST_GROUP));

    ClientResponse cr = resource
            .path("add.jsp")
            .post(ClientResponse.class, formData);

    assertEquals(HttpStatus.SC_OK, cr.getStatus());

    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    // System.out.println(doc.html());

    assertFalse("not message form", doc.select("#messageForm").isEmpty());
    assertFalse("missing error test", doc.select(".error").isEmpty());
    assertFalse("missing csrf", doc.select("input[name=csrf]").isEmpty());
  }

  @Test
  public void testPostSuccess() throws IOException {
    String auth = WebHelper.doLogin(resource, TEST_USER, TEST_PASSWORD);

    MultivaluedMap<String, String> formData = new MultivaluedMapImpl();

    formData.add("section", Integer.toString(Section.SECTION_FORUM));
    formData.add("group", Integer.toString(TEST_GROUP));
    formData.add("csrf", "csrf");
    formData.add("title", TEST_TITLE);
    formData.add("msg", TEST_MESSAGE);

    ClientResponse cr = resource
            .path("add.jsp")
            .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
            .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
            .post(ClientResponse.class, formData);

    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    assertTrue(doc.select(".error").text(), doc.select("#messageForm").isEmpty());

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr.getStatus());

    ClientResponse tempPage = resource      // TODO remove temp redirect from Controller
            .uri(cr.getLocation())
            .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, tempPage.getStatus());

    ClientResponse page = resource
            .uri(tempPage.getLocation())
            .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_OK, page.getStatus());

    Document finalDoc = Jsoup.parse(page.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    assertEquals(TEST_TITLE, finalDoc.select("h1[itemprop=headline] a").text());
  }
}
