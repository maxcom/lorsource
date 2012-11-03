package ru.org.linux.topic;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.httpclient.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.org.linux.csrf.CSRFProtectionService;
import ru.org.linux.section.Section;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

public class AddTopicControllerWebTest {
  private static final String LOCAL_SERVER = "http://127.0.0.1:8080";
  private static final int TEST_GROUP = 4068;
  private static final String TEST_USER = "Shaman007";
  private static final String TEST_PASSWORD = "passwd";
  private static final String AUTH_COOKIE = "remember_me";
  private static final String TEST_TITLE = "Test Title";

  private WebResource resource;

  @BeforeMethod
  public void initResource() {
    Client client = new Client();

    client.setFollowRedirects(false);

    resource = client.resource(LOCAL_SERVER);
  }

  @Test
  public void testPostForm() throws IOException {
    ClientResponse cr = resource
            .path("add-section.jsp")
            .queryParam("section", Integer.toString(Section.SECTION_NEWS))
            .get(ClientResponse.class);

    Assert.assertEquals(HttpStatus.SC_OK, cr.getStatus());

    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    Assert.assertFalse(doc.select("input[name=csrf]").isEmpty(), "missing csrf");
  }

  @Test
  public void testPostDenied() throws IOException {
    MultivaluedMap<String, String> formData = new MultivaluedMapImpl();

    formData.add("section", Integer.toString(Section.SECTION_FORUM));
    formData.add("group", Integer.toString(TEST_GROUP));

    ClientResponse cr = resource
            .path("add.jsp")
            .post(ClientResponse.class, formData);

    Assert.assertEquals(HttpStatus.SC_OK, cr.getStatus());

    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    // System.out.println(doc.html());

    Assert.assertFalse(doc.select("#messageForm").isEmpty(), "not message form");
    Assert.assertFalse(doc.select(".error").isEmpty(), "missing error test");
    Assert.assertFalse(doc.select("input[name=csrf]").isEmpty(), "missing csrf");
  }

  @Test
  public void testPostSuccess() throws IOException {
    String auth = doLogin();

    MultivaluedMap<String, String> formData = new MultivaluedMapImpl();

    formData.add("section", Integer.toString(Section.SECTION_FORUM));
    formData.add("group", Integer.toString(TEST_GROUP));
    formData.add("csrf", "csrf");
    formData.add("title", TEST_TITLE);

    ClientResponse cr = resource
            .path("add.jsp")
            .cookie(new Cookie(AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
            .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
            .post(ClientResponse.class, formData);

    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    Assert.assertTrue(doc.select("#messageForm").isEmpty(), doc.select(".error").text());

    Assert.assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr.getStatus());

    ClientResponse tempPage = resource      // TODO remove temp redirect from Controller
            .uri(cr.getLocation())
            .get(ClientResponse.class);

    Assert.assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, tempPage.getStatus());

    ClientResponse page = resource
            .uri(tempPage.getLocation())
            .get(ClientResponse.class);

    Assert.assertEquals(HttpStatus.SC_OK, page.getStatus());

    Document finalDoc = Jsoup.parse(page.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    Assert.assertEquals(TEST_TITLE, finalDoc.select("h1[itemprop=headline] a").text());
  }

  /**
   * Login as a test user
   * @return rememberme cookie value
   * @throws IOException
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

    Assert.assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr.getStatus());

    for (Cookie cookie : cr.getCookies()) {
      if (cookie.getName().equals(AUTH_COOKIE)) {
        return cookie.getValue();
      }
    }

    System.out.println(cr.getCookies());
    Assert.fail("Can't find rememberme cookie");
    return null;
  }
}
