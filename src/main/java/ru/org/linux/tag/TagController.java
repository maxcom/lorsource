/*
 * Copyright 1998-2016 Linux.org.ru
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

package ru.org.linux.tag;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.Template;
import ru.org.linux.topic.TagTopicListController;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
public class TagController {
  private static final Logger logger = LoggerFactory.getLogger(TagController.class);

  private static final String REJECT_REASON = "недостаточно прав доступа";

  @Autowired
  private TagModificationService tagModificationService;

  @Autowired
  private TagService tagService;

  /**
   * Обработчик по умолчанию. Показ тегов по самой первой букве.
   *
   * @return объект web-модели
   */
  @RequestMapping("/tags")
  public ModelAndView showDefaultTagListHandlertags() throws TagNotFoundException {
    return showTagListHandler("");
  }

  /**
   * Показ тегов по первой букве.
   *
   * @param firstLetter фильтр: первая буква для тегов, которые должны быть показаны
   * @return объект web-модели
   */
  @RequestMapping("/tags/{firstLetter}")
  public ModelAndView showTagListHandler(
    @PathVariable String firstLetter
  )
    throws TagNotFoundException {
    ModelAndView modelAndView = new ModelAndView("tags");

    Collection<String> firstLetters = tagService.getFirstLetters();
    modelAndView.addObject("firstLetters", firstLetters);

    if (Strings.isNullOrEmpty(firstLetter)) {
      firstLetter = firstLetters.iterator().next();
    }

    modelAndView.addObject("currentLetter", firstLetter);

    Map<TagRef, Integer> tags = tagService.getTagsByPrefix(firstLetter, 1);

    if (tags.isEmpty()) {
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
  @ResponseBody
  @RequestMapping(value = "/tags", params = {"term"})
  public
  List<String> showTagListHandlerJSON(
    @RequestParam("term") final String term
  ) {
    Collection<String> tags = tagService.suggestTagsByPrefix(term, 10);

    return ImmutableList.copyOf(Iterables.filter(tags, TagName::isGoodTag));
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
    @RequestParam("tagName") String oldTagName
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

    if (tagService.getTagIdOpt(tagRequestChange.getOldTagName()).isEmpty()) {
      errors.rejectValue("oldTagName", "", "Тега с таким именем не существует!");
    }

    if (!TagName.isGoodTag(tagRequestChange.getTagName())) {
      errors.rejectValue("tagName", "", "Некорректный тег: '" + tagRequestChange.getTagName() + "'");
    } else {
      if (tagService.getTagIdOpt(tagRequestChange.getTagName()).isDefined()) {
        errors.rejectValue("tagName", "", "Тег с таким именем уже существует!");
      }
    }

    if (!errors.hasErrors()) {
      tagModificationService.change(tagRequestChange.getOldTagName(), tagRequestChange.getTagName());

      logger.info(
              "Тег '{}' изменен пользователем {}",
              tagRequestChange.getOldTagName(),
              template.getNick()
      );

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
    @RequestParam("tagName") String oldTagName
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

    if (tagService.getTagIdOpt(tagRequestDelete.getOldTagName()).isEmpty()) {
      errors.rejectValue("oldTagName", "", "Тега с таким именем не существует!");
    }

    if (!Strings.isNullOrEmpty(tagRequestDelete.getTagName())) {
      if (!TagName.isGoodTag(tagRequestDelete.getTagName())) {
        errors.rejectValue("tagName", "", "Некорректный тег: '"+tagRequestDelete.getTagName()+"'");
      }
    }

    if (Objects.equals(tagRequestDelete.getOldTagName(), tagRequestDelete.getTagName())) {
      errors.rejectValue("tagName", "", "Заменяемый тег не должен быть равен удаляемому!");
    }

    if (!errors.hasErrors()) {
      if (Strings.isNullOrEmpty(tagRequestDelete.getTagName())) {
        tagModificationService.delete(tagRequestDelete.getOldTagName());
      } else {
        tagModificationService.merge(tagRequestDelete.getOldTagName(), tagRequestDelete.getTagName());
      }

      logger.info("Тег '{}' удален пользователем {}", tagRequestDelete.getOldTagName(), template.getNick());

      return redirectToListPage(firstLetter);
    } else {
      ModelAndView modelAndView = new ModelAndView("tags-delete");
      modelAndView.addObject("firstLetter", firstLetter);

      return modelAndView;
    }
  }

  /**
   * перенаправление на страницу показа списка тегов.
   *
   * @param tagName название тега
   * @return объект web-модели
   */
  private ModelAndView redirectToListPage(String tagName) {
    char firstLetter = tagName.toLowerCase().charAt(0);
    String redirectUrl = TagTopicListController.tagsUrl(firstLetter);
    return new ModelAndView(new RedirectView(redirectUrl));
  }
}
