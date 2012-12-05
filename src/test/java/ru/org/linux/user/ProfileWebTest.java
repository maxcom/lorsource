package ru.org.linux.user;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.httpclient.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.org.linux.test.WebHelper;

import javax.ws.rs.core.Cookie;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("integration-tests-context.xml")
public class ProfileWebTest {
  private WebResource resource;

  @Before
  public void initResource() throws Exception {
    Client client = new Client();
    client.setFollowRedirects(false);
    resource = client.resource(WebHelper.MAIN_URL);
  }

  @Test
  public void testJB() throws IOException {
    Cookie authJB = WebHelper.doLoginCookie(resource, "JB", "passwd"); //
    ClientResponse crSunch = WebHelper.getPage(resource, authJB, "/people/Sun-ch/profile");
    ClientResponse crTailgunner = WebHelper.getPage(resource, authJB, "/people/tailgunner/profile");
    ClientResponse crWaker = WebHelper.getPage(resource, authJB, "/people/waker/profile");

    assertEquals(HttpStatus.SC_OK, crSunch.getStatus());
    Document docSunch = Jsoup.parse(crSunch.getEntityInputStream(), "UTF-8", "");
    assertFalse(docSunch.select("#loginGreating").isEmpty());
    assertTrue(docSunch.select("#deletedTopicsLink").isEmpty());
    assertTrue(docSunch.select("#deletedCommentsLink").isEmpty());

    assertEquals(HttpStatus.SC_OK, crTailgunner.getStatus());
    Document docTailgunner = Jsoup.parse(crTailgunner.getEntityInputStream(), "UTF-8", "");
    assertFalse(docTailgunner.select("#loginGreating").isEmpty());
    assertTrue(docTailgunner.select("#deletedTopicsLink").isEmpty());
    assertTrue(docTailgunner.select("#deletedCommentsLink").isEmpty());

    assertEquals(HttpStatus.SC_OK, crWaker.getStatus());
    Document docWaker = Jsoup.parse(crWaker.getEntityInputStream(), "UTF-8", "");
    assertFalse(docWaker.select("#loginGreating").isEmpty());
    assertTrue(docWaker.select("#deletedTopicsLink").isEmpty());
    assertTrue(docWaker.select("#deletedCommentsLink").isEmpty());
  }

  @Test
  public void testSvu() throws IOException {
    Cookie authJB = WebHelper.doLoginCookie(resource, "svu", "passwd"); //
    ClientResponse crSunch = WebHelper.getPage(resource, authJB, "/people/Sun-ch/profile");
    ClientResponse crTailgunner = WebHelper.getPage(resource, authJB, "/people/tailgunner/profile");
    ClientResponse crWaker = WebHelper.getPage(resource, authJB, "/people/waker/profile");

    assertEquals(HttpStatus.SC_OK, crSunch.getStatus());
    Document docSunch = Jsoup.parse(crSunch.getEntityInputStream(), "UTF-8", "");
    assertFalse(docSunch.select("#loginGreating").isEmpty());
    assertFalse(docSunch.select("#deletedTopicsLink").isEmpty());
    assertFalse(docSunch.select("#deletedCommentsLink").isEmpty());

    assertEquals(HttpStatus.SC_OK, crTailgunner.getStatus());
    Document docTailgunner = Jsoup.parse(crTailgunner.getEntityInputStream(), "UTF-8", "");
    assertFalse(docTailgunner.select("#loginGreating").isEmpty());
    assertTrue(docTailgunner.select("#deletedTopicsLink").isEmpty());
    assertFalse(docTailgunner.select("#deletedCommentsLink").isEmpty());

    assertEquals(HttpStatus.SC_OK, crWaker.getStatus());
    Document docWaker = Jsoup.parse(crWaker.getEntityInputStream(), "UTF-8", "");
    assertFalse(docWaker.select("#loginGreating").isEmpty());
    assertTrue(docWaker.select("#deletedTopicsLink").isEmpty());
    assertTrue(docWaker.select("#deletedCommentsLink").isEmpty());
  }

  @Test
  public void testMaxcom() throws IOException {
    Cookie authJB = WebHelper.doLoginCookie(resource, "maxcom", "passwd"); //
    ClientResponse crSunch = WebHelper.getPage(resource, authJB, "/people/Sun-ch/profile");
    ClientResponse crTailgunner = WebHelper.getPage(resource, authJB, "/people/tailgunner/profile");
    ClientResponse crWaker = WebHelper.getPage(resource, authJB, "/people/waker/profile");

    assertEquals(HttpStatus.SC_OK, crSunch.getStatus());
    Document docSunch = Jsoup.parse(crSunch.getEntityInputStream(), "UTF-8", "");
    assertFalse(docSunch.select("#loginGreating").isEmpty());
    assertFalse(docSunch.select("#deletedTopicsLink").isEmpty());
    assertFalse(docSunch.select("#deletedCommentsLink").isEmpty());

    assertEquals(HttpStatus.SC_OK, crTailgunner.getStatus());
    Document docTailgunner = Jsoup.parse(crTailgunner.getEntityInputStream(), "UTF-8", "");
    assertFalse(docTailgunner.select("#loginGreating").isEmpty());
    assertTrue(docTailgunner.select("#deletedTopicsLink").isEmpty());
    assertFalse(docTailgunner.select("#deletedCommentsLink").isEmpty());

    assertEquals(HttpStatus.SC_OK, crWaker.getStatus());
    Document docWaker = Jsoup.parse(crWaker.getEntityInputStream(), "UTF-8", "");
    assertFalse(docWaker.select("#loginGreating").isEmpty());
    assertTrue(docWaker.select("#deletedTopicsLink").isEmpty());
    assertTrue(docWaker.select("#deletedCommentsLink").isEmpty());
  }

}
