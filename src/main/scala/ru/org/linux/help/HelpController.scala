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

package ru.org.linux.help

import javax.servlet.ServletRequest

import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.markdown.MarkdownRenderService

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

@Controller
class HelpController @Autowired() (renderService: MarkdownRenderService) {
  import HelpController._

  @RequestMapping(Array("/help/lorcode.md"))
  def helpPage(request:ServletRequest) = {
    val source = IOUtils.toString(request.getServletContext.getResource("/help/lorcode.md"))

    // TODO use DeferredResult ?
    // TODO handle timeout
    // TODO deadline
    val result = Await.result(renderService.render(source), RenderTimeout)

    new ModelAndView("help", Map(
      "title" -> "Разметка сообщений (LORCODE)",
      "helpText" -> result
    ).asJava)
  }
}

object HelpController {
  private val RenderTimeout = 30.seconds
}
