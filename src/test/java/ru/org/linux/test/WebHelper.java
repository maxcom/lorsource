/*
 * Copyright 1998-2015 Linux.org.ru
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
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;
import ru.org.linux.csrf.CSRFProtectionService;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.io.IOException;

/**
 */
public class WebHelper {

  public static final String AUTH_COOKIE = "remember_me";
  public static final String MAIN_URL = "http://127.0.0.1:8080/";

  public static ClientResponse addPhoto(WebResource resource, String filename, String auth) {
    File file = new File(filename);

    FormDataMultiPart form = new FormDataMultiPart();
    form.bodyPart(new FormDataBodyPart("csrf", "csrf"));
    form.bodyPart(new FileDataBodyPart("file", file));

    return resource
        .path("addphoto.jsp")
        .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
        .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
        .type(MediaType.MULTIPART_FORM_DATA_TYPE)
        .post(ClientResponse.class, form);

  }

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
