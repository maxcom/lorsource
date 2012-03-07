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

package ru.org.linux.topic;

import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.site.Template;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.SortedSet;

@Controller
public class TagsController {

  @Autowired
  private TagService tagService;

  /**
   * Обработчик по умолчанию. Показ тегов по самой первой букве.
   *
   * @return
   */
  @RequestMapping("/tags")
  public ModelAndView showDefaultTagListHandlertags(
    HttpServletRequest request
  )
    throws TagNotFoundException {
    return showTagListHandler("", request);
  }

  /**
   * Показ тегов по первой букве.
   *
   * @param firstLetter - фильтр: первая буква для тегов, которые должны быть показаны
   * @return
   */
  @RequestMapping("/tags/{firstLetter}")
  public ModelAndView showTagListHandler(
    @PathVariable String firstLetter,
    HttpServletRequest request
  )
    throws TagNotFoundException {
    ModelAndView modelAndView = new ModelAndView("tags");
    Template tmpl = Template.getTemplate(request);

    SortedSet<String> firstLetters = tagService.getFirstLetters(!tmpl.isModeratorSession());
    modelAndView.addObject("firstLetters", firstLetters);


    if (Strings.isNullOrEmpty(firstLetter)) {
      firstLetter = firstLetters.first();
    }
    modelAndView.addObject("currentLetter", firstLetter);

    Map<String, Integer> tags = tagService.getTagsByFirstLetter(firstLetter, !tmpl.isModeratorSession());

    if (tags.size() == 0) {
      throw new TagNotFoundException("Tag list is empty");
    }
    modelAndView.addObject("tags", tags);

    return modelAndView;
  }

  @Deprecated
  @RequestMapping("/tags.jsp")
  public String oldTagsRedirectHandler() {
    return "redirect:/tags";
  }
}
