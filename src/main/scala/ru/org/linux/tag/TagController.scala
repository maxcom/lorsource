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
package ru.org.linux.tag

import com.google.common.base.Strings
import com.typesafe.scalalogging.StrictLogging
import io.circe.Json
import io.circe.syntax.*
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.{AuthorizedOpt, ModeratorOnly}
import ru.org.linux.topic.TagTopicListController

import scala.jdk.CollectionConverters.*

@Controller
class TagController(tagModificationService: TagModificationService, tagService: TagService, tagDao: TagCloudDao)
    extends StrictLogging {
  /**
   * Обработчик по умолчанию. Показ тегов по самой первой букве.
   *
   * @return объект web-модели
   */
  @RequestMapping(path = Array("/tags"))
  @throws[TagNotFoundException]
  def showDefaultTagListHandlertags: ModelAndView = showTagListHandler("")

  /**
   * Показ тегов по первой букве.
   *
   * @param firstLetter фильтр: первая буква для тегов, которые должны быть показаны
   * @return объект web-модели
   */
  @RequestMapping(path = Array("/tags/{firstLetter}"))
  @throws[TagNotFoundException]
  def showTagListHandler(@PathVariable firstLetter: String): ModelAndView = AuthorizedOpt { currentUser =>
    val modelAndView = new ModelAndView("tags")

    val firstLetters = tagService.getFirstLetters

    modelAndView.addObject("firstLetters", firstLetters.asJava)

    if (Strings.isNullOrEmpty(firstLetter)) {
      val list = tagDao.getTags(100)
      modelAndView.addObject("tagcloud", list)
    } else {
      modelAndView.addObject("currentLetter", firstLetter)

      val threshold = if (currentUser.exists(u => u.corrector || u.moderator)) {
        1
      } else {
        2
      }

      val tags = tagService.getTagsByPrefix(firstLetter, threshold)

      if (tags.isEmpty) {
        throw new TagNotFoundException("Tag list is empty")
      }

      modelAndView.addObject("tags", tags.asJava)
    }

    modelAndView
  }

  @RequestMapping(path = Array("/tags.jsp"))
  def oldTagsRedirectHandler = "redirect:/tags"

  /**
   * JSON-обработчик формирования списка тегов по начальным символам.
   *
   * @param term часть тега
   * @return Список тегов
   */
  @ResponseBody
  @RequestMapping(value = Array("/tags"), params = Array("term"))
  def showTagListHandlerJSON(@RequestParam("term") term: String): Json = {
    val tags = tagService.suggestTagsByPrefix(term, 10)

    tags.filter(TagName.isGoodTag).asJson
  }

  /**
   * Показ формы изменения существующего тега.
   *
   * @param firstLetter фильтр: первая буква для тегов, которые должны быть показаны
   * @param oldTagName  название редактируемого тега
   * @return объект web-модели
   */
  @RequestMapping(value = Array("/tags/change"), method = Array(RequestMethod.GET))
  @throws[AccessViolationException]
  def changeTagShowFormHandler(
                                @RequestParam(value = "firstLetter", required = false, defaultValue = "") firstLetter: String,
                                @RequestParam("tagName") oldTagName: String): ModelAndView = ModeratorOnly { _ =>
    val tagRequestChange = new TagRequest.Change

    tagRequestChange.setOldTagName(oldTagName)
    tagRequestChange.setTagName(oldTagName)

    val modelAndView = new ModelAndView("tags-change")

    modelAndView.addObject("firstLetter", firstLetter)
    modelAndView.addObject("tagRequestChange", tagRequestChange)

    modelAndView
  }

  /**
   * Обработка данных с формы изменения тега.
   *
   * @param firstLetter      фильтр: первая буква для тегов, которые должны быть показаны
   * @param tagRequestChange форма добавления тега
   * @param errors           обработчик ошибок ввода для формы
   * @return объект web-модели
   */
  @RequestMapping(value = Array("/tags/change"), method = Array(RequestMethod.POST))
  @throws[AccessViolationException]
  def changeTagSubmitHandler(
                              @RequestParam(value = "firstLetter", required = false, defaultValue = "") firstLetter: String,
                              @ModelAttribute("tagRequestChange") tagRequestChange: TagRequest.Change,
                              errors: Errors): ModelAndView = ModeratorOnly { currentUser =>
    if (tagService.getTagIdOpt(tagRequestChange.getOldTagName).isEmpty) {
      errors.rejectValue("oldTagName", "", "Тега с таким именем не существует!")
    }

    if (!TagName.isGoodTag(tagRequestChange.getTagName)) {
      errors.rejectValue("tagName", "", s"Некорректный тег: '${tagRequestChange.getTagName}'")
    } else if (tagService.getTagIdOpt(tagRequestChange.getTagName).isDefined) {
      errors.rejectValue("tagName", "", "Тег с таким именем уже существует!")
    }

    if (!errors.hasErrors) {
      tagModificationService.change(tagRequestChange.getOldTagName, tagRequestChange.getTagName)

      logger.info("Тег '{}' изменен пользователем {}", tagRequestChange.getOldTagName, currentUser.user.getNick)

      redirectToListPage(tagRequestChange.getTagName)
    } else {
      val modelAndView = new ModelAndView("tags-change")
      modelAndView.addObject("firstLetter", firstLetter)
      modelAndView
    }
  }

  /**
   * Показ формы удаления существующего тега.
   *
   * @param firstLetter фильтр: первая буква для тегов, которые должны быть показаны
   * @param oldTagName  название редактируемого тега
   * @return объект web-модели
   */
  @RequestMapping(value = Array("/tags/delete"), method = Array(RequestMethod.GET))
  @throws[AccessViolationException]
  def deleteTagShowFormHandler(@RequestParam(value = "firstLetter", required = false, defaultValue = "") firstLetter: String,
                               @RequestParam("tagName") oldTagName: String): ModelAndView = ModeratorOnly { _ =>
    val tagRequestDelete = new TagRequest.Delete

    tagRequestDelete.setOldTagName(oldTagName)

    val modelAndView = new ModelAndView("tags-delete")

    modelAndView.addObject("firstLetter", firstLetter)
    modelAndView.addObject("tagRequestDelete", tagRequestDelete)
    modelAndView.addObject("synonym", tagService.getTagBySynonym(oldTagName).isDefined)

    modelAndView
  }

  /**
   * Обработка данных с формы изменения тега.
   *
   * @param request форма удаления тега
   * @param errors           обработчик ошибок ввода для формы
   * @return объект web-модели
   */
  @RequestMapping(value = Array("/tags/delete"), method = Array(RequestMethod.POST))
  @throws[AccessViolationException]
  def deleteTagSubmitHandler(@ModelAttribute("tagRequestDelete") request: TagRequest.Delete,
                             errors: Errors): ModelAndView = ModeratorOnly { currentUser =>
    val synonym = tagService.getTagBySynonym(request.getOldTagName).isDefined

    val performDelete = synonym || Strings.isNullOrEmpty(request.getTagName)

    if (!synonym) {
      if (tagService.getTagIdOpt(request.getOldTagName).isEmpty) {
        errors.rejectValue("oldTagName", "", "Тега с таким именем не существует!")
      }

      if (!performDelete && !TagName.isGoodTag(request.getTagName)) {
        errors.rejectValue("tagName", "", "Некорректный тег: '" + request.getTagName + "'")
      }

      if (request.getOldTagName == request.getTagName) {
        errors.rejectValue("tagName", "", "Заменяемый тег не должен быть равен удаляемому!")
      }

      if (request.isCreateSynonym && performDelete) {
        errors.rejectValue("tagName", "", "Не указан тег для создания синонима!")
      }
    }

    if (!errors.hasErrors) {
      val firstLetter = if (performDelete) {
        tagModificationService.delete(request.getOldTagName)
        request.getOldTagName.substring(0, 1)
      } else {
        tagModificationService.merge(request.getOldTagName, request.getTagName, request.isCreateSynonym)
        request.getTagName.substring(0, 1)
      }

      logger.info("Тег '{}' удален пользователем {}", request.getOldTagName, currentUser.user.getNick)
      redirectToListPage(firstLetter)
    } else {
      val firstLetter = request.getOldTagName.substring(0, 1)

      val modelAndView = new ModelAndView("tags-delete")
      modelAndView.addObject("firstLetter", firstLetter)
      modelAndView.addObject("synonym", synonym)

      modelAndView
    }
  }

  /**
   * перенаправление на страницу показа списка тегов.
   *
   * @param tagName название тега
   * @return объект web-модели
   */
  private def redirectToListPage(tagName: String): ModelAndView = {
    val firstLetter = tagName.toLowerCase.charAt(0)
    val redirectUrl = TagTopicListController.tagsUrl(firstLetter)

    new ModelAndView(new RedirectView(redirectUrl))
  }
}