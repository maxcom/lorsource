/*
 * Copyright 1998-2026 Linux.org.ru
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
    function jump(link) {
      if (link && link.href) {
        document.location = link.href;
      }
    }

    if (typeof jQuery.hotkeys !== 'undefined') {
      $(document).on('keydown', {combi: 'Ctrl+left', disableInInput: true}, function () {
        jump(document.getElementById('PrevLink'))
      });
      $(document).on('keydown', {combi: 'Ctrl+right', disableInInput: true}, function () {
        jump(document.getElementById('NextLink'))
      });
    }
  });
}

function initStarPopovers() {
  $script.ready('plugins', function () {
    $(function () {
      const favsTippy = tippy(document.getElementById('favs_button'), {
        content: "Для добавления в избранное надо залогиниться!",
        trigger: 'manual'
      });
      const memoriesTippy = tippy(document.getElementById('memories_button'), {
        content: "Для добавления в отслеживаемое надо залогиниться!",
        trigger: 'manual'
      });

      $("#favs_button").on("click", function (event) {
        event.preventDefault();
        event.stopPropagation();
        memoriesTippy.hide();
        favsTippy.show();
      });

      $("#memories_button").on("click", function (event) {
        event.preventDefault();
        event.stopPropagation();
        favsTippy.hide();
        memoriesTippy.show();
      });
    });
  });
}

function init_interpage_adv(ads) {
  $(function () {
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
      method: "POST",
      data: {msgid: msgid, add: "add", watch: event.data.watch, csrf: csrf}
    }).done(function (t) {
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
      method: "POST",
      data: {id: event.data.id, remove: "remove", csrf: csrf}
    }).done(function (t) {
      form_setup(0, event.data.watch);
      if (t >= 0) {
        if (event.data.watch) {
          $('#memories_count').text(t);
        } else {
          $('#favs_count').text(t);
        }
      }
    });
  }

  function form_setup(memId, watch) {
    let el;

    if (watch) {
      el = $('#memories_button');
    } else {
      el = $('#favs_button');
    }

    if (memId == 0) {
      el.removeClass('selected');
      el.attr('title', watch ? "Отслеживать" : "В избранное");

      el.off("click", memories_remove);
      el.on("click", {watch: watch}, memories_add);
    } else {
      el.addClass('selected');
      el.attr('title', watch ? "Не отслеживать" : "Удалить из избранного");

      el.off("click", memories_add);
      el.on("click", {watch: watch, id: memId}, memories_remove);
    }
  }

  $(function () {
    form_setup(memId, watch);
  });
}

function tag_memories_form_setup(tag, csrf_token) {
  $script.ready('plugins', function () {
    $(function () {
      const tagFavTippy = tippy(document.getElementById('tagFavNoth'), {
        content: "Для добавления в избранное надо залогиниться!",
        trigger: 'manual'
      });
      const tagIgnTippy = tippy(document.getElementById('tagIgnNoth'), {
        content: "Для добавления в список игнорирования надо залогиниться!",
        trigger: 'manual'
      });

      $("#tagFavNoth").on("click", function (event) {
        event.preventDefault();
        event.stopPropagation();
        tagFavTippy.show();
      });

      $("#tagIgnNoth").on("click", function (event) {
        event.preventDefault();
        event.stopPropagation();
        tagIgnTippy.show();
      });

      function tag_filter(event) {
        event.preventDefault();

        const data = {tagName: tag};

        const el = $('#tagFavAdd');
        const add = !el.hasClass("selected");

        if (add) {
          data['add'] = 'add';
        } else {
          data['del'] = 'del';
        }

        data['csrf'] = csrf_token;

        $.ajax({
          url: "/user-filter/favorite-tag",
          method: "POST",
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

      $("#tagFavAdd").on("click", tag_filter);

      function tag_ignore(event) {
        event.preventDefault();

        const data = {tagName: tag};

        const el = $('#tagIgnore');
        const add = !el.hasClass("selected");

        if (add) {
          data['add'] = 'add';
        } else {
          data['del'] = 'del';
        }

        data['csrf'] = csrf_token;

        $.ajax({
          url: "/user-filter/ignore-tag",
          method: "POST",
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

      $("#tagIgnore").on("click", tag_ignore);
    });
  });
}

function replace_state() {
  if (typeof (history.replaceState) !== 'function') return;

  if (document.location.hash.indexOf('#comment-') === 0 && !document.location.pathname.startsWith("/view-deleted")) {
    // Yes, we are viewing a comment

    // exit if no such target
    if (document.getElementById(document.location.hash.substring(1)) === null) return;

    // target not yet loaded
    if (document.querySelector('article.msg:target') === null) {
      setTimeout(replace_state, 50);
      return;
    }

    const hash = document.location.hash.split('-');
    if (parseInt(hash[1]) > 0) {
      // OK, comment ID is valid
      const p = document.location.pathname.split('/');
      // make sure that path doesn't contain /pagex or other parts
      const pathname = [p[0], p[1], p[2], p[3]].join('/');
      // now replace state
      history.replaceState(null, document.title, pathname + '?cid=' + hash[1]);
    }
  }
}

function initLoginForm() {
  $(function () {
    $script.ready('plugins', function () {
      const options = {
        method: "post",
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

      $('#regform').on('submit', function (e) {
        e.preventDefault();
        $.ajax($.extend(options, {
          url: $(this).attr('action'),
          data: $(this).serialize()
        }));
      });

      $('#loginbutton').on('click', function (e) {
        $("#regmenu").fadeOut("fast", function () {
          $("#regform").fadeIn("fast", function () {
            $("#regform input[name='nick']").focus();
          });
        });
        return false;
      });

      $('#hide_loginbutton').on('click', function (e) {
        $("#regform").fadeOut("fast", function () {
          $("#regmenu").fadeIn("fast");
        });
        return false;
      });
    });
  });
}


$(document).ready(function () {
  function initCtrlEnter() {
    function ctrl_enter(e, form) {
      if (((e.keyCode == 13) || (e.keyCode == 10)) && (e.ctrlKey || e.metaKey)) {
        window.onbeforeunload = null;

        $(form).trigger('submit');

        return false;
      }
    }

    $('textarea').on('keypress', function (e) {
      ctrl_enter(e, e.target.form);
    });
  }

  function initSamepageCommentNavigation() {
    $("article.msg a[data-samepage=true]").on("click", function (event) {
      event.preventDefault();
      location.hash = "comment-" + this.search.match(/cid=(\d+)/)[1];
    })
  }

  function initScollupButton() {
    const backButton = $('<button id="ft-back-button">');

    backButton.text("Вверх");

    backButton.on("click", function () {
      $("html, body").animate({scrollTop: 0});
    });

    $('#ft').prepend(backButton);
  }

  function spoilerShow() {
    const $this = $(this);
    $(this).closest('.spoiled').removeClass('spoiled').addClass("unspoiled");
    $this.remove();
    return false;
  }

  function initCodeSpoilers() {
    $('div.code').each(function () {
      if (this.scrollHeight > this.clientHeight) {
        $(this)
            .append($('<div class="spoiler-open"><span class="btn btn-small btn-default spoiler-button">Развернуть</span></div> ').on('click', spoilerShow))
            .addClass('spoiled');
      }
    });
  }

  function initClearWarningForm() {
    $('.clear-warning-form').on('submit', function (e) {
      e.preventDefault();
      const form = $(this);
      $.ajax({
        url: form.attr('action'),
        method: 'POST',
        data: form.serialize(),
        success: function () {
          form.hide();
          form.parent().wrap("<s></s>");
        }
      });
    });
  }

  function initReactionsUI() {
    $script.ready('plugins', function () {
      twemoji.parse(document.body);

      $(".reaction-anonymous").prop("disabled", false).each(function () {
        tippy(this, {
          content: "Для добавления реакции нужно залогиниться!",
          trigger: 'manual'
        });
      }).on("click", function (event) {
        event.preventDefault();
        event.stopPropagation();
        this._tippy.show();
      });
    });

    $('.reaction-show').on('click', function (event) {
      event.preventDefault();

      const reactions = $(this).parents('.msg_body').find('.reactions');

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
      $('button.reaction').not(".reaction-anonymous").on('click', function (event) {
        event.preventDefault();

        const value = $(this).attr('value');
        const btn = $(this);
        const form = $(this).parents(".reactions-form")
        const reactions = $(this).parents('.msg_body').find('.reactions form');

        $(reactions).find(".error").remove();

        const options = {
          url: "/reactions/ajax",
          data: {"reaction": value},
          success: function (response) {
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
          error: function (jqXHR, textStatus, errorThrown) {
            reactions.append(
                $("<div class=error>")
                    .text("Возможно, что превышен лимит реакций. Попробуйте снова через 10 минут. " + errorThrown)
            );
          }
        };

        const formData = $(this).parents('.reactions-form').serializeArray();
        formData.push({name: 'reaction', value: value});
        $.ajax($.extend(options, {
          method: 'POST',
          data: $.param(formData)
        }));
      });
    })
  }

  function initNotificationsOpener() {
    $('button.notifications-item').on('click', function (event) {
      if (event.ctrlKey || event.metaKey || event.shiftKey) {
        $(this).parent().attr('target', '_blank');
      } else {
        $(this).parent().removeAttr('target');
      }

      $(this).removeClass("event-unread-true").addClass("event-unread-false");
    });
    $('button.notifications-item').on('auxclick', function (event) {
      $(this).removeClass("event-unread-true").addClass("event-unread-false");
      $(this).parent().attr('target', '_blank');
      $(this).parent().trigger('submit');
    });
  }

  function initThemeSwitcher() {
    const themes = ['dark', 'light', 'auto'];

    $('#theme-indicator').on('click', function() {
      const html = document.documentElement;
      const current = html.getAttribute('data-theme');
      const idx = themes.indexOf(current);
      if (idx === -1) return;

      const next = themes[(idx + 1) % themes.length];

      document.body.style.opacity = '0';
      setTimeout(function() {
        html.setAttribute('data-theme', next);
        localStorage.setItem('lor-theme', next);
        document.body.style.opacity = '1';
      }, 200);
    });
  }

  initCtrlEnter();

  initSamepageCommentNavigation();
  initScollupButton();
  initClearWarningForm();
  initThemeSwitcher();


  replace_state();
  $(window).on('hashchange', replace_state);

  initCodeSpoilers();
  initReactionsUI();

  initNotificationsOpener();

  // fix images on Pale Moon
  $('.medium-image-container').each(function () {
    if ($(this).width() == 0) {
      $(this).css('width', 'var(--image-width)')
    }
  });
  $('.slider-parent').each(function () {
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
        Cookies.set('tz', tz, {expires: 365})
      }

      if (tz !== serverTz) {
        $(function () {
          $("time[data-format]").each(function () {
            const date = Date.parse($(this).attr("datetime"));

            const format = $(this).attr("data-format");

            const diff = Date.now() - date;
            const today = new Date().setHours(0, 0, 0, 0);
            const yesterdayTs = new Date(today);
            yesterdayTs.setDate(yesterdayTs.getDate() - 1);
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
              } else if (date >= yesterdayTs) {
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
              } else if (date >= yesterdayTs) {
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