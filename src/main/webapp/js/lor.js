/*
 * Copyright 1998-2022 Linux.org.ru
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

function initNextPrevKeys() {
  $script.ready('plugins', function () {
    $(function () {
      function jump(link) {
        if (link && link.href) {
          document.location = link.href;
        }
      }

      if (typeof  jQuery.hotkeys !== 'undefined') {
        $(document).bind('keydown', {combi: 'Ctrl+left', disableInInput: true}, function () {
          jump(document.getElementById('PrevLink'))
        });
        $(document).bind('keydown', {combi: 'Ctrl+right', disableInInput: true}, function () {
          jump(document.getElementById('NextLink'))
        });
      }
    })
  });
}

function initStarPopovers() {
  $script.ready('plugins', function () {
    $(function () {
      $("#favs_button").click(function (event) {
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

      $("#memories_button").click(function (event) {
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
    })
  });
}

function init_interpage_adv(ads) {
    $(function() {
        var ad = ads[Math.floor(Math.random() * ads.length)];

        if (ad.type==='img') {
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
            $('#interpage').append(anchor);
        }

        if (ad.type==='rimg') {
            var anchor = $('<a>');
            anchor.attr('href', ad.href);
            anchor.attr('target', '_blank');

            var img = $('<img>');

            if (window.matchMedia("(min-width: 768px)").matches) {
                // img.attr('width', 728);
                img.attr('height', 90);
                img.attr('src', ad.img730);
            } else {
                img.attr('width', 320);
                img.attr('height', 100);
                img.attr('src', ad.img320);
            }

            anchor.append(img);
            $('#interpage').append(anchor);
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

  $(function () {
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
      }).popover({
        content: "Для добавления в избранное надо залогиниться!"
      });

      $("#tagIgnNoth").click(function(event) {
        event.preventDefault();
        event.stopPropagation();
        $("#tagIgnNoth").popover('show');
      }).popover({
        content: "Для добавления в список игнорирования надо залогиниться!"
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

  $(function() {
    function tag_ignore(event) {
      event.preventDefault();

      var data = { tagName: tag};

      var el = $('#tagIgnore');
      var add = !el.hasClass("selected");

      if (add) {
        data['add'] = 'add';
      } else {
        data['del'] = 'del';
      }

      data['csrf'] = csrf_token;

      $.ajax({
        url: "/user-filter/ignore-tag",
        type: "POST",
        dataType: "json",
        data: data
      }).done(function (t) {
            if (t.error) {
              alert(t.error);
            } else {
              el.attr('title', add ? "Перестать игнорировать" : "Игнорировать");

              $('#ignoreCount').text(t['count']);

              if (add) {
                el.addClass("selected");
              } else {
                el.removeClass("selected");
              }
            }
          });
    }

    $("#tagIgnore").bind("click", tag_ignore);
  });
 
}

function replace_state() {
    if (typeof(history.replaceState) !== 'function') return;
    
    if (document.location.hash.indexOf('#comment-') == 0) {
        // Yes, we are viewing a comment
        
        // exit if no such target
        if (document.querySelector('article.msg:target') === null) return;
        
        var hash = document.location.hash.split('-');
        if (parseInt(hash[1]) > 0) {
            // OK, comment ID is valid
            var p = document.location.pathname.split('/');
            // make sure that path doesn't contain /pagex or other parts
            var pathname = [p[0], p[1], p[2], p[3]].join('/');
            // now replace state
            history.replaceState(null, document.title, pathname + '?cid=' + hash[1]);
        }
    }
}

$script.ready('plugins', function() {
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

    $('#loginbutton').bind('click', function (e) {
      $("#regmenu").fadeOut("fast", function () {
        $("#regform").fadeIn("fast", function () {
          $("#regform input[name='nick']").focus();
        });
      });
      return false;
    });

    $('#hide_loginbutton').bind('click', function (e) {
      $("#regform").fadeOut("fast", function () {
        $("#regmenu").fadeIn("fast");
      });
      return false;
    });
  }

  $(function() {
    initLoginForm();
  });
});

$(document).ready(function() {
  function initCtrlEnter() {
    function ctrl_enter(e, form) {
        if (((e.keyCode == 13) || (e.keyCode == 10)) && (e.ctrlKey||e.metaKey)) {
          window.onbeforeunload = null;

          $(form).submit();

          return false;
        }
    }

    $('textarea').bind('keypress', function(e) {
      ctrl_enter(e, e.target.form);
    });
  }

  function initSamepageCommentNavigation() {
    $("article.msg a[data-samepage]").click(function(event) {
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

  function spoilerShow() {
    var $this = $(this);
    $this.parent().removeClass('spoiled');
    $this.remove();
    return false;
  }

  function initCodeSpoilers() {
    $('.code').each(function() {
      var $this = $(this);
      if ($this.height() > 512) {
        $this
          .append($('<a href="#" class="spoiler-open">Развернуть</a>').on('click', spoilerShow))
          .addClass('spoiled');
      }
    });
  }

  initCtrlEnter();

  initSamepageCommentNavigation();
  initScollupButton();
  
  replace_state()
  $(window).bind('hashchange', replace_state);

  initCodeSpoilers();
});
