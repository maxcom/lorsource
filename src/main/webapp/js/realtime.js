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

(function () {
  const COMMENT_PREFIX = 'comment ';
  const COMMENTS_PREFIX = 'comments ';
  const EVENTS_REFRESH = 'events-refresh';
  const RECONNECT_DELAY = 5000;

  // один запрос вместо отдельного запроса на каждого потребителя; результат
  // рассылается страничным скриптам документным событием events-refresh
  function updateEventsCount() {
    $.ajax({
      url: '/notifications-count',
      cache: false
    }).done(function (data) {
      const count = Number(data);

      const countEl = $('#main_events_count');
      const numberEl = $('#main_events_count_number');

      if (countEl.length > 0 || numberEl.length > 0) {
        countEl.text(count === 0 ? '' : '(' + count + ')');

        if (count === 0) {
          numberEl.removeClass('set').text('');
        } else {
          numberEl.addClass('set').text(count);
        }
      }

      $(document).trigger(EVENTS_REFRESH, count);
    });
  }

  // обновление счётчика уведомлений по активности на странице; не чаще раза в секунду
  let eventsCountTimer = null;

  function scheduleEventsCountUpdate() {
    if (eventsCountTimer !== null) {
      return;
    }

    eventsCountTimer = setTimeout(function () {
      eventsCountTimer = null;
      updateEventsCount();
    }, 1000);
  }

  // форма выбора формы слова для русского числительного (правила как в comment-count.tag)
  function pluralComments(count) {
    const mod100 = count % 100;
    const mod10 = count % 10;

    if (mod100 >= 11 && mod100 <= 14) {
      return "комментариев";
    }

    if (mod10 === 1) {
      return "комментарий";
    }

    if (mod10 >= 2 && mod10 <= 4) {
      return "комментария";
    }

    return "комментариев";
  }

  function newCommentsMessage(count) {
    if (count % 10 === 1 && count % 100 !== 11) {
      return `Был добавлен ${count} новый комментарий.`;
    }

    return `Было добавлено ${count} новых ${pluralComments(count)}.`;
  }

  // состояние переживает переподключения websocket-соединения
  let newCommentsCount = 0;
  let firstNewCommentId = null;

  function renderNewComments() {
    const banner = $('#realtime');

    if (!banner.length || firstNewCommentId === null) {
      return;
    }

    banner
      .text(`${newCommentsMessage(newCommentsCount)} `)
      .append($('<a>')
        .attr('href', `${RealtimeContext.link}?cid=${encodeURIComponent(firstNewCommentId)}&skipdeleted=true`)
        .text("Обновить."))
      .show();
  }

  function onLiveComment(cid) {
    if (firstNewCommentId === null || cid < firstNewCommentId) {
      firstNewCommentId = cid;
    }

    newCommentsCount += 1;

    renderNewComments();
  }

  function onMissedComments(firstCid, count) {
    // сервер считает пропущенные от исходного lastCommentId страницы, поэтому его значение авторитетно
    firstNewCommentId = firstCid;
    newCommentsCount = count;

    renderNewComments();
  }

  function isCommentSubmitting() {
    return Boolean(window._commentSubmitting) || $('#commentForm').find('.spinner').length > 0;
  }

  const RealtimeContext = {
    started: false,
    setupTopic: function (topic, link, cid) {
      RealtimeContext.topic = topic;
      RealtimeContext.link = link;
      RealtimeContext.cid = cid;
    },
    start: function (wsUrl) {
      if (RealtimeContext.started) {
        return;
      }

      RealtimeContext.started = true;

      $script.ready('jquery', function () {
        $(function () {
          if (!('WebSocket' in window)) {
            return;
          }

          const ws = new WebSocket(`${wsUrl}ws`);

          ws.onmessage = function (event) {
            if (typeof event.data !== 'string') {
              return;
            }

            const data = event.data;

            if (data === EVENTS_REFRESH) {
              updateEventsCount();
              return;
            }

            if (!data.startsWith(COMMENT_PREFIX) && !data.startsWith(COMMENTS_PREFIX)) {
              return;
            }

            // при отправке комментария страница вот-вот перезагрузится — уведомление не показываем
            if (isCommentSubmitting()) {
              ws.close();
              return;
            }

            if (data.startsWith(COMMENTS_PREFIX)) {
              const parts = data.substring(COMMENTS_PREFIX.length).split(' ');
              const firstCid = Number(parts[0]);
              const count = Number(parts[1]);

              if (Number.isInteger(firstCid) && Number.isInteger(count) && firstCid > 0 && count > 0) {
                onMissedComments(firstCid, count);
              }
            } else {
              const cid = Number(data.substring(COMMENT_PREFIX.length));

              if (Number.isInteger(cid) && cid > 0) {
                onLiveComment(cid);
              }
            }

            scheduleEventsCountUpdate();
          };

          if (RealtimeContext.topic) {
            ws.onopen = function () {
              ws.send(`${RealtimeContext.topic} ${RealtimeContext.cid}`);
            };
          }

          ws.onclose = function () {
            RealtimeContext.started = false;

            setTimeout(function () {
              RealtimeContext.start(wsUrl);
            }, RECONNECT_DELAY);
          };
        });
      });
    }
  };

  window.RealtimeContext = RealtimeContext;
})();
