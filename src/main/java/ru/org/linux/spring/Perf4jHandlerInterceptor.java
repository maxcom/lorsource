/*
 * Copyright 1998-2010 Linux.org.ru
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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

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
    CommonsLogStopWatch watch = new CommonsLogStopWatch(handler.getClass().getSimpleName());

    watch.setTimeThreshold(TIME_THRESHOLD);

    request.setAttribute(ATTRIBUTE, watch);

    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    StopWatch stopWatch = (StopWatch) request.getAttribute(ATTRIBUTE);

    stopWatch.stop();
  }
}
