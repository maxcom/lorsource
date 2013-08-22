/*
 * Copyright 1998-2013 Linux.org.ru
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
package ru.org.linux.csrf;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.RequestDataValueProcessor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static ru.org.linux.csrf.CSRFProtectionService.CSRF_ATTRIBUTE;
import static ru.org.linux.csrf.CSRFProtectionService.CSRF_INPUT_NAME;

//
// Thanks to http://blog.eyallupu.com/2012/04/csrf-defense-in-spring-mvc-31.html for the idea
//
@Component("requestDataValueProcessor")
public class CSRFRequestDataValueProcessor implements RequestDataValueProcessor {
  private static final Log logger = LogFactory.getLog(CSRFRequestDataValueProcessor.class);

  @Override
  public String processAction(HttpServletRequest request, String action) {
    return action;
  }

  @Override
  public String processFormFieldValue(HttpServletRequest request, String name, String value, String type) {
    return value;
  }

  @Override
  public Map<String, String> getExtraHiddenFields(HttpServletRequest request) {
    String csrfAttribute = (String) request.getAttribute(CSRF_ATTRIBUTE);

    if (csrfAttribute!=null) {
      return ImmutableMap.of(CSRF_INPUT_NAME, csrfAttribute);
    } else {
      logger.debug("missing CSRF attribute "+request.getRequestURI());

      return null;
    }
  }

  @Override
  public String processUrl(HttpServletRequest request, String url) {
    return url;
  }
}
