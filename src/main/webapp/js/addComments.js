/*
 * Copyright 1998-2013 Linux.org.ru
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

var element = '';
var csrf = '';

function sh(type, id) {
  if (csrf.length>0) {
    $("input[name='csrf']").attr('value', csrf);
  }

  if (type == 1) {
    reply_to = $("input[name='replyto']", element);
    if (reply_to.attr('value') != id) {
      element.hide();
    }

    if (element.is(':hidden')) {
      reply = $('div.reply', $('div.msg_body', $('#comment-' + id)));
      reply.append(element);
      reply_to.attr('value', id);
      element.slideDown('slow', function() { $("#msg").focus(); });
    } else {
      element.slideUp('slow');
    }
  } else if (type == 0) {
    topic_id = $("input[name='topic']", element).attr('value');

    reply_to = $("input[name='replyto']", element);
    if (reply_to.attr('value') != 0) {
      element.hide();
    }

    if (element.is(':hidden')) {
      reply = $('div.reply', $('div.msg_body', $('#topic-' + topic_id)));
      reply.append(element);
      reply_to.attr('value', '0');
      element.slideDown('slow', function() { $("#msg").focus(); });
    } else {
      element.slideUp('slow');
    }
  }
}

$(document).ready(function() {
  element = $("#commentForm").parent();

  if (document.cookie.match(/CSRF_TOKEN\=(\w+)\;?/)) {
    csrf = document.cookie.match(/CSRF_TOKEN\=(\w+)\;?/);
    csrf = csrf[1];
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

  $("#commentForm").bind("submit", function() {
    window.onbeforeunload = null;
  });

  $("#commentForm").bind("reset", function() {
    element.slideUp('slow');
  })
});
