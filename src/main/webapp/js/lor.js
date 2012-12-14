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


