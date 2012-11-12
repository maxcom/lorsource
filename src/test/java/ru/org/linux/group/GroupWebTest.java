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
package ru.org.linux.group;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.httpclient.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import ru.org.linux.test.WebHelper;

import javax.ws.rs.core.Cookie;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
public class GroupWebTest {

  private WebResource resource;

  @Before
  public void initResource() throws Exception {
    Client client = new Client();
    client.setFollowRedirects(false);
    resource = client.resource(WebHelper.MAIN_URL);
  }

  @Test
  public void test1GroupInfo() throws IOException {
    ClientResponse cr = resource
        .path("/forum/talks/")
        .get(ClientResponse.class);
    assertEquals(HttpStatus.SC_OK, cr.getStatus());
    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());
    assertTrue(doc.select(".infoblock").text().length() > 0);
  }

  @Test
  public void test2GroupInfo() throws IOException {
    String auth = WebHelper.doLogin(resource, "maxcom", "passwd");
    ClientResponse cr = resource
        .path("/forum/talks/")
        .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
        .get(ClientResponse.class);
    assertEquals(HttpStatus.SC_OK, cr.getStatus());
    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());
    assertTrue(doc.select(".infoblock").text().length() > 0);
    // у модератора в последнем абзаце groupInfo ссылка на изменение groupinfo
    assertEquals("[править]", doc.select(".infoblock p").last().text());
    assertEquals("groupmod.jsp?group=8404", doc.select(".infoblock p").last().select("a").attr("href"));

    ClientResponse cr2 = resource
        .path("/forum/job/")
        .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
        .get(ClientResponse.class);
    assertEquals(HttpStatus.SC_OK, cr2.getStatus());
    Document doc2 = Jsoup.parse(cr2.getEntityInputStream(), "UTF-8", resource.getURI().toString());
    // кстати у форумов без userinfo кнопочки нет (
    assertTrue(doc2.select(".infoblock").isEmpty());
    assertTrue(doc2.select(".infoblock p").isEmpty());
  }

}
