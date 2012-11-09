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
package ru.org.linux.user;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.org.linux.test.WebHelper;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("integration-tests-context.xml")
public class AddPhotoWebTest {
  private WebResource resource;

  @Autowired
  private UserDao userDao;

  private void rescueJB() throws Exception {
    final User user = userDao.getUser("JB");
    userDao.unblock(user);
  }

  @Before
  public void initResource() throws Exception {
    Client client = new Client();
    client.setFollowRedirects(false);
    resource = client.resource(WebHelper.MAIN_URL);
    rescueJB();
  }

  @After
  public void clean() throws Exception {
    rescueJB();
  }

  /**
   * @throws java.io.IOException
   */
  @Test
  public void testSimple() throws IOException {
    String auth = WebHelper.doLogin(resource, "JB", "passwd");

    ClientResponse cr = resource
        .path("addphoto.jsp")
        .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
        .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_OK, cr.getStatus());
    String content = "This is a binary content";

    FormDataMultiPart form = new FormDataMultiPart();
    form.bodyPart(new FormDataBodyPart("csrf", "csrf"));
    form.bodyPart(new FormDataBodyPart("file", content.getBytes(), new MediaType("image", "jpeg")));

/*    ClientResponse cr2 = resource
        .path("addphoto.jsp")
        .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
        .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
        .type(MediaType.MULTIPART_FORM_DATA_TYPE)
        .post(ClientResponse.class, form);

    assertEquals(HttpStatus.SC_BAD_REQUEST, cr.getStatus());

    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString()); */


  }


}
