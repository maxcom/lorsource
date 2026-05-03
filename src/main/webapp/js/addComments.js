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

$script.ready(['jquery', 'hljs'], function() {
  'use strict';

  $(function() {
    const REPLY_TYPE = 1;
    const TOPIC_TYPE = 0;

    function getCookie(name) {
      const match = document.cookie.match(new RegExp(`(^|; )${name}=([^;]*)`));
      return match ? match[2] : null;
    }

    const commentForm = $("#commentForm");
    commentForm.append($('<div id="commentPreview">').hide());
    const commentPreview = $('#commentPreview');
    const commentFormContainer = commentForm.parent();

    const csrf = (getCookie("CSRF_TOKEN") || '').replace(/(^")|("$)/g, "");

    function updateCsrf() {
      if (csrf) {
        $("input[name='csrf']", commentForm).val(csrf);
      }
    }

    function updateAuthorReadonlyNote(authorReadonly) {
      $('#author-readonly-note').text(
        authorReadonly
          ? "Внимание! Вы отвечаете на комментарий, автор которого не может создавать новые комментарии в этом топике."
          : ""
      );
    }

    function moveAndShowForm(selector, replyToValue) {
      const replyTo = $("input[name='replyto']", commentFormContainer);
      if (replyTo.val() !== String(replyToValue)) {
        commentFormContainer.hide();
      }

      if (commentFormContainer.is(':hidden')) {
        const reply = $('div.reply', $('div.msg_body', $(selector)));
        reply.after(commentFormContainer);
        replyTo.val(replyToValue);
        commentFormContainer.slideDown('slow', function() { $("#msg").focus(); });
      } else {
        commentFormContainer.slideUp('slow');
      }
    }

    function toggleCommentForm(type, id, authorReadonly) {
      updateCsrf();
      updateAuthorReadonlyNote(authorReadonly);

      if (type === REPLY_TYPE) {
        moveAndShowForm('#comment-' + id, id);
      } else if (type === TOPIC_TYPE) {
        const topicId = $("input[name='topic']", commentFormContainer).val();
        moveAndShowForm('#topic-' + topicId, 0);
      }
    }

    $('div.reply').each(function() {
      const container = this;

      $('a[href^="comment-message.jsp"]', container).on("click", function(e) {
        e.preventDefault();
        toggleCommentForm(TOPIC_TYPE, 0, false);
      });

      const lnk = $('a[href^="add_comment.jsp"]', container);
      if (lnk.length > 0) {
        const ids = lnk.attr('href').match(/\d+/g);
        const commentId = ids[1];
        lnk.on("click", function(e) {
          e.preventDefault();
          toggleCommentForm(REPLY_TYPE, commentId, lnk.attr('data-author-readonly') === "true");
        });
      }
    });

    function warnOnUnload(e) {
      if ($("#msg").val() !== '' && !commentFormContainer.is(":hidden")) {
        e.returnValue = "Вы что-то напечатали в форме. Все введенные данные будут потеряны при закрытии страницы.";
        return e.returnValue;
      }
    }

    window.addEventListener('beforeunload', warnOnUnload);

    commentForm.on("reset", function() {
      commentFormContainer.slideUp('slow');
      commentPreview.hide().empty();
    });

    const previewButton = commentForm.find("button[name=preview]");
    previewButton.attr("type", "button");

    function startSpinner() {
      const spinner = $("<i class='icon-spin spinner' style='margin-left: 0.5em'>");
      commentForm.find(".form-actions button").last().after(spinner);
    }

    function stopSpinner() {
      commentForm.find(".spinner").remove();
    }

    function scrollToPreview() {
      commentPreview.show();

      const scrollTop = $(window).scrollTop();
      const viewportBottom = scrollTop + window.innerHeight;
      const previewTop = commentPreview.offset().top;

      if (previewTop < scrollTop || previewTop > viewportBottom) {
        $('html,body').animate({scrollTop: previewTop - window.innerHeight / 3}, 500);
      }
    }

    function displayPreview(data) {
      commentPreview.html(`<h2>Предпросмотр</h2>${data['preview']}`);
      $('pre code', commentPreview).each(function(_i, block) {
        hljs.highlightBlock(block);
      });

      if (data['errors']) {
        const errors = $('<div class="error">');
        $.each(data['errors'], function(_idx, v) {
          errors.append($("<span>").text(v));
          errors.append($("<br>"));
        });
        commentPreview.prepend(errors);
      }

      scrollToPreview();
    }

    function ajaxError(jqXHR, textStatus, errorThrown) {
      commentPreview.empty().append(
        $('<div class="error">')
          .text("Не удалось выполнить запрос, попробуйте повторить еще раз. " + errorThrown)
      );
      scrollToPreview();
    }

    function clearErrors() {
      $("div[error]").remove();
    }

    previewButton.on("click", function() {
      previewButton.prop("disabled", true);
      const form = commentForm.serialize() + "&preview=preview";

      startSpinner();
      clearErrors();

      $.ajax({
        method: "POST",
        url: "/add_comment_ajax",
        data: form,
        timeout: 10000
      }).always(function() {
        previewButton.prop("disabled", false);
        stopSpinner();
      }).fail(ajaxError).done(displayPreview);
    });

    let submitInProcess = false;

    commentForm.on("submit", function(e) {
      e.preventDefault();

      if (submitInProcess) {
        return;
      }

      submitInProcess = true;
      window.removeEventListener('beforeunload', warnOnUnload);

      const form = commentForm.serialize();

      startSpinner();
      clearErrors();

      $.ajax({
        method: "POST",
        url: "/add_comment_ajax",
        data: form,
        timeout: 30000
      }).always(function() {
        submitInProcess = false;
        stopSpinner();
      }).fail(function() {
        window.addEventListener('beforeunload', warnOnUnload);
        ajaxError.apply(this, arguments);
      }).done(function(data) {
        if (data['url']) {
          window.location.href = data['url'];
        } else {
          window.addEventListener('beforeunload', warnOnUnload);
          displayPreview(data);
        }
      });
    });
  });
});
