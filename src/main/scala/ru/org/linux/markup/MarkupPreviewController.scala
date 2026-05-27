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

package ru.org.linux.markup

import io.circe.syntax.*
import io.circe.{Encoder, Json}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}
import ru.org.linux.auth.AuthUtil.MaybeAuthorized
import ru.org.linux.msgbase.MessageText
import ru.org.linux.user.UserPermissionService

object MarkupPreviewController {
  private val MaxTextLength = 65536
}

@Controller
@RequestMapping(path = Array("/markup"))
class MarkupPreviewController(textService: MessageTextService) {

  @RequestMapping(path = Array("/preview"), method = Array(RequestMethod.POST),
    produces = Array("application/json; charset=UTF-8"))
  @ResponseBody
  def preview(@RequestParam(value = "text", required = false) text: String,
              @RequestParam(value = "markup", required = false) markupFormId: String): Json = MaybeAuthorized { implicit session =>
    val markupTypeOrError: Either[String, MarkupType] = Option(markupFormId) match
      case Some(formId) =>
        try Right(MarkupType.ofFormId(formId))
        catch case _: IllegalArgumentException => Left("Недопустимый режим разметки")
      case None =>
        Right(session.profile.formatMode)

    markupTypeOrError match
      case Left(error) =>
        MarkupPreviewResponse(error = Some(error), html = None).asJson
      case Right(markupType) =>
        val allowedFormats = UserPermissionService.allowedFormats(session.userOpt.orNull)
        if !allowedFormats.contains(markupType) then
          MarkupPreviewResponse(error = Some("Недопустимый режим разметки"), html = None).asJson
        else if text == null || text.isEmpty then
          MarkupPreviewResponse(error = None, html = Some("")).asJson
        else if text.length > MarkupPreviewController.MaxTextLength then
          MarkupPreviewResponse(error = Some("Слишком длинный текст"), html = None).asJson
        else
          val html = textService.renderCommentText(MessageText(text, markupType), nofollow = true)
          MarkupPreviewResponse(error = None, html = Some(html)).asJson
  }
}

case class MarkupPreviewResponse(error: Option[String], html: Option[String])

object MarkupPreviewResponse {
  given Encoder[MarkupPreviewResponse] = Encoder.instance { response =>
    val fields = List.newBuilder[(String, Json)]
    response.error.foreach(e => fields += ("error" -> Json.fromString(e)))
    response.html.foreach(h => fields += ("html" -> Json.fromString(h)))
    Json.fromFields(fields.result())
  }
}
