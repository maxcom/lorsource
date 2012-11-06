package ru.org.linux.user;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.httpclient.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.org.linux.csrf.CSRFProtectionService;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 */
public class EditRegisterWebTest {
  private static final String LOCAL_SERVER = "http://127.0.0.1:8080";
  private static final String TEST_USER = "maxcom";
  private static final String TEST_PASSWORD = "passwd";
  private static final String AUTH_COOKIE = "remember_me";

  private WebResource resource;

  @Before
  public void initResource() {
    Client client = new Client();
    client.setFollowRedirects(false);
    resource = client.resource(LOCAL_SERVER);
  }

  @Test
  public void testChangePassword() throws IOException {
    String auth = doLogin();
    ClientResponse cr = resource
            .path("people/maxcom/edit")
            .cookie(new Cookie(AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
            .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_OK, cr.getStatus());

    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    String name = doc.getElementById("name").val();
    String url = doc.getElementById("url").val();
    String email = doc.getElementById("email").val();
    String town = doc.getElementById("town").val();
    String info = doc.getElementById("info").val();

    assertEquals("Максим Валянский", name);
    assertEquals("http://maxcom.pp.ru/", url);
    assertEquals("max.valjanski+test93@gmail.com", email);
    assertEquals("Москва", town);
    assertEquals("test<b>test</b>", info);

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
            .cookie(new Cookie(AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
            .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
            .post(ClientResponse.class, formData);

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr2.getStatus());

    String newAuth = getAuthCookie(cr2);

    assertNotNull(newAuth);

    ClientResponse cr3 = resource
            .uri(cr2.getLocation())
            .cookie(new Cookie(AUTH_COOKIE, newAuth, "/", "127.0.0.1", 1))
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
            .cookie(new Cookie(AUTH_COOKIE, newAuth, "/", "127.0.0.1", 1))
            .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
            .post(ClientResponse.class, formData2);

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr4.getStatus());

    String newAuth2 = getAuthCookie(cr4);

    ClientResponse cr5 = resource
            .uri(cr4.getLocation())
            .cookie(new Cookie(AUTH_COOKIE, newAuth2, "/", "127.0.0.1", 1))
            .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_OK, cr5.getStatus());

 }

  /**
   * Login as a test user
   * @return rememberme cookie value
   * @throws java.io.IOException
   */
  public String doLogin() throws IOException {
    MultivaluedMap<String, String> formData = new MultivaluedMapImpl();

    formData.add("nick", TEST_USER);
    formData.add("passwd", TEST_PASSWORD);
    formData.add("csrf", "csrf");

    ClientResponse cr = resource
            .path("login_process")
            .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
            .post(ClientResponse.class, formData);

    //System.out.print(Jsoup.parse(cr.getEntityInputStream(), "utf-8", LOCAL_SERVER).html());

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr.getStatus());

    String auth = getAuthCookie(cr);
    if(auth != null) {
      return auth;
    }

    Assert.fail("Can't find rememberme cookie");
    return null;
  }

  public String getAuthCookie(ClientResponse cr) {
    for (Cookie cookie : cr.getCookies()) {
      if (cookie.getName().equals(AUTH_COOKIE)) {
        return cookie.getValue();
      }
    }

    System.out.println("cookies:" + cr.getCookies());
    return null;
  }


}