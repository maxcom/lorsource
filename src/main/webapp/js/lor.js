/*
 * Copyright 1998-2012 Linux.org.ru
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

// addtag.js
function initTopTagSelection() {
  $(function() {
    function addTag() {
      var tags = $('#tags');
      var curVal = tags.val();

      if (curVal != "") {
        curVal += ",";
      }

      tags.val(curVal + $(this).text());
    }

    $('a[data-toptag]').click(addTag);
  });
}

function initNextPrevKeys() {
  $(function() {
    function jump(link) {
      if (link && link.href) {
        document.location = link.href;
      }
    }

    if (typeof  jQuery.hotkeys !== 'undefined') {
      $(document).bind('keydown', {combi:'Ctrl+left', disableInInput: true}, function() {
        jump(document.getElementById('PrevLink'))
      });
      $(document).bind('keydown', {combi:'Ctrl+right', disableInInput: true}, function() {
        jump(document.getElementById('NextLink'))
      });
    }
  });
}

function initStarPopovers() {
  $(function() {
      $("#favs_button").click(function(event) {
          event.preventDefault();
          event.stopPropagation();
          $("#memories_button").popover('hide');
          $("#favs_button").popover('show');
      });
      $("#favs_button").popover({
          content: "Для добавления в избранное надо залогиниться!",
          autoReposition: false,
          trigger: 'manual'
      });

      $("#memories_button").click(function(event) {
           event.preventDefault();
           event.stopPropagation();
           $("#favs_button").popover('hide');
           $("#memories_button").popover('show');
      });
      $("#memories_button").popover({
          content: "Для добавления в отслеживаемое надо залогиниться!",
          autoReposition: false,
          trigger: 'manual'
      });
  });
}

function topic_memories_form_setup(memId, watch, msgid, csrf) {
  function memories_add(event) {
    event.preventDefault();

    $.ajax({
      url: "/memories.jsp",
      type: "POST",
      data: { msgid : msgid, add: "add", watch: event.data.watch, csrf: csrf }
    }).done(function(t) {
       form_setup(t['id'], event.data.watch);
       if (event.data.watch) {
         $('#memories_count').text(t['count']);
       } else {
         $('#favs_count').text(t['count']);
       }
    });
  }

  function memories_remove(event) {
    event.preventDefault();

    $.ajax({
      url: "/memories.jsp",
      type: "POST",
      data: { id : event.data.id, remove: "remove", csrf: csrf }
    }).done(function(t) {
      form_setup(0, event.data.watch);
      if (t>=0) {
        if (event.data.watch) {
          $('#memories_count').text(t);
        } else {
          $('#favs_count').text(t);
        }
      }
    });
  }

  function form_setup(memId, watch) {
    var el;

    if (watch) {
      el = $('#memories_button');
    } else {
      el = $('#favs_button');
    }

    if (memId==0) {
      el.removeClass('selected');
      el.attr('title', watch?"Отслеживать":"В избранное");

      el.unbind("click", memories_remove);
      el.bind("click", {watch: watch}, memories_add);
    } else {
      el.addClass('selected');
      el.attr('title', watch?"Не отслеживать":"Удалить из избранного");

      el.unbind("click", memories_add);
      el.bind("click", {watch: watch, id: memId}, memories_remove);
    }
  }

  $().ready(function () {
    form_setup(memId, watch);
  });
}

$(document).ready(function() {
  function initLoginForm() {
    var options = {
      type:"post",
      dataType:"json",
      success:function (response, status) {
        if (response.loggedIn) {
          window.location.reload();
        } else {
          alert("Ошибка авторизации. Неправильное имя пользователя, e-mail или пароль.");
          window.location = "/login.jsp";
        }
      },
      error:function (response, status) {
        alert("Ошибка авторизации. Неправильное имя пользователя, e-mail или пароль.");
        window.location = "/login.jsp";
      }
    };

    $('#regform').ajaxForm(options);

    if (navigator.userAgent.indexOf('Opera Mini') == -1) {
      $('#loginbutton').bind('click', function(e) {
        $("#regmenu").fadeOut("fast", function() {
          $("#regform").fadeIn("fast", function() {
            $("#regform input[name='nick']").focus();
          });
        });
        return false;
      });

      $('#hide_loginbutton').bind('click', function(e) {
        $("#regform").fadeOut("fast", function() {
          $("#regmenu").fadeIn("fast");
        });
        return false;
      });
    }
  }

  function initCtrlEnter() {
    function ctrl_enter(e, form) {
        if (((e.keyCode == 13) || (e.keyCode == 10)) && (e.ctrlKey)) {
          window.onbeforeunload = null;

          $(form).submit();

          return false;
        }
    }

    $('textarea').bind('keypress', function(e) {
      ctrl_enter(e, e.target.form);
    });
  }

  function initCommentFormValidation() {
    $("#commentForm").validate({
      messages : {
        msg :  "Введите сообщение",
        title : "Введите заголовок"
      }
    });
  }

  function initSamepageCommentNavigation() {
    $("article.msg .title a[data-samepage]").click(function(event) {
      event.preventDefault();
      location.hash = "comment-" + this.search.substr(5);
    })
  }

  initLoginForm();
  initCtrlEnter();
  initCommentFormValidation();

  // remove hidden quote elements
  $(".none").remove();

  initSamepageCommentNavigation();
});


