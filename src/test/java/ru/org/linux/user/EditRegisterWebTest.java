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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.org.linux.csrf.CSRFProtectionService;
import ru.org.linux.test.WebHelper;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("integration-tests-context.xml")
public class EditRegisterWebTest {

  private static String MAXCOM_NAME = "Максим Валянский";
  private static String MAXCOM_URL = "http://maxcom.pp.ru/";
  private static String MAXCOM_EMAIL = "max.valjanski+test93@gmail.com";
  private static String MAXCOM_TOWN = "Москва";
  private static String MAXCOM_INFO = "test<b>test</b>";
  private static String MAXCOM_PASS = "passwd";

  private WebResource resource;

  @Autowired
  private UserDao userDao;

  private void rescueMaxcom() throws Exception {
    User user = userDao.getUser("maxcom");
    userDao.updateUser(
        user,
        MAXCOM_NAME,
        MAXCOM_URL,
        MAXCOM_EMAIL,
        MAXCOM_TOWN,
        MAXCOM_PASS,
        MAXCOM_INFO
    );
  }

  @Before
  public void initResource() throws Exception {
    Client client = new Client();
    client.setFollowRedirects(false);
    resource = client.resource(WebHelper.MAIN_URL);
    rescueMaxcom();
  }

  @After
  public void clean() throws Exception {
    rescueMaxcom();
  }

  @Test
  public void testChangePassword() throws IOException {
    String auth = WebHelper.doLogin(resource, "maxcom", "passwd");
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
}