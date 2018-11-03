/*
 * Copyright 1998-2015 Linux.org.ru
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
  $(document).ready(function() {
    function getCookie(name) {
      return(document.cookie.match('(^|; )'+name+'=([^;]*)')||0)[2];
    }

    var commentForm = $("#commentForm");
    commentForm.append($("<div id=commentPreview>").hide());
    var commentPreview = $('#commentPreview');

    var commentFormContainer = commentForm.parent();

    var csrf = '';

    if (getCookie("CSRF_TOKEN")) {
      csrf = getCookie("CSRF_TOKEN").replace(/(^")|("$)/g, "");
    }

    function sh(type, id) {
      if (csrf.length>0) {
        $("input[name='csrf']").attr('value', csrf);
      }

      if (type == 1) {
        var reply_to = $("input[name='replyto']", commentFormContainer);
        if (reply_to.attr('value') != id) {
          commentFormContainer.hide();
        }

        if (commentFormContainer.is(':hidden')) {
          var reply = $('div.reply', $('div.msg_body', $('#comment-' + id)));
          reply.after(commentFormContainer);
          reply_to.attr('value', id);
          commentFormContainer.slideDown('slow', function() { $("#msg").focus(); });
        } else {
          commentFormContainer.slideUp('slow');
        }
      } else if (type == 0) {
        var topic_id = $("input[name='topic']", commentFormContainer).attr('value');

        reply_to = $("input[name='replyto']", commentFormContainer);
        if (reply_to.attr('value') != 0) {
          commentFormContainer.hide();
        }

        if (commentFormContainer.is(':hidden')) {
          var reply = $('div.reply', $('div.msg_body', $('#topic-' + topic_id)));
          reply.after(commentFormContainer);
          reply_to.attr('value', '0');
          commentFormContainer.slideDown('slow', function() { $("#msg").focus(); });
        } else {
          commentFormContainer.slideUp('slow');
        }
      }
    }

    $('div.reply').each(function() {
      $('a[href^="comment-message.jsp"]', this).bind("click", function() {
        sh(0, 0);
        return false;
      });

      var lnk = $('a[href^="add_comment.jsp"]', this);
      if (lnk.length>0) {
        var buff = lnk.attr('href').match(/\d+/g);
        var idr = buff[1];
        lnk.bind("click", function() {
          sh(1, idr);
          return false;
        });
      }
    });

    window.onbeforeunload = function() {
      if ($("#msg").val()!='' && ! $("#commentForm").parent().is(":hidden")) {
        return "Вы что-то напечатали в форме. Все введенные данные будут потеряны при закрытии страницы.";
      }
    };

    commentForm.bind("submit", function() {
      window.onbeforeunload = null;
    });

    commentForm.bind("reset", function() {
      commentFormContainer.slideUp('slow');
      commentPreview.hide();
      commentPreview.html('');
    });

    var previewButton = commentForm.find("button[name=preview]");
    previewButton.attr("type", "button");

    function startSpinner() {
      var spinner = $("<i class='icon-spin spinner' style='margin-left: 0.5em'>");

      commentForm.find(".form-actions button").last().after(spinner);
    }

    function stopSpinner() {
      commentForm.find(".spinner").remove();
    }

    function showPreview() {
      commentPreview.show();

      var visible_area_start = $(window).scrollTop();
      var visible_area_end = visible_area_start + window.innerHeight;

      var offset = commentPreview.offset().top;

      if(offset < visible_area_start || offset > visible_area_end) {
        $('html,body').animate({scrollTop: offset - window.innerHeight/3}, 500);
        return false;
      }
    }

    function displayPreview(data) {
      var title = "Предпросмотр";

      if (data['preview']['title']) {
        title = data['preview']['title'];
      }

      commentPreview.html("<h2>"+title+"</h2>"+data['preview']['processedMessage']);
      $('pre code', commentPreview).each(function(i, block) {
        hljs.highlightBlock(block);
      });

      if (data['errors']) {
        var errors = $("<div class=error>");
        $.each(data['errors'], function(idx, v) {
          errors.append($("<span>").text(v));
          errors.append($("<br>"));
        });

        commentPreview.prepend(errors);
      }

      showPreview();
    }

    function ajaxError(jqXHR, textStatus, errorThrown) {
      commentPreview.empty().append(
          $("<div class=error>")
              .text("Не удалось выполнить запрос, попробуйте повторить еще раз. " + errorThrown)
      );
      showPreview();
    }

    previewButton.click(function() {
      previewButton.prop("disabled", true);
      var form = commentForm.serialize();
      form = form+"&preview=preview";

      startSpinner();

      $("div[error]").remove();

      $.ajax({
        type: "POST",
        url: "/add_comment_ajax",
        data: form,
        timeout: 10000
      }).always(function () {
        previewButton.prop("disabled", false);
        stopSpinner();
      }).fail(ajaxError).done(displayPreview);
    });

    var submitInProcess = false;

    commentForm.submit(function() {
      if (!submitInProcess) {
        submitInProcess = true;

        var form = commentForm.serialize();

        startSpinner();

        $("div[error]").remove();

        $.ajax({
          type: "POST",
          url: "/add_comment_ajax",
          data: form,
          timeout: 30000
        }).always(function() { submitInProcess = false; stopSpinner(); })
            .fail(ajaxError)
            .done(function(data) {
              if (data['url']) {
                window.location.href = data['url'];
              } else {
                displayPreview(data);
              }
            });
      }

      return false;
    });
  });
});
