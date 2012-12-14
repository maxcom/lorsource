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
package ru.org.linux.rest;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Before;
import ru.org.linux.test.WebHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Общий код для всех Acceptance-тестов.
 */
public class RestWebTestCommon {
  protected WebResource resource;
  protected final ObjectMapper mapper = new ObjectMapper();

  @Before
  public void initResource() throws Exception {
    Client client = new Client();
    client.setFollowRedirects(false);
    resource = client.resource(WebHelper.MAIN_URL);
  }

  protected final TypeReference<TreeMap<String, Object>> mapTypeRef
          = new TypeReference<TreeMap<String, Object>>() {
  };

  protected final TypeReference<LinkedList<Object>> listTypeRef
          = new TypeReference<LinkedList<Object>>() {
  };


  protected List<Object> convertJsonStringToCommonArray(String inputStr) throws IOException {
    return mapper.readValue(inputStr, listTypeRef);
  }

  protected List<Object> convertJsonStringToCommonArray(InputStream entityInputStream) throws IOException {
    return mapper.readValue(entityInputStream, listTypeRef);
  }

  protected Map<String, Object> convertJsonStringToCommonMap(InputStream entityInputStream) throws IOException {
    return mapper.readValue(entityInputStream, mapTypeRef);
  }

  protected Map<String, Object> convertJsonStringToCommonMap(String inputStr) throws IOException {
    return mapper.readValue(inputStr, mapTypeRef);
  }
}
