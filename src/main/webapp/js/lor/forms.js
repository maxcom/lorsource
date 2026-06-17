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

$(document).ready(function () {
  function initCtrlEnter() {
    function ctrl_enter(e, form) {
      if (((e.keyCode === 13) || (e.keyCode === 10)) && (e.ctrlKey || e.metaKey)) {
        window.onbeforeunload = null;

        $(form).trigger('submit');

        return false;
      }
    }

    $('textarea').on('keypress', function (e) {
      ctrl_enter(e, e.target.form);
    });
  }

  function initClearWarningForm() {
    $('.clear-warning-form').on('submit', function (e) {
      e.preventDefault();
      const form = $(this);
      $.ajax({
        url: form.attr('action'),
        method: 'POST',
        data: form.serialize(),
        success: function () {
          form.hide();
          form.parent().wrap("<s></s>");
        }
      });
    });
  }

  initCtrlEnter();
  initClearWarningForm();
});