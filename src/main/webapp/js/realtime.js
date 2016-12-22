/*
 * Copyright 1998-2016 Linux.org.ru
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

function startRealtime(link, cid) {
    $script.ready('jquery', function () {
        $(function () {
            setTimeout(function () {
              var evtSource;

              if (cid==0) {
                evtSource = new EventSource(link+"/realtime");
              } else {
                evtSource = new EventSource(link+"/realtime?cid="+cid);
              }

              evtSource.addEventListener("comment", function (event) {
                $("#realtime")
                    .text("Был добавлен новый комментарий. ")
                    .append($("<a>").attr("href", link+"?cid="+event.data).text("Обновить."))
                    .show();

                evtSource.close();
              });
            }, 3000);
        });
    });
}

function startRealtimeWS(topic, link, cid, wsUrl) {
  $script.ready('jquery', function () {
    $(function () {
      var supportsWebSockets = 'WebSocket' in window || 'MozWebSocket' in window;

      if (supportsWebSockets) {
        var canceled = false;
        var ws = new WebSocket(wsUrl + "ws");

        ws.onmessage = function (event) {
          $("#realtime")
              .text("Был добавлен новый комментарий. ")
              .append($("<a>").attr("href", link + "?cid=" + event.data).text("Обновить."))
              .show();

          canceled = true;
          ws.close()
        };

        ws.onopen = function() {
          if (cid==0) {
            ws.send(topic)
          } else {
            ws.send(topic + ' ' + cid)
          }
        };

        ws.onclose = function(){
          if (!canceled) {
            setTimeout(function () {
              startRealtimeWS(topic, link, cid)
            }, 5000);
          }
        };
      }
    });
  })
}