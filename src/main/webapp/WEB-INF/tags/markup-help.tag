<%--
  ~ Copyright 1998-2026 Linux.org.ru
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
<%@ tag pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ attribute name="mode" required="true" type="java.lang.String" %>

<c:if test="${mode == 'lorcode'}">
<b>Внимание:</b> прочитайте описание разметки <a href="/help/lorcode.md" target="_blank" title="[br] - перевод строки

[b]жирный текст[/b]

[i]курсив[/i]

[u]подчёркнутый текст[/u]

[s]зачёркнутый текст[/s]

[em]emphasis[/em]

[strong]stronger emphasis[/strong]

[pre]preformatted text[/pre]

[user]maxcom[/user] - ссылка на профиль пользователя.
При использовании этого тега упомянутому пользователю приходит уведомление

[code]код[/code]

[inline]Строчное оформление кода[/inline]

Цитата:
[quote]цитата[/quote] или
[quote='название цитаты']цитата[/quote] или
>>цитата

Ссылка:
[url]http://www.linux.org.ru/[/url]
можно с параметром, например:
[url=http://www.example.com/]Сюда![/url]">LORCODE</a>.
</c:if>
<c:if test="${mode == 'markdown'}">
<b>Внимание:</b> прочитайте описание разметки <a target="_blank" href="/help/markdown.md">Markdown</a>.
</c:if>
