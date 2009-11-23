var element = '';
var jsid = '';

function sh(type, id) {
  $("input[name='session']").attr('value', jsid);

  if (type == 1) {
    reply_to = $("input[name='replyto']", element);
    if (reply_to.attr('value') != id) {
      element.hide();
    }

    if (element.is(':hidden')) {
      reply = $('div.reply', $('div.msg_body', $('#comment-' + id)));
      reply.append(element);
      reply_to.attr('value', id);
      element.slideDown('slow');
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
      element.slideDown('slow');
    } else {
      element.slideUp('slow');
    }
  }
}

$(document).ready(function() {
  element = $("#commentForm").parent();

  if (document.cookie.match(/JSESSIONID\=(\w+)\;?/)) {
    jsid = document.cookie.match(/JSESSIONID\=(\w+)\;?/);
    jsid = jsid[1];
  }

  $('div.reply').each(function() {
    $('a[href^=comment-message.jsp]', this).bind("click", function() {
      sh(0, 0);
      return false;
    });

    var lnk = $("a[href^=add_comment.jsp]", this);
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
    if ($("#msg").val()!='') {
      return "Вы что-то напечатали в форме. Все введенные данные будут потеряны при закрытии страницы.";
    }
  };

  $("#commentForm").bind("submit", function() {
    window.onbeforeunload = null;
  });
});