<%--
  ~ Copyright 1998-2023 Linux.org.ru
  ~    Licensed under the Apache License, Version 2.0 (the "License");
  ~    you may not use this file except in compliance with the License.
  ~    You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~    Unless required by applicable law or agreed to in writing, software
  ~    distributed under the License is distributed on an "AS IS" BASIS,
  ~    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~    See the License for the specific language governing permissions and
  ~    limitations under the License.
  --%>
<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@ attribute name="comment" required="true" type="ru.org.linux.comment.PreparedComment" %>
<%@ attribute name="topic" required="true" type="ru.org.linux.topic.Topic" %>
<%@ attribute name="showMenu" required="true" type="java.lang.Boolean" %>
<%@ attribute name="commentsAllowed" required="true" type="java.lang.Boolean" %>
<%@ attribute name="reactionList" required="false" type="ru.org.linux.reaction.PreparedReactionList" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<article class="msg" id="comment-${comment.id}">
  <div class="title">
    <c:if test="${comment.deleted}">
      <strong>Сообщение удалено
        <c:if test="${comment.deleteInfo != null}">
          ${comment.deleteInfo.nick} по причине <c:out value="${comment.deleteInfo.reason}" escapeXml="true"/>
        </c:if>
      </strong>

      <c:if test="${comment.undeletable}">
        &emsp;[<a href="/undelete_comment?msgid=${comment.id}">Восстановить</a>]
      </c:if>

      <br>
    </c:if>

    <c:if test="${comment.reply!=null}">
      Ответ на:
      <c:if test="${comment.reply.deleted}">удаленный комментарий</c:if>
      <c:if test="${not comment.reply.deleted}">
        <a href="${topic.link}?cid=${comment.reply.id}" data-samepage="${comment.reply.samePage}">
          <c:if test="${comment.reply.title!=null}">
            <l:title>${comment.reply.title}</l:title>
          </c:if>
          <c:if test="${comment.reply.title==null}">комментарий</c:if>
        </a>
        от ${comment.reply.author}<c:out value=" "/><lor:date date="${comment.reply.postdate}"/>
      </c:if>
    </c:if>
  </div>

  <div class="msg-container">
    <c:set var="body_style" value=""/>
    <c:if test="${comment.userpic !=null}">
      <l:userpic userpic="${comment.userpic}"/>
      <c:set var="body_style" value="message-w-userpic"/>
    </c:if>

    <div class="msg_body ${body_style}">
      <c:if test="${comment.title!=null}">
        <h1><l:title><c:out value="${comment.title}" escapeXml="true"/></l:title></h1>
      </c:if>

      ${comment.processedMessage}

      <div class="sign">
        <lor:sign user="${comment.author}" postdate="${comment.postdate}"/>

        <c:if test="${comment.author.id == topic.authorUserId and not comment.author.anonymous}">
          <span class="user-tag">автор топика</span>
        </c:if>

        <c:if test="${comment.remark!=null}">
          <span><c:out value="${comment.remark}" escapeXml="true"/></span>
        </c:if>

        <c:if test="${comment.postIP!=null}">
          <a href="sameip.jsp?ip=${comment.postIP}">${comment.postIP}</a>
        </c:if>

        <c:if test="${comment.editSummary!=null}">
          <span class="sign_more">
            <br>
            Последнее исправление: ${comment.editSummary.editNick}<c:out value=" "/>
            <lor:date date="${comment.editSummary.editDate}"/>
            (всего
              <a href="${topic.link}/${comment.id}/history">
                исправлений: ${comment.editSummary.editCount}
              </a>)
          </span>
        </c:if>

        <c:if test="${comment.userAgent!=null}">
          <br>
          <span class="sign_more">
            <c:out value="${comment.userAgent}" escapeXml="true"/>
            &nbsp;<a href="sameip.jsp?ua=${comment.userAgentId}&ip=${comment.postIP}&mask=0">&#x1f50d;</a>
          </span>
        </c:if>
      </div>

      <c:if test="${not comment.deleted && showMenu}">
        <div class="reply">
          <ul>
            <c:if test="${commentsAllowed}">
              <li><a href="add_comment.jsp?topic=${topic.id}&replyto=${comment.id}" data-author-readonly="${comment.authorReadonly}">Ответить<span class="hideon-phone"> на это сообщение</span></a></li>
            </c:if>

            <c:if test="${comment.reactions.emptyMap and comment.reactions.allowInteract}">
              <li><a class="reaction-show" href="/reactions?topic=${topic.id}&comment=${comment.id}">Реакции</a></li>
            </c:if>

            <c:if test="${comment.editable}">
              <li><a href="/edit_comment?original=${comment.id}&topic=${topic.id}">Править</a></li>
            </c:if>

            <c:if test="${comment.deletable}">
              <li><a href="/delete_comment.jsp?msgid=${comment.id}">Удалить</a></li>
            </c:if>

            <c:if test="${comment.answerCount > 1}">
              <li><a href="${comment.answerLink}">Показать ответы</a></li>
            </c:if>

            <c:if test="${comment.answerCount == 1}">
              <li><a href="${comment.answerLink}" data-samepage="${comment.answerSamepage}">Показать ответ</a></li>
            </c:if>

            <li><a href="${topic.link}?cid=${comment.id}">Ссылка</a></li>
          </ul>
        </div>
      </c:if>

      <lor:reactions reactions="${comment.reactions}" reactionList="${reactionList}" topic="${topic}"
                     comment="${comment}"/>
    </div>
  </div>

</article>


