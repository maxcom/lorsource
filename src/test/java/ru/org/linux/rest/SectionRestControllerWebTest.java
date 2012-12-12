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

import com.sun.jersey.api.client.ClientResponse;
import junit.framework.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * Acceptance тесты для SectionRestController
 */
public class SectionRestControllerWebTest extends RestWebTestCommon {


  @Test
  public void test1SectionList() throws IOException {
    // given

    // when
    ClientResponse cr = resource
            .path("/api/sections/")
            .get(ClientResponse.class);

    List<Object> actualResult = convertJsonStringToCommonArray(cr.getEntityInputStream());
    // then
    Assert.assertEquals(4, actualResult.size());

    String expectedValue = "[{\"id\":1,\"alias\":\"news\",\"link\":\"/api/sections/1\",\"title\":\"news\"}," +
            "{\"id\":2,\"alias\":\"forum\",\"link\":\"/api/sections/2\",\"title\":\"forum\"}," +
            "{\"id\":3,\"alias\":\"gallery\",\"link\":\"/api/sections/3\",\"title\":\"gallery\"}," +
            "{\"id\":5,\"alias\":\"polls\",\"link\":\"/api/sections/5\",\"title\":\"polls\"}]";
    List<Object> expectedResult = convertJsonStringToCommonArray(expectedValue);

    Assert.assertEquals(expectedResult, actualResult);

  }
}
