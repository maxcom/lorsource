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

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.Template;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

@Controller
public class TagController {

  private static final String REJECT_REASON = "недостаточно прав доступа";

  @Autowired
  private TagService tagService;

  /**
   * Обработчик по умолчанию. Показ тегов по самой первой букве.
   *
   * @return объект web-модели
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
   * @param firstLetter фильтр: первая буква для тегов, которые должны быть показаны
   * @return объект web-модели
   */
  @RequestMapping("/tags/{firstLetter}")
  public ModelAndView showTagListHandler(
    @PathVariable String firstLetter,
    HttpServletRequest request
  )
    throws TagNotFoundException {
    ModelAndView modelAndView = new ModelAndView("tags");

    Template tmpl = Template.getTemplate(request);
    modelAndView.addObject("isModeratorSession", tmpl.isModeratorSession());

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

  /**
   * JSON-обработчик формирования списка тегов по начальным символам.
   *
   * @param term часть тега
   * @return Список тегов
   */
  @RequestMapping(value = "/tags", params = {"term"})
  public
  @ResponseBody
  List<String> showTagListHandlerJSON(
    @RequestParam("term") final String term
  ) {
    Map<String, Integer> tags = tagService.getTagsByFirstLetter(term.substring(0, 1), false);

    return ImmutableList.copyOf(Iterables.filter(tags.keySet(), new Predicate<String>() {
      @Override
      public boolean apply(String input) {
        return input.startsWith(term);
      }
    }));
  }

  /**
   * Показ формы изменения существующего тега.
   *
   * @param request     данные запроса от web-клиента
   * @param firstLetter фильтр: первая буква для тегов, которые должны быть показаны
   * @param oldTagName  название редактируемого тега
   * @return объект web-модели
   */
  @RequestMapping(value = "/tags/change", method = RequestMethod.GET)
  public ModelAndView changeTagShowFormHandler(
    HttpServletRequest request,
    @RequestParam(value = "firstLetter", required = false, defaultValue = "") String firstLetter,
    @RequestParam(value = "tagName") String oldTagName
  ) throws AccessViolationException {
    Template template = Template.getTemplate(request);
    if (!template.isModeratorSession()) {
      throw new AccessViolationException(REJECT_REASON);
    }
    TagRequest.Change tagRequestChange = new TagRequest.Change();
    tagRequestChange.setOldTagName(oldTagName);
    tagRequestChange.setTagName(oldTagName);
    ModelAndView modelAndView = new ModelAndView("tags-change");
    modelAndView.addObject("firstLetter", firstLetter);
    modelAndView.addObject("tagRequestChange", tagRequestChange);
    return modelAndView;
  }

  /**
   * Обработка данных с формы изменения тега.
   *
   * @param request          данные запроса от web-клиента
   * @param firstLetter      фильтр: первая буква для тегов, которые должны быть показаны
   * @param tagRequestChange форма добавления тега
   * @param errors           обработчик ошибок ввода для формы
   * @return объект web-модели
   */
  @RequestMapping(value = "/tags/change", method = RequestMethod.POST)
  public ModelAndView changeTagSubmitHandler(
    HttpServletRequest request,
    @RequestParam(value = "firstLetter", required = false, defaultValue = "") String firstLetter,
    @ModelAttribute("tagRequestChange") TagRequest.Change tagRequestChange,
    Errors errors
  ) throws AccessViolationException {
    Template template = Template.getTemplate(request);
    if (!template.isModeratorSession()) {
      throw new AccessViolationException(REJECT_REASON);
    }

    tagService.change(tagRequestChange.getOldTagName(), tagRequestChange.getTagName(), errors);

    if (!errors.hasErrors()) {
      return redirectToListPage(tagRequestChange.getTagName());
    }

    ModelAndView modelAndView = new ModelAndView("tags-change");
    modelAndView.addObject("firstLetter", firstLetter);
    return modelAndView;
  }

  /**
   * Показ формы удаления существующего тега.
   *
   * @param request     данные запроса от web-клиента
   * @param firstLetter фильтр: первая буква для тегов, которые должны быть показаны
   * @param oldTagName  название редактируемого тега
   * @return объект web-модели
   */
  @RequestMapping(value = "/tags/delete", method = RequestMethod.GET)
  public ModelAndView deleteTagShowFormHandler(
    HttpServletRequest request,
    @RequestParam(value = "firstLetter", required = false, defaultValue = "") String firstLetter,
    @RequestParam(value = "tagName") String oldTagName
  ) throws AccessViolationException {
    Template template = Template.getTemplate(request);
    if (!template.isModeratorSession()) {
      throw new AccessViolationException(REJECT_REASON);
    }
    TagRequest.Delete tagRequestDelete = new TagRequest.Delete();
    tagRequestDelete.setOldTagName(oldTagName);
    ModelAndView modelAndView = new ModelAndView("tags-delete");
    modelAndView.addObject("firstLetter", firstLetter);
    modelAndView.addObject("tagRequestDelete", tagRequestDelete);
    return modelAndView;
  }


  /**
   * Обработка данных с формы изменения тега.
   *
   * @param request          данные запроса от web-клиента
   * @param firstLetter      фильтр: первая буква для тегов, которые должны быть показаны
   * @param tagRequestDelete форма удаления тега
   * @param errors           обработчик ошибок ввода для формы
   * @return объект web-модели
   */
  @RequestMapping(value = "/tags/delete", method = RequestMethod.POST)
  public ModelAndView deleteTagSubmitHandler(
    HttpServletRequest request,
    @RequestParam(value = "firstLetter", required = false, defaultValue = "") String firstLetter,
    @ModelAttribute("tagRequestDelete") TagRequest.Delete tagRequestDelete,
    Errors errors
  ) throws AccessViolationException {
    Template template = Template.getTemplate(request);
    if (!template.isModeratorSession()) {
      throw new AccessViolationException(REJECT_REASON);
    }

    tagService.delete(tagRequestDelete.getOldTagName(), tagRequestDelete.getTagName(), errors);

    if (!errors.hasErrors()) {
      return redirectToListPage(firstLetter);
    }

    ModelAndView modelAndView = new ModelAndView("tags-delete");
    modelAndView.addObject("firstLetter", firstLetter);
    return modelAndView;
  }

  /**
   * перенаправление на страницу показа списка тегов.
   *
   * @param tagName название тега
   * @return объект web-модели
   */
  private ModelAndView redirectToListPage(String tagName) {
    String firstLetter = String.valueOf(tagName.toLowerCase().charAt(0));
    String redirectUrl;
    try {
      redirectUrl = "/tags/" + URLEncoder.encode(firstLetter, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      redirectUrl = "/tags";
    }
    ModelAndView modelAndView = new ModelAndView(new RedirectView(redirectUrl));
    return modelAndView;
  }
}
