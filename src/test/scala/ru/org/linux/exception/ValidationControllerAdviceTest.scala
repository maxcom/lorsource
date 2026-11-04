/*
 * Copyright 1998-2026 Linux.org.ru
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

package ru.org.linux.exception

import munit.FunSuite
import org.springframework.core.MethodParameter
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.web.bind.MethodArgumentNotValidException

class ValidationControllerAdviceTest extends FunSuite:
  test("MethodArgumentNotValidException рендерится страницей errors/bad-parameter"):
    val bindingResult = new BeanPropertyBindingResult(new AnyRef, "add")
    bindingResult.reject(null, "нельзя добавлять в устаревшие темы")

    val parameter = new MethodParameter(classOf[ValidationControllerAdvice].getConstructors.head, -1)
    val exception = new MethodArgumentNotValidException(parameter, bindingResult)

    val modelAndView = new ValidationControllerAdvice().methodArgumentNotValid(exception)

    assertEquals(modelAndView.getViewName, "errors/bad-parameter")
    assert(modelAndView.getModel.containsKey("exception"))
    assert(modelAndView.getModel.get("exception").eq(exception))
