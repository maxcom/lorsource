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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.org.linux.rest.decorator.SectionDecorator;
import ru.org.linux.section.SectionService;

import java.util.List;

/**
 * Обработчик запросов секций.
 */
@Controller
@RequestMapping(value = Constants.URL_SECTION)
public class SectionRestController {

  private final SectionService sectionService;

  @Autowired
  public SectionRestController(SectionService sectionService) {
    this.sectionService = sectionService;
  }

  @RequestMapping(method = RequestMethod.GET)
  @ResponseBody
  public List<SectionDecorator> getSectionListHandler() {
    return SectionDecorator.getSections(sectionService.getSectionList());
  }

}
