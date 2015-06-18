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

$script.ready('jquery', function() {
  $(document).ready(function() {
    var commentForm = $("#commentForm");
    commentForm.append($("<div id=commentPreview>").hide());
    var commentPreview = $('#commentPreview');

    var commentFormContainer = commentForm.parent();

    var csrf = '';

    if (document.cookie.match(/CSRF_TOKEN\=(\w+)\;?/)) {
      csrf = document.cookie.match(/CSRF_TOKEN\=(\w+)\;?/);
      csrf = csrf[1];
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
          reply.append(commentFormContainer);
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
          reply.append(commentFormContainer);
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
    previewButton.click(function() {
      previewButton.prop("disabled", true);
      var form = commentForm.serialize();
      form = form+"&preview=preview";
      $.post("/add_comment_ajax", form)
              .always(function() {
                previewButton.prop("disabled", false);
              })
              .done(function(data) {
        var title = "Предпросмотр";

        if (data['preview']['title']) {
          title = data['preview']['title'];
        }

        $("div[error]").remove();

        commentPreview.html("<h2>"+title+"</h2>"+data['preview']['processedMessage']);

        if (data['errors']) {
          var errors = $("<div class=error>");
          $.each(data['errors'], function(idx, v) {
            errors.append($("<span>").text(v));
            errors.append($("<br>"));
          });

          commentPreview.prepend(errors);
        }

        commentPreview.show();

        var visible_area_start = $(window).scrollTop();
        var visible_area_end = visible_area_start + window.innerHeight;

        var offset = commentPreview.offset().top;

        if(offset < visible_area_start || offset > visible_area_end) {
          // Not in view so scroll to it
          $('html,body').animate({scrollTop: offset - window.innerHeight/3}, 500);
          return false;
        }
      });
    })
  });
});

