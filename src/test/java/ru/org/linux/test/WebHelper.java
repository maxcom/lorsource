/*
 * Copyright 1998-2022 Linux.org.ru
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

package ru.org.linux.test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.Assert;
import ru.org.linux.csrf.CSRFProtectionService;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;

public class WebHelper {

  public static final String AUTH_COOKIE = "remember_me";
  public static final String MAIN_URL = "http://127.0.0.1:8080/";

  public static String doLogin(WebResource resource, String user, String password) {
    MultivaluedMap<String, String> formData = new MultivaluedMapImpl();

    formData.add("nick", user);
    formData.add("passwd", password);
    formData.add("csrf", "csrf");

    ClientResponse cr = resource
            .path("login_process")
            .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
            .post(ClientResponse.class, formData);

    Assert.assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr.getStatus());

    return getAuthCookie(cr);
  }

  public static String getAuthCookie(ClientResponse cr) {
    for (Cookie cookie : cr.getCookies()) {
      if (cookie.getName().equals(AUTH_COOKIE)) {
        return cookie.getValue();
      }
    }

    return null;
  }
}
