package ru.org.linux.user;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class EditRegisterWebTest {
  private static final String LOCAL_SERVER = "http://127.0.0.1:8080";

  private WebResource resource;

  @Before
  public void initResource() {
    Client client = new Client();
    client.setFollowRedirects(false);
    resource = client.resource(LOCAL_SERVER);
  }

  @Test
  public void testChangePassword() {
    ClientResponse cr = resource
            .path("people/maxcom/edit")
            .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_OK, cr.getStatus());

  }

}