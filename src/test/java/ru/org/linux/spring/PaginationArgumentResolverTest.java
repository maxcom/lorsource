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
package ru.org.linux.spring;

import junit.framework.Assert;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import ru.org.linux.util.Pagination;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link PaginationArgumentResolver}
 */
@RunWith(JUnitParamsRunner.class)
public class PaginationArgumentResolverTest {
  private PaginationArgumentResolver paginationArgumentResolver;
  private MethodParameter methodParameter;
  private NativeWebRequest webRequest;

  @Before
  public void setUp() throws Exception {
    paginationArgumentResolver = new PaginationArgumentResolver();
    methodParameter = mock(MethodParameter.class);

    final Class paginationClass = Pagination.class;
    when(methodParameter.getParameterType())
            .thenReturn(paginationClass);
    webRequest = mock(NativeWebRequest.class);
  }


  @Test
  @Parameters(method = "parsePageNumDataSource")
  public void parsePageNum(
          String parameterName,
          String parameterValue,
          int expectedParameterValue
  ) throws Exception {
    // given
    Map<String, String[]> parametersMap = new HashMap<String, String[]>();
    parametersMap.put(parameterName, new String[]{parameterValue});

    when(webRequest.getParameterMap())
            .thenReturn(parametersMap);

    // when
    Pagination pagination = (Pagination) paginationArgumentResolver.resolveArgument(methodParameter, null, webRequest, null);

    // then
    Assert.assertEquals(expectedParameterValue, pagination.getIndex());
  }

  public Object[][] parsePageNumDataSource() {
    return new Object[][]{
            new Object[]{"page", "-5", 1},
            new Object[]{"page", "0", 1},
            new Object[]{"PAGE", "5", 5},
            new Object[]{"Page", "blabla", 1},
            new Object[]{"page.size", "4", 1},
    };
  }

  @Test
  @Parameters(method = "parsePageSizeDataSource")
  public void parsePageSize(
          String parameterName,
          String parameterValue,
          int expectedParameterValue
  ) throws Exception {
    // given
    Map<String, String[]> parametersMap = new HashMap<String, String[]>();
    parametersMap.put(parameterName, new String[]{parameterValue});

    when(webRequest.getParameterMap())
            .thenReturn(parametersMap);

    // when
    Pagination pagination = (Pagination) paginationArgumentResolver.resolveArgument(methodParameter, null, webRequest, null);

    // then
    Assert.assertEquals(expectedParameterValue, pagination.getSize());
  }

  public Object[][] parsePageSizeDataSource() {
    return new Object[][]{
            new Object[]{"page.size", "-5", 50},
            new Object[]{"page.size", "0", 50},
            new Object[]{"PAGE.size", "30", 30},
            new Object[]{"Page.size", "blabla", 50},
            new Object[]{"page", "4", 50},
    };
  }

  @Test
  @Parameters(method = "parseSortDataSource")
  public void parseSort(
          String parameterName,
          String parameterValue,
          List<Pagination.Sort> expectedParameterValue
  ) throws Exception {
    // given
    Map<String, String[]> parametersMap = new HashMap<String, String[]>();
    parametersMap.put(parameterName, new String[]{parameterValue});

    when(webRequest.getParameterMap())
            .thenReturn(parametersMap);

    // when
    Pagination pagination = (Pagination) paginationArgumentResolver.resolveArgument(methodParameter, null, webRequest, null);

    // then
    Assert.assertEquals(expectedParameterValue, pagination.getSortList());
  }

  public Object[][] parseSortDataSource() {
    List<Pagination.Sort> emptySortList = new LinkedList<Pagination.Sort>();

    List<Pagination.Sort> sortList1 = new LinkedList<Pagination.Sort>();
    sortList1.add(new Pagination.Sort("field1", Pagination.Sort.Direction.DESC));

    List<Pagination.Sort> sortList2 = new LinkedList<Pagination.Sort>();
    sortList2.add(new Pagination.Sort("field1", Pagination.Sort.Direction.ASC));
    sortList2.add(new Pagination.Sort("field2", Pagination.Sort.Direction.DESC));
    sortList2.add(new Pagination.Sort("field3", Pagination.Sort.Direction.DESC));
    sortList2.add(new Pagination.Sort("field4", Pagination.Sort.Direction.ASC));

    return new Object[][]{
            new Object[]{"sortBy", "", emptySortList},
            new Object[]{"sortBy", "field1", sortList1},
            new Object[]{"SORTBY", "field1", sortList1},
            new Object[]{"SortBY", "field1", sortList1},
            new Object[]{"sortBy", "field1:field2:field3:ASC:FIELD4", sortList1},
            new Object[]{"sortBy", "field1:ASC,field2:BLABLA,FIELD3:desc,field4:asc", sortList2},
    };
  }

}
