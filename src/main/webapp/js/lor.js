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

function init_interpage_adv(ads) {
    $(function() {
        var ad = ads[Math.floor(Math.random() * ads.length)];

        if (ad.type=='flash') {
            $script('/js/jquery.swfobject.1-1-1.min.js', function() {
                $('#interpage-adv').flash({
                    "swf": ad.src,
                    "width": 728,
                    "height": 90
                });
            });
        }

        if (ad.type=='img') {
            var anchor = $('<a>');
            anchor.attr('href', ad.href);
            anchor.attr('target', '_blank');

            var img = $('<img>');
            img.attr('src', ad.src);
            if ('width' in ad) {
                img.attr('width', ad.width);
            } else {
                img.attr('width', 728);
            }

            if ('height' in ad) {
                img.attr('height', ad.height);
            } else {
                img.attr('height', 90);
            }

            anchor.append(img);
            $('#interpage-adv').append(anchor);
        }
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

function tag_memories_form_setup(tag, csrf_token) {
  $script.ready('plugins', function() {
    $(function() {
      $("#tagFavNoth").click(function(event) {
        event.preventDefault();
        event.stopPropagation();
        $("#tagFavNoth").popover('show');
      });
      $("#tagFavNoth").popover({
        content: "Для добавления в избранное надо залогиниться!"
      });
    });
  });

  $(function() {
    function tag_filter(event) {
      event.preventDefault();

      var data = { tagName: tag};

      var el = $('#tagFavAdd');
      var add = !el.hasClass("selected");

      if (add) {
        data['add'] = 'add';
      } else {
        data['del'] = 'del';
      }

      data['csrf'] = csrf_token;

      $.ajax({
        url: "/user-filter/favorite-tag",
        type: "POST",
        dataType: "json",
        data: data
      }).done(function (t) {
            if (t.error) {
              alert(t.error);
            } else {
              el.attr('title', add ? "Удалить из избранного" : "В избранное");

              $('#favsCount').text(t['count']);

              if (add) {
                el.addClass("selected");
              } else {
                el.removeClass("selected");
              }
            }
          });
    }

    $("#tagFavAdd").bind("click", tag_filter);
  });
}

$(document).ready(function() {
  function initLoginForm() {
    var options = {
      type:"post",
      dataType:"json",
      xhrFields: {
        withCredentials: true
      },
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

    if (location.protocol === 'https:' || jQuery.support.cors) {
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

  function initScollupButton() {
    var backButton = $('<button id="ft-back-button">');

    backButton.text("Вверх");

    backButton.click(function() {
      $("html, body").animate({ scrollTop: 0 });
    });

    $('#ft').prepend(backButton);
  }

  function initUpdateEventsCount() {
    function update_count() {
      $.ajax({
        url: "/notifications-count",
        cache: false
      }).success(function(data) {
        var value = data==0 ? "" : ("("+data+")" );

        $('#main_events_count').text(value);
      });
    }

    $(function() {
      if ($('#main_events_count').length>0) {
        update_count();
      }
    });
  }

  initLoginForm();
  initCtrlEnter();
  initCommentFormValidation();
  initUpdateEventsCount();

  // remove hidden quote elements
  $(".none").remove();

  initSamepageCommentNavigation();
  initScollupButton();
});


