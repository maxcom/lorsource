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

<%@ attribute name="forced" required="false" type="java.lang.Boolean" %>
<%@ attribute name="lazy" required="false" type="java.lang.Boolean" %>

<c:if test="${forced || captchaRequired}">
  <c:if test="${lazy}">
    <div class="h-captcha" data-sitekey="${configuration.captchaPublicKey}" data-lazy-captcha></div>
  </c:if>
  <c:if test="${not lazy}">
    <div class="captcha-state">
      <div class="captcha-loading"><i class="icon-spin spinner"></i>Капча загружается…</div>
      <div class="captcha-error">Не удалось загрузить капчу. Проверьте соединение и обновите страницу.</div>
      <div class="h-captcha" data-sitekey="${configuration.captchaPublicKey}"></div>
    </div>
    <script>
      (function () {
        var box = document.currentScript.previousElementSibling;
        if (!box) {
          return;
        }
        var widget = box.querySelector('.h-captcha');
        var sitekey = widget && widget.getAttribute('data-sitekey');
        var timer = null;
        var done = false;
        var hideLoading = function () {
          if (done) {
            return;
          }
          done = true;
          if (timer) {
            clearTimeout(timer);
          }
          window.removeEventListener('message', onMessage);
          box.classList.add('captcha-state--loaded');
        };
        // hCaptcha сообщает о готовности чекбокса через postMessage (label: checkbox-loaded);
        // событие load у iframe стреляет заметно раньше реальной отрисовки
        var onMessage = function (e) {
          if (typeof e.data !== 'string') {
            return;
          }
          try {
            var msg = JSON.parse(e.data);
            if (msg && msg.source === 'hcaptcha' && msg.label === 'checkbox-loaded') {
              hideLoading();
            }
          } catch (err) {
            // сообщение не от hCaptcha
          }
        };
        window.lorCaptchaFail = function () {
          box.classList.add('captcha-state--failed');
        };
        window.lorCaptchaReady = function () {
          window.addEventListener('message', onMessage);
          // страховка: если событие готовности так и не придёт
          timer = setTimeout(hideLoading, 20000);
          try {
            hcaptcha.render(widget, {sitekey: sitekey});
          } catch (err) {
            hideLoading(); // hCaptcha покажет собственный блок ошибки
          }
        };
      })();
    </script>
    <script src="https://js.hcaptcha.com/1/api.js?render=explicit&amp;onload=lorCaptchaReady" async defer
            onerror="if (window.lorCaptchaFail) { window.lorCaptchaFail(); }"></script>
  </c:if>
</c:if>
