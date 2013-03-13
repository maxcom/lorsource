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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class AddTopicControllerWebTest {
  private static final int TEST_GROUP = 4068;
  private static final String TEST_USER = "Shaman007";
  private static final String TEST_PASSWORD = "passwd";
  private static final String TEST_TITLE = "Test Title";

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
    formData.add("msg", "http://pubs.opengroup.org/onlinepubs/9699919799/utilities/V3_chap02.html#tag_18_06_03");

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

    //
    // Search
    //

    ClientResponse searchCR = resource
        .path("search.jsp")
        .queryParam("q", "*opengroup*")
        .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
        .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_OK, searchCR.getStatus());

    Document searchDoc = Jsoup.parse(searchCR.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    Matcher m = Pattern
        .compile("Всего найдено (\\d+) результатов")
        .matcher(searchDoc.select(".infoblock div").text().trim());

    assertTrue(m.find());                            // Всего найдено
    assertTrue(Integer.parseInt(m.group(1)) > 0);    //
  }
}
