/*
 * Copyright 1998-2025 Linux.org.ru
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
      const img = $('<img>');
      const anchor = $('<a>');
      const ad = ads[Math.floor(Math.random() * ads.length)];

      if (ad.type === 'img') {
        anchor.attr('href', ad.href);
        anchor.attr('target', '_blank');

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

      if (ad.type === 'rimg') {
        anchor.attr('href', ad.href);
        anchor.attr('target', '_blank');

        const interpage = $('#interpage');

        if (interpage.width() > 1024) {
          img.attr('width', 980);
          img.attr('height', 120);
          img.attr('src', ad.img980);
        } else if (interpage.width() > 750) {
          img.attr('width', 730);
          img.attr('height', 90);
          img.attr('src', ad.img730);
          img.attr('style', "margin-top: 15px");
        } else {
          img.attr('width', 320);
          img.attr('height', 100);
          img.attr('style', "margin-top: 5px");
          img.attr('src', ad.img320);
        }

        anchor.append(img);
        interpage.append(anchor);
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
  if (typeof (history.replaceState) !== 'function') return;

  if (document.location.hash.indexOf('#comment-') === 0) {
    // Yes, we are viewing a comment

    // exit if no such target
    if (document.getElementById(document.location.hash.substring(1)) === null) return;

    // target not yet loaded
    if (document.querySelector('article.msg:target') === null) {
      setTimeout(replace_state, 50);
      return;
    }

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

function initLoginForm() {
  $(function () {
    $script.ready('plugins', function () {
      var options = {
        type: "post",
        dataType: "json",
        xhrFields: {
          withCredentials: true
        },
        success: function (response, status) {
          if (response.loggedIn) {
            window.location.reload();
          } else {
            alert("Ошибка авторизации. Неправильное имя пользователя, e-mail или пароль.");
            window.location = "/login.jsp";
          }
        },
        error: function (response, status) {
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
    });
  });
}


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
    $("article.msg a[data-samepage=true]").click(function(event) {
      event.preventDefault();
      location.hash = "comment-" + this.search.match(/cid=(\d+)/)[1];
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
    $(this).closest('.spoiled').removeClass('spoiled').addClass("unspoiled");
    $this.remove();
    return false;
  }

  function initCodeSpoilers() {
    $('div.code').each(function() {
      if (this.scrollHeight > this.clientHeight) {
        $(this)
          .append($('<div class="spoiler-open"><span class="btn btn-small btn-default spoiler-button">Развернуть</span></div> ').on('click', spoilerShow))
          .addClass('spoiled');
      }
    });
  }

  function initClearWarningForm() {
    $script.ready('plugins', function() {
      $('.clear-warning-form').ajaxForm({
        success: function(responseText, statusText, xhr, form) {
          form.hide();
          form.parent().wrap("<s></s>")
        }
      });
    });
  }

  function initReactionsUI() {
    $script.ready('plugins', function() {
      twemoji.parse(document.body);

      $(".reaction-anonymous").enable();
      $(".reaction-anonymous").click(function (event) {
        event.preventDefault();
        event.stopPropagation();
        $(this).popover('show')
      }).popover({
        content: "Для добавления реакции нужно залогиниться!"
      });
    });

    $('.reaction-show').on('click', function(event) {
      event.preventDefault();

      var reactions = $(this).parents('.msg_body').find('.reactions');

      if (reactions.is(":hidden") || reactions.find('.zero-reactions').is(":hidden")) {
        $('.zero-reactions').hide();
        $('.reactions .reaction-show').html("&raquo;");

        if (reactions.hasClass("zero-reactions")) {
          reactions.show();
        } else {
          reactions.find('.zero-reactions').show();
          $(this).html("&laquo;");
        }
      } else {
        $('.zero-reactions').hide();
        $('.reactions .reaction-show').html("&raquo;");
      }
    })

    $script.ready('plugins', function () {
      $('button.reaction').not(".reaction-anonymous").on('click', function(event) {
        event.preventDefault();

        var value = $(this).attr('value');
        var btn = $(this);
        var form = $(this).parents(".reactions-form")
        var reactions = $(this).parents('.msg_body').find('.reactions form');

        $(reactions).find(".error").remove();

        var options = {
          url: "/reactions/ajax",
          data: { "reaction" : value },
          success: function(response) {
            reactions.parents(".zero-reactions").removeClass("zero-reactions")

            btn.find('.reaction-count').text(response.count);

            if (value.endsWith('-true')) {
              form.find("button.btn-primary").each(function () {
                $(this).attr('value', $(this).attr('value').replace(/-.*/, "-true"));
                $(this).find(".reaction-count").text($(this).find(".reaction-count").text() - 1);
                $(this).removeClass("btn-primary");
              });

              btn.attr('value', value.replace(/-.*/, "-false"));
              btn.addClass("btn-primary");
            } else {
              btn.attr('value', value.replace(/-.*/, "-true"));
              btn.removeClass("btn-primary");
            }
          },
          error: function(jqXHR, textStatus, errorThrown) {
            reactions.append(
              $("<div class=error>")
                  .text("Возможно, что превышен лимит реакций. Попробуйте снова через 10 минут. " + errorThrown)
            );
          }
        };

        $(this).parents('.reactions-form').ajaxSubmit(options);
      });
    })
  }

  initCtrlEnter();

  initSamepageCommentNavigation();
  initScollupButton();
  initClearWarningForm();

  
  replace_state();
  $(window).bind('hashchange', replace_state);

  initCodeSpoilers();
  initReactionsUI();

  // fix images on Pale Moon
  $('.medium-image-container').each(function() {
    if ($(this).width() == 0) {
        $(this).css('width', 'var(--image-width)')
    }
  });
  $('.slider-parent').each(function() {
    if ($(this).height() <= 48) {
        $(this).css('width', 'var(--image-width)')
    }
  });

  $script.ready('plugins', function () {
    if (window.matchMedia("(min-width: 70em)").matches) {
      $(".msg_body .swiffy-slider").addClass("slider-nav-outside-expand").addClass("slider-nav-visible");
    }

    $(".slider-indicators a").attr('href', 'javascript:;');

    window.swiffyslider.init();
  });
});

