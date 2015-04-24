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
import org.apache.commons.httpclient.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.org.linux.test.WebHelper;

import javax.ws.rs.core.Cookie;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SimpleIntegrationTestConfiguration.class)
public class AddPhotoWebTest {
  private WebResource resource;

  @Autowired
  private UserDao userDao;

  private void rescueJB() throws Exception {
    final User user = userDao.getUser(userDao.findUserId("JB"));
    userDao.unblock(user, user);
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
  public void testPage() throws IOException {
    String auth = WebHelper.doLogin(resource, "JB", "passwd");
    ClientResponse cr = resource
        .path("addphoto.jsp")
        .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
        .get(ClientResponse.class);
    assertEquals(HttpStatus.SC_OK, cr.getStatus());
  }

  @Test
  /**
   * Тест неправильной картинки
   */
  public void testInvalidImage() throws IOException {
    String auth = WebHelper.doLogin(resource, "JB", "passwd");
    ClientResponse cr = WebHelper.addPhoto(resource, "src/test/resources/ROOT.xml", auth);
    assertEquals(HttpStatus.SC_BAD_REQUEST, cr.getStatus());
    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());
    assertEquals("Ошибка! Invalid image", doc.select(".error").text()); // сообщение об ошипке

  }

  @Test
  /**
   * Тест неправильной картинки
   */
  public void testInvalid2Image() throws IOException {
    String auth = WebHelper.doLogin(resource, "JB", "passwd");
    ClientResponse cr = WebHelper.addPhoto(resource, "src/main/webapp/img/tux.png", auth);
    assertEquals(HttpStatus.SC_BAD_REQUEST, cr.getStatus());
    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());
    assertEquals("Ошибка! Сбой загрузки изображения: слишком большой файл", doc.select(".error").text()); // сообщение об ошипке
  }

  @Test
  /**
   * Тест неправильной картинки
   */
  public void testInvalid3Image() throws IOException {
    String auth = WebHelper.doLogin(resource, "JB", "passwd");
    ClientResponse cr = WebHelper.addPhoto(resource, "src/main/webapp/img/twitter.png", auth);
    assertEquals(HttpStatus.SC_BAD_REQUEST, cr.getStatus());
    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());
    assertEquals("Ошибка! Сбой загрузки изображения: недопустимые размеры фотографии", doc.select(".error").text()); // сообщение об ошипке
  }

  @Test
  /**
   * Тест неправильной картинки
   */
  public void testInvalid4Image() throws IOException {
    String auth = WebHelper.doLogin(resource, "JB", "passwd");
    ClientResponse cr = WebHelper.addPhoto(resource, "src/test/resources/images/animated.gif", auth);
    assertEquals(HttpStatus.SC_BAD_REQUEST, cr.getStatus());
    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());
    assertEquals("Ошибка! Сбой загрузки изображения: анимация не допустима", doc.select(".error").text()); // сообщение об ошипке
  }

  @Test
  /**
   * Тест неправильной картинки
   */
  public void testValidImage() throws IOException {
    String auth = WebHelper.doLogin(resource, "JB", "passwd");
    ClientResponse cr = WebHelper.addPhoto(resource, "src/main/webapp/tango/img/android.png", auth);
    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr.getStatus());
    final String redirect = cr.getLocation().toString();
    final String url = "http://127.0.0.1:8080/people/JB/profile";
    final String val = "?nocache=";
    assertEquals(url, redirect.substring(0, url.length()));
    assertEquals(val, redirect.substring(url.length(), url.length() + val.length()));
    assertTrue("у nocache должен быть апгумент", redirect.length() > url.length() + val.length());
  }

  @Test
  /**
   * Тест с apng анимацией и поней
   * image source via http://tamalesyatole.deviantart.com/art/I-want-to-be-a-Hero-APNG-Animated-332248278
   */
  public void testAPNGImage() throws IOException {
    String auth = WebHelper.doLogin(resource, "JB", "passwd");
    ClientResponse cr = WebHelper.addPhoto(resource, "src/test/resources/images/i_want_to_be_a_hero__apng_animated__by_tamalesyatole-d5ht8eu.png", auth);
    assertEquals(HttpStatus.SC_BAD_REQUEST, cr.getStatus());
    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());
    assertEquals("Ошибка! Сбой загрузки изображения: анимация не допустима", doc.select(".error").text()); // сообщение об ошипке
  }

}
