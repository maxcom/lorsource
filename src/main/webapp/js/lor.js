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

// ctrlenter.js

function ctrl_enter(e, form)
{
    if (((e.keyCode == 13) || (e.keyCode == 10)) && (e.ctrlKey)) {
      window.onbeforeunload = null;

      form.submit();

      return false;
    }
}

// addtag.js
function addTag(tag) {
  var tags = document.getElementById('tags');
  if (tags.value != "") {
    tags.value += ",";
  }
  tags.value += tag;
}

// hightlight.js

var highLighted;

function highLight(toHighLight)
{
    if (highLighted==toHighLight) {
      return;
    }

    if (highLighted) {
      highLighted.className="msg";
    }

    highLighted = toHighLight;
    highLighted.className = "msg highLighted";
}

function highlightMessage(id)
{
  var toHighLight = document.getElementById(id);

  if (toHighLight)
  {
    highLight(toHighLight);
  }
}

function parseHash()
{
  var results = location.hash.match(/^#([1-9]\d*)$/);
  if (results) {
    highlightMessage(results[1]);
  }
}

function jump(link) {
  if (link && link.href) {
    document.location = link.href;
  }
}

// enable comment frame
setInterval(parseHash, 1000);

$(document).ready(function() {
  var options = {
    type: "post",
    dataType: "json",
    success: function(response, status) {
      if(response.loggedIn) {
        window.location.reload();
      } else {
        alert("Ошибка авторизации. Неправильное имя пользователя, e-mail или пароль.");
        window.location="/login.jsp";
      }
    },
    error: function(response, status) {
        alert("Ошибка авторизации. Неправильное имя пользователя, e-mail или пароль.");
        window.location="/login.jsp";
    }
  }

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

  $('textarea').bind('keypress', function(e) { ctrl_enter(e, e.target.form); });

  $("#commentForm").validate({
    messages : {
      msg :  "Введите сообщение",
      title : "Введите заголовок"
    }
  });

  // remove hidden quote elements
  $(".none").remove()
});

hljs.initHighlightingOnLoad();


