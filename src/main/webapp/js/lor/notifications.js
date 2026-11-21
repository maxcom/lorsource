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

$(document).ready(function () {
  // форма слова «уведомление» для русского числительного (правила как в show-replies-new.jsp)
  function pluralNotifications(count) {
    const mod10 = count % 10;
    const mod100 = count % 100;

    if (mod10 === 1 && mod100 !== 11) {
      return 'уведомление';
    }

    if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
      return 'уведомления';
    }

    return 'уведомлений';
  }

  function getUnreadCounterText(count) {
    if (count === 0) {
      return 'У вас нет непрочитанных уведомлений';
    }

    if (count % 10 === 1 && count % 100 !== 11) {
      return 'У вас ' + count + ' непрочитанное уведомление';
    }

    return 'У вас ' + count + ' непрочитанных ' + pluralNotifications(count);
  }

  function newNotificationsMessage(count) {
    if (count % 10 === 1 && count % 100 !== 11) {
      return 'Добавлено ' + count + ' новое уведомление';
    }

    return 'Добавлено ' + count + ' новых ' + pluralNotifications(count);
  }

  function initNotificationsOpener() {
    const notificationsRoot = $('.notifications');

    if (notificationsRoot.length === 0) {
      return;
    }

    function getUnreadDelta(button) {
      const unreadDelta = parseInt(button.attr('data-unread-delta'), 10);

      if (!Number.isNaN(unreadDelta)) {
        return unreadDelta;
      }

      const countNode = button.find('.notifications-number');

      if (countNode.length === 0) {
        return 1;
      }

      const match = countNode.text().match(/\d+/);
      return match ? parseInt(match[0], 10) : 1;
    }

    function decrementUnreadCounter(button) {
      const counterBlock = $('#counter_block');

      if (counterBlock.length === 0 || !button.hasClass('event-unread-true')) {
        return;
      }

      const currentCount = parseInt(counterBlock.attr('data-unread-count'), 10);

      if (Number.isNaN(currentCount)) {
        return;
      }

      const nextCount = Math.max(currentCount - getUnreadDelta(button), 0);

      counterBlock.attr('data-unread-count', nextCount);
      $('#counter_text').text(getUnreadCounterText(nextCount));

      if (nextCount === 0) {
        $('#reset_form').remove();

        startNewNotificationsWatch(CLICK_READ_SYNC_DELAY);
      }
    }

    function handleClick(event, openInNewTab) {
      const button = $(this);
      const form = button.closest('form');

      if (openInNewTab) {
        decrementUnreadCounter(button);
      }

      button.removeClass("event-unread-true").addClass("event-unread-false");

      if (openInNewTab) {
        form.attr('target', '_blank');
        return;
      }

      form.removeAttr('target');
      event.preventDefault();

      fetch('/notifications-click/ajax', {
        method: 'POST',
        headers: { 'X-Requested-With': 'XMLHttpRequest', 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json' },
        body: form.serialize()
      })
      .then(function (response) {
        if (!response.ok) throw new Error('Request failed');
        return response.json();
      })
      .then(function (data) {
        window.location.href = data.url;
      })
      .catch(function () {
        window.location.href = '/notifications';
      });
    }

    $('button.notifications-item').on('click', function (event) {
      const openInNewTab = event.ctrlKey || event.metaKey || event.shiftKey;
      handleClick.call(this, event, openInNewTab);
    });

    $('button.notifications-item').on('auxclick', function (event) {
      if (event.button !== 1) {
        return;
      }

      const button = $(this);
      const form = button.closest('form');

      event.preventDefault();
      decrementUnreadCounter(button);
      button.removeClass("event-unread-true").addClass("event-unread-false");
      form.attr('target', '_blank');
      form.trigger('submit');
    });

    window.addEventListener('pageshow', function (event) {
      if (event.persisted) {
        window.location.replace(window.location.href);
      }
    });
  }

  // отслеживание новых уведомлений, когда все показанные прочитаны; включается
  // при загрузке с нулём непрочитанных и когда счётчик доходит до нуля при чтении
  // прямо на странице (ctrl/middle-click)
  let newNotificationsWatchStarted = false;

  // задержка первой сверки при включении после прочтения на странице: POST
  // прочтения уходит в новую вкладку только после обработчика клика и должен
  // успеть зафиксироваться на сервере — иначе собственное прочтение засчитается
  // как «новое уведомление»
  const CLICK_READ_SYNC_DELAY = 3000;

  function startNewNotificationsWatch(initialSyncDelay) {
    if (newNotificationsWatchStarted) {
      return;
    }

    newNotificationsWatchStarted = true;

    const counterText = $('#counter_text');
    let reloadButton = null;

    function render(count) {
      if (count > 0) {
        counterText.text(newNotificationsMessage(count));

        if (reloadButton === null) {
          reloadButton = $('<button type="button" class="btn btn-small btn-default">Обновить</button>')
            .css('margin-left', '1em')
            .on('click', function () {
              window.location.reload();
            })
            .insertAfter(counterText);
        }
      } else {
        counterText.text(getUnreadCounterText(0));

        if (reloadButton !== null) {
          reloadButton.remove();
          reloadButton = null;
        }
      }
    }

    function update() {
      $.ajax({
        url: '/notifications-count',
        cache: false
      }).done(function (data) {
        render(Number(data));
      });
    }

    // realtime.js прикладывает к событию свежее число непрочитанных из своего запроса
    $(document).on('events-refresh', function (event, count) {
      if (typeof count === 'number') {
        render(count);
      } else {
        update();
      }
    });

    // запасной опрос: кадр events-refresh мог быть пропущен при обрыве websocket
    setInterval(update, 30000);

    // сверяемся с сервером: до включения отслеживания события могли быть пропущены
    setTimeout(update, initialSyncDelay);
  }

  // активен только на странице уведомлений нового дизайна
  function initNewNotificationsWatch() {
    const counterBlock = $('#counter_block');

    if (counterBlock.length === 0) {
      return;
    }

    // при отображённых непрочитанных отслеживание включится позже —
    // когда пользователь прочитает их всё прямо на странице
    if (parseInt(counterBlock.attr('data-unread-count'), 10) !== 0) {
      return;
    }

    startNewNotificationsWatch(0);
  }

  initNotificationsOpener();
  initNewNotificationsWatch();
});