function fixTimezone(serverTz) {
  const tz = Intl.DateTimeFormat().resolvedOptions().timeZone;

  if (typeof tz !== 'undefined') {
    $script.ready('plugins', function () {
      if (Cookies.get('tz') !== tz) {
        Cookies.set('tz', tz, { expires: 365 })
      }

      if (tz !== serverTz) {
        $(function() {
          $("time[data-format]").each(function() {
            const date = Date.parse($(this).attr("datetime"));

            const format = $(this).attr("data-format");

            const diff = Date.now() - date;
            const today = new Date().setHours(0)
            const yesterday = new Date()
            yesterday.setDate(yesterday.getDate() - 1)
            const min = Math.floor(diff / (1000 * 60))

            if (format === 'default') {
              $(this).text(moment(date).format("DD.MM.yy HH:mm:ss Z"));
            } else if (format === 'date') {
                $(this).text(moment(date).format("DD.MM.yy"));
            } else if (format === 'compact-interval') {
              if (diff < 1000 * 60 * 60) {
                $(this).text(Math.max(1, min) + "\xA0мин");
              } else if (diff < 1000 * 60 * 60 * 4 || date >= today) {
                $(this).text(moment(date).format("HH:mm"));
              } else if (date >= yesterday) {
                $(this).text("вчера")
              } else {
                $(this).text(moment(date).format("DD.MM.yy"));
              }
            } else if (format === 'interval') {
              if (diff < 2 * 1000 * 60) {
                $(this).text("минуту назад");
              } else if (diff < 1000 * 60 * 60) {
                if (min % 10 < 5 && min % 10 > 1 && (min > 20 || min < 10)) {
                  $(this).text(min + "\xA0минуты назад");
                } else if (min % 10 === 1 && min > 20) {
                  $(this).text(min + "\xA0минута назад");
                } else {
                  $(this).text(min + "\xA0минут назад");
                }
              } else if (date >= today) {
                $(this).text("сегодня " + moment(date).format("HH:mm"));
              } else if (date >= yesterday) {
                $(this).text("вчера " + moment(date).format("HH:mm"));
              } else {
                $(this).text(moment(date).format("DD.MM.yy HH:mm"));
              }
            }
          });
        })
      }
    });
  }
}