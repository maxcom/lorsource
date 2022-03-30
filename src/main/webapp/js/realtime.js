/*
 * Copyright 1998-2017 Linux.org.ru
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

var RealtimeContext = {
  started: false,
  setupTopic: function(topic, link, cid) {
    this.topic = topic
    this.link = link
    this.cid = cid
  },
  start: function(wsUrl) {
    if (!RealtimeContext.started) {
      RealtimeContext.started = true;

      $script.ready('jquery', function () {
        $(function () {
          var supportsWebSockets = 'WebSocket' in window || 'MozWebSocket' in window;

          if (supportsWebSockets) {
            var canceled = false;
            var ws = new WebSocket(wsUrl + "ws");

            ws.onmessage = function (event) {
              if (!$('#commentForm').find(".spinner").length) {
                $("#realtime")
                    .text("Был добавлен новый комментарий. ")
                    .append($("<a>").attr("href", RealtimeContext.link + "?cid=" + event.data + "&skipdeleted=true").text("Обновить."))
                    .show();

                canceled = true;
                ws.close()
              } else {
                // retry in 5 seconds
                ws.close()
              }
            };

            if (RealtimeContext.topic) {
              ws.onopen = function () {
                if (RealtimeContext.cid == 0) {
                  ws.send(RealtimeContext.topic)
                } else {
                  ws.send(RealtimeContext.topic + ' ' + RealtimeContext.cid)
                }
              };
            }

            ws.onclose = function () {
              if (!canceled) {
                setTimeout(function () {
                  RealtimeContext.start(wsUrl)
                }, 5000);
              }
            };
          }
        });
      })
    }
  }
}