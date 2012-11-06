package ru.org.linux.test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import ru.org.linux.csrf.CSRFProtectionService;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

/**
 */
public class WebHelper {

  public static final String AUTH_COOKIE = "remember_me";
  public static final String MAIN_URL = "http://127.0.0.1:8080/";

  public static String doLogin(WebResource resource, String user, String password) throws IOException {
    MultivaluedMap<String, String> formData = new MultivaluedMapImpl();

    formData.add("nick", user);
    formData.add("passwd", password);
    formData.add("csrf", "csrf");

    ClientResponse cr = resource
            .path("login_process")
            .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
            .post(ClientResponse.class, formData);


    String auth = getAuthCookie(cr);
    if(auth != null) {
      return auth;
    }

    return null;
  }

  public static String getAuthCookie(ClientResponse cr) {
    for (Cookie cookie : cr.getCookies()) {
      if (cookie.getName().equals(AUTH_COOKIE)) {
        return cookie.getValue();
      }
    }

    System.out.println("cookies:" + cr.getCookies());
    return null;
  }


}
