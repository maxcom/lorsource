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

package ru.org.linux.spring;

import org.perf4j.StopWatch;
import org.perf4j.commonslog.CommonsLogStopWatch;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Perf4jHandlerInterceptor extends HandlerInterceptorAdapter {
  private static final String ATTRIBUTE = "perf4jStopWatch";
  private static final int TIME_THRESHOLD = 500;

  @Override
  public boolean preHandle(
          HttpServletRequest request,
          HttpServletResponse response,
          Object handler
  ) throws Exception {
    if (handler instanceof ResourceHttpRequestHandler || handler instanceof DefaultServletHttpRequestHandler) {
      return true;
    }

    String name;

    if (handler instanceof HandlerMethod) {
      name = ((HandlerMethod) handler).getBeanType().getSimpleName();
    } else {
      name = handler.getClass().getSimpleName();
    }

    CommonsLogStopWatch watch = new CommonsLogStopWatch(name);

    watch.setTimeThreshold(TIME_THRESHOLD);

    request.setAttribute(ATTRIBUTE, watch);

    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    StopWatch stopWatch = (StopWatch) request.getAttribute(ATTRIBUTE);

    if (stopWatch!=null) {
      stopWatch.stop();
    }
  }
}
