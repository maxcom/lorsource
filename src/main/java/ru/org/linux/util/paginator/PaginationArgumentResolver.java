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
package ru.org.linux.util.paginator;

import com.google.common.base.Enums;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Обработчик Pagination-параметров.
 */
public class PaginationArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter methodParameter) {
    return methodParameter.getParameterType().equals(Pagination.class);
  }


  private final static Pagination.Sort.Direction DEFAULT_SORT_DIRECTION = Pagination.Sort.Direction.DESC;
  private final static int DEFAULT_PAGE_NUMBER = 1;
  private final static int DEFAULT_PAGE_SIZE = 50;

  /**
   * @param methodParameter объект
   * @param webRequest      объект WEB-запроса
   * @return
   * @throws Exception
   */
  @Override
  public Object resolveArgument(
          MethodParameter methodParameter,
          ModelAndViewContainer modelAndViewContainer,
          NativeWebRequest webRequest,
          WebDataBinderFactory webDataBinderFactory
  ) throws Exception {

    Map<String, String[]> parametersMap = makeCaseInsensitiveParametersMap(webRequest);

    Integer pageNum = validateInteger(parametersMap, ParameterName.PAGE_NUMBER.getName(), DEFAULT_PAGE_NUMBER);
    Integer pageSize = validateInteger(parametersMap, ParameterName.PAGE_SIZE.getName(), DEFAULT_PAGE_SIZE);

    Pagination pagination = new Pagination(pageNum, pageSize);
    pagination.addSort(getSortByParameter(parametersMap));
    return pagination;
  }

  /**
   * Получить карту параметров WEB-запроса с регистронезависимыми именами параметров.
   *
   * @param webRequest объект WEB-запроса
   * @return карта параметров
   */
  @SuppressWarnings("unchecked")
  private Map<String, String[]> makeCaseInsensitiveParametersMap(NativeWebRequest webRequest) {
    Map<String, String[]> parametersMap = (Map<String, String[]>) new CaseInsensitiveMap();
    parametersMap.putAll(webRequest.getParameterMap());
    return parametersMap;
  }

  /**
   * Проверка на правильный формат числа и на позитивное значение.
   *
   * @param parametersMap карта с параметрами WEB-запроса
   * @param paramName     название папаметра
   * @return проверенное значение или 1 если параметр не указан или неверен.
   */
  private Integer validateInteger(Map<String, String[]> parametersMap, String paramName, Integer defaultValue) {
    Integer retValue;

    try {
      retValue = NumberUtils.createInteger(getParameterValue(parametersMap, paramName));
    } catch (NumberFormatException ignored) {
      retValue = defaultValue;
    }

    if (retValue == null || retValue < 1) {
      retValue = defaultValue;
    }
    return retValue;
  }

  /**
   * Парсер строки параметров сортировки.
   * Строка должна быть вида:
   * field1:DESC,field2:ASC,field3,field4:DESC,...
   *
   * @param parametersMap карта с параметрами WEB-запроса
   * @return список параметров сортировки
   */
  private List<Pagination.Sort> getSortByParameter(Map<String, String[]> parametersMap) {

    List<Pagination.Sort> sortList = new LinkedList<Pagination.Sort>();
    String value = getParameterValue(parametersMap, ParameterName.SORT_BY.getName());

    if (value != null) {
      // используется карта для избежания дубликатов.
      // используется связанная карта для сохранения порядка перечисления полей сортировки
      Map<String, Pagination.Sort.Direction> orderList = new LinkedHashMap<String, Pagination.Sort.Direction>();
      String[] sortInfos = value.split(",");
      for (String sortInfo : sortInfos) {
        String[] oneSortInfo = sortInfo.split(":");
        String sortName = oneSortInfo[0].trim().toLowerCase();
        if (!sortName.isEmpty()) {
          orderList.put(
              sortName,
              getSortDirectionFromString(oneSortInfo)
          );
        }
      }
      for (Map.Entry<String, Pagination.Sort.Direction> stringDirectionEntry : orderList.entrySet()) {
        sortList.add(
            new Pagination.Sort(stringDirectionEntry.getKey(), stringDirectionEntry.getValue())
        );
      }
    }
    return sortList;
  }

  /**
   * Получить направление сортировки.
   *
   * @param sortDirectionArray массив строк вида:  ([0] = название поля, [1] = направление сортировки)
   * @return направление сортировки
   */
  private Pagination.Sort.Direction getSortDirectionFromString(String[] sortDirectionArray) {
    String sortDirectionStr = (sortDirectionArray.length > 1) ? sortDirectionArray[1] : "";
    return Enums.getIfPresent(Pagination.Sort.Direction.class, sortDirectionStr.toUpperCase()).or(DEFAULT_SORT_DIRECTION);
  }

  /**
   * получить значение параметра.
   *
   * @param parametersMap карта с параметрами WEB-запроса
   * @param paramName     название папаметра
   * @return значение или null если параметр не указан.
   */
  private String getParameterValue(Map<String, String[]> parametersMap, String paramName) {
    if (!parametersMap.containsKey(paramName)) {
      return null;
    }
    return parametersMap.get(paramName)[0];
  }


  private enum ParameterName {
    PAGE_NUMBER("page"),
    PAGE_SIZE("page.size"),
    SORT_BY("sortBy");

    private final String name;

    public String getName() {
      return name;
    }

    private ParameterName(String name) {
      this.name = name;
    }
  }

}
