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

    function getUnreadCounterText(count) {
      const mod10 = count % 10;

      if (count === 1 || (count > 20 && mod10 === 1)) {
        return 'У вас ' + count + ' непрочитанное уведомление';
      }

      if (count === 2 || count === 3 || (count > 20 && (mod10 === 2 || mod10 === 3))) {
        return 'У вас ' + count + ' непрочитанных уведомления';
      }

      return 'У вас ' + count + ' непрочитанных уведомлений';
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

      const nextCount = currentCount - getUnreadDelta(button);

      if (nextCount <= 0) {
        counterBlock.remove();
        return;
      }

      counterBlock.attr('data-unread-count', nextCount);
      $('#counter_text').text(getUnreadCounterText(nextCount));
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

  initNotificationsOpener();
});
