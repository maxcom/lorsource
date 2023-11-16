/*
 * Copyright 1998-2023 Linux.org.ru
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
package ru.org.linux.section

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ExceptionHandler, RequestMapping, RequestParam, ResponseStatus}
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.group.{Group, GroupDao}
import ru.org.linux.section.Section.SECTION_FORUM
import ru.org.linux.section.SectionController.NonTech

import scala.jdk.CollectionConverters.*

@Controller
class SectionController(sectionService: SectionService, groupDao: GroupDao) {
  @RequestMapping(path = Array("/forum"))
  def forum(): ModelAndView = {
    val section = sectionService.getSection(SECTION_FORUM)

    val allGroups = groupDao.getGroups(section)

    val (other, tech) = allGroups.asScala.partition(g => NonTech.contains(g.id))

    new ModelAndView("forum", Map(
      "section" -> section,
      "tech" -> tech.asJava,
      "other" -> other.asJava
    ).asJava)
  }

  @RequestMapping(value = Array("/view-section.jsp"))
  def oldLink(@RequestParam("section") sectionid: Int) = new RedirectView(Section.getSectionLink(sectionid))

  @ExceptionHandler(Array(classOf[SectionNotFoundException]))
  @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleNotFoundException = new ModelAndView("errors/code404")
}

object SectionController {
  val NonTech: Set[Int] = Set(8404, 4068, 9326, 19405)

  def groupsSorted(groups: collection.Seq[Group]): collection.Seq[Group] =
    groups.sortBy(g => (SectionController.NonTech.contains(g.id), g.id))
}