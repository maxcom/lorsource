/*
 * Copyright 1998-2016 Linux.org.ru
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
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.httpclient.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.org.linux.csrf.CSRFProtectionService;
import ru.org.linux.test.WebHelper;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
        @ContextConfiguration("classpath:database.xml"),
        @ContextConfiguration(classes = SimpleIntegrationTestConfiguration.class)
})
public class EditRegisterWebTest {
  private static String MAXCOM_NAME = "Максим Валянский";
  private static String MAXCOM_URL = "http://maxcom.pp.ru/";
  private static String MAXCOM_EMAIL = "max.valjanski+test93@gmail.com";
  private static String MAXCOM_TOWN = "Москва";
  private static String MAXCOM_INFO = "test<b>test</b>";
  private static String MAXCOM_PASS = "passwd";

  private static String JB_NAME = "Тёма";
  private static String JB_URL = "http://darkmachine.org";
  private static String JB_EMAIL = "mail@darkmachine.org";
  private static String JB_TOWN = "Самара";
  private static String JB_INFO = "[i]Эффективный менеджер по распилу гос-бабла[/i]";
  private static String JB_PASS = "passwd";

  private WebResource resource;

  @Autowired
  private UserDao userDao;

  private void rescueMaxcom() throws Exception {
    final User user = userDao.getUser(userDao.findUserId("maxcom"));
    userDao.updateUser(
        user,
        MAXCOM_NAME,
        MAXCOM_URL,
        MAXCOM_EMAIL,
        MAXCOM_TOWN,
        MAXCOM_PASS,
        MAXCOM_INFO
    );
    userDao.acceptNewEmail(user, MAXCOM_EMAIL);
  }

  private void rescueJB() throws Exception {
    final User user = userDao.getUser(userDao.findUserId("JB"));
    userDao.updateUser(
        user,
        JB_NAME,
        JB_URL,
        JB_EMAIL,
        JB_TOWN,
        JB_PASS,
        JB_INFO
    );
    userDao.acceptNewEmail(user, JB_EMAIL);
    userDao.unblock(user, user);
  }

  @Before
  public void initResource() throws Exception {
    Client client = new Client();
    client.setFollowRedirects(false);
    resource = client.resource(WebHelper.MAIN_URL);
    rescueMaxcom();
    rescueJB();
  }

  @After
  public void clean() throws Exception {
    rescueMaxcom();
    rescueJB();
  }

  /**
   * Вводим теже данные которые и были изначально. После изменений должен быть
   * redirect в профиль
   * @throws IOException
   */
  @Test
  public void testSimple() throws IOException {
    String auth = WebHelper.doLogin(resource, "JB", JB_PASS);

    ClientResponse cr = resource
        .path("people/JB/edit")
        .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
        .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_OK, cr.getStatus());

    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    assertEquals("/people/JB/edit", doc.getElementById("editRegForm").attr("action"));

    String name = doc.getElementById("name").val();
    String url = doc.getElementById("url").val();
    String email = doc.getElementById("email").val();
    String town = doc.getElementById("town").val();
    String info = doc.getElementById("info").val();

    assertEquals(JB_NAME, name);
    assertEquals(JB_URL, url);
    assertEquals(JB_EMAIL, email);
    assertEquals(JB_TOWN, town);
    assertEquals(JB_INFO, info);

    MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
    formData.add("name", name);
    formData.add("url", url);
    formData.add("email", email);
    formData.add("town", town);
    formData.add("info", info);
    formData.add("csrf", "csrf");
    formData.add("oldpass", JB_PASS);

    ClientResponse cr2 = resource
        .path("people/maxcom/edit")
        .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
        .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
        .post(ClientResponse.class, formData);

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr2.getStatus());
    assertEquals("http://127.0.0.1:8080/people/JB/profile", cr2.getLocation().toString());
  }

  @Test
  public void testChangePassword() throws IOException {
    String auth = WebHelper.doLogin(resource, "maxcom", MAXCOM_PASS);
    ClientResponse cr = resource
            .path("people/maxcom/edit")
            .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
            .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_OK, cr.getStatus());

    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    String name = doc.getElementById("name").val();
    String url = doc.getElementById("url").val();
    String email = doc.getElementById("email").val();
    String town = doc.getElementById("town").val();
    String info = doc.getElementById("info").val();

    assertEquals(MAXCOM_NAME, name);
    assertEquals(MAXCOM_URL, url);
    assertEquals(MAXCOM_EMAIL, email);
    assertEquals(MAXCOM_TOWN, town);
    assertEquals(MAXCOM_INFO, info);

    MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
    formData.add("name", name);
    formData.add("url", url);
    formData.add("email", email);
    formData.add("town", town);
    formData.add("info", info);
    formData.add("csrf", "csrf");
    formData.add("oldpass", "passwd");
    formData.add("password", "passwd2");
    formData.add("password2", "passwd2");

    ClientResponse cr2 = resource
            .path("people/maxcom/edit")
            .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
            .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
            .post(ClientResponse.class, formData);

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr2.getStatus());

    String newAuth = WebHelper.getAuthCookie(cr2);

    assertNotNull(newAuth);

    ClientResponse cr3 = resource
            .uri(cr2.getLocation())
            .cookie(new Cookie(WebHelper.AUTH_COOKIE, newAuth, "/", "127.0.0.1", 1))
            .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_OK, cr3.getStatus());

    MultivaluedMap<String, String> formData2 = new MultivaluedMapImpl();
    formData2.add("name", name);
    formData2.add("url", url);
    formData2.add("email", email);
    formData2.add("town", town);
    formData2.add("info", info);
    formData2.add("csrf", "csrf");
    formData2.add("oldpass", "passwd2");
    formData2.add("password", "passwd");
    formData2.add("password2", "passwd");

    ClientResponse cr4 = resource
            .path("people/maxcom/edit")
            .cookie(new Cookie(WebHelper.AUTH_COOKIE, newAuth, "/", "127.0.0.1", 1))
            .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
            .post(ClientResponse.class, formData2);

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr4.getStatus());

    String newAuth2 = WebHelper.getAuthCookie(cr4);

    ClientResponse cr5 = resource
            .uri(cr4.getLocation())
            .cookie(new Cookie(WebHelper.AUTH_COOKIE, newAuth2, "/", "127.0.0.1", 1))
            .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_OK, cr5.getStatus());
 }

  @Test
  public void testChange() throws IOException {
    String auth = WebHelper.doLogin(resource, "JB", JB_PASS);

    ClientResponse cr = resource
        .path("people/JB/edit")
        .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
        .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_OK, cr.getStatus());

    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    assertEquals("/people/JB/edit", doc.getElementById("editRegForm").attr("action"));

    String name = doc.getElementById("name").val();
    String url = doc.getElementById("url").val();
    String email = doc.getElementById("email").val();
    String town = doc.getElementById("town").val();
    String info = doc.getElementById("info").val();

    assertEquals(JB_NAME, name);
    assertEquals(JB_URL, url);
    assertEquals(JB_EMAIL, email);
    assertEquals(JB_TOWN, town);
    assertEquals(JB_INFO, info);

    MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
    formData.add("name", name);
    formData.add("url", url);
    formData.add("email", email);
    formData.add("town", town);
    formData.add("info", info);
    formData.add("csrf", "csrf");

    ClientResponse cr2 = resource
        .path("people/JB/edit")
        .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
        .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
        .post(ClientResponse.class, formData);

    Document doc2 = Jsoup.parse(cr2.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    assertEquals(HttpStatus.SC_OK, cr2.getStatus());
    assertEquals("Для изменения регистрации нужен ваш пароль", doc2.select(".error").text());
    assertEquals("/people/JB/edit", doc2.getElementById("editRegForm").attr("action"));
  }

}