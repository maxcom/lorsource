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
  function updateCount(countEl, numberEl) {
    $.ajax({
      url: "/notifications-count",
      cache: false
    }).done(function (data) {
      const count = Number(data);

      countEl.text(count === 0 ? "" : "(" + count + ")");

      if (count === 0) {
        numberEl.removeClass("set").text("");
      } else {
        numberEl.addClass("set").text(count);
      }
    });
  }

  function initUpdateEventsCount() {
    $(function () {
      const countEl = $('#main_events_count');
      const numberEl = $('#main_events_count_number');

      if (countEl.length > 0 || numberEl.length > 0) {
        updateCount(countEl, numberEl);
      }
    });
  }

  $script.ready('jquery', function () {
    initUpdateEventsCount();
  });

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
            initUpdateEventsCount(); // temporary solution

            if (typeof event.data !== 'string') {
              return;
            }

            if (event.data.startsWith("comment ")) {
              const comment = event.data.substring("comment ".length);

              if (!$('#commentForm').find(".spinner").length && !window._commentSubmitting) {
                if ($("#realtime").is(":hidden")) {
                  $("#realtime")
                      .text("Был добавлен новый комментарий. ")
                      .append($("<a>").attr("href", `${RealtimeContext.link}?cid=${encodeURIComponent(comment)}&skipdeleted=true`).text("Обновить."))
                      .show();
                }
              } else {
                // retry in 5 seconds
                ws.close();
              }
            }
          };

          if (RealtimeContext.topic) {
            ws.onopen = function () {
              if (RealtimeContext.cid === 0) {
                ws.send(RealtimeContext.topic);
              } else {
                ws.send(`${RealtimeContext.topic} ${RealtimeContext.cid}`);
              }
            };
          }

          ws.onclose = function () {
            RealtimeContext.started = false;

            setTimeout(function () {
              RealtimeContext.start(wsUrl);
            }, 5000);
          };
        });
      });
    }
  };

  window.RealtimeContext = RealtimeContext;
})();
