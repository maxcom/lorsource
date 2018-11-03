/*
 * Copyright 1998-2015 Linux.org.ru
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

var LorViewDiffHistory = function() {
  return {
    htmlHash: {},
    currentHash: 44032,
    dmp: new diff_match_patch(),

    pushHash: function (tag) {
       if (typeof(this.htmlHash[tag]) == 'undefined') {
        this.htmlHash[tag] = eval('"\\u'+this.currentHash.toString(16)+'"');
        this.currentHash++;
      }
      return this.htmlHash[tag];
    },

    clearHash: function () {
      this.htmlHash = {};
      this.currentHash = 44032;
    },

    html2plain: function (html) {
      html = html.replace(/(<(\S*?)[^>]*>|<.*?\/>)/g, function(tag){
          return document.lorViewDiffHistory.pushHash(tag);
      });

      return html;
    },

    plain2html: function (plain) {
      for(var tag in this.htmlHash){
        plain = plain.replace(RegExp(this.htmlHash[tag], 'g'), tag);
      }
      return plain;
    },

    makeHtml: function(diffs) {
      var html = [];
      var pattern_amp = /&/g;
      var pattern_lt = /</g;
      var pattern_gt = />/g;
      var pattern_para = /\n/g;
      for (var x = 0; x < diffs.length; x++) {
        var op = diffs[x][0];    // Operation (insert, delete, equal)
        var data = diffs[x][1];  // Text of change.
        var text = data
            .replace(pattern_amp, '&amp;')
            .replace(pattern_lt, '&lt;')
            .replace(pattern_gt, '&gt;');

        switch (op) {
          case DIFF_INSERT:
            html[x] = '<span class="difference-insert">' + text + '</span>';
            break;
          case DIFF_DELETE:
            html[x] = '<span class="difference-delete">' + text + '</span>';
            break;
          case DIFF_EQUAL:
            html[x] = text;
            break;
        }
      }
      return html.join('');
    },


    highlightContentDiff: function (object1, object2) {
      this.clearHash();
      var content1 = this.html2plain ($(object1).html());
      var content2 = this.html2plain ($(object2).html());

      var diffs = this.dmp.diff_main(content1, content2);
      this.dmp.diff_cleanupSemantic(diffs);
      var html = this.makeHtml(diffs);

      return this.plain2html(html);
    }
  };
}
document.lorViewDiffHistory = new LorViewDiffHistory();

function diffHighlightOn(button) {
  document.prev_object = null;
  $("div.messages div.msg div.msg_body").each(
    function(ind, obj){
      var htmlContent = $(obj).html();
      $(obj).attr("oldHtml", htmlContent);
      htmlContent = htmlContent.replace(/\n/,'').trim();
      if (htmlContent == '') {
        return;
      }
      if (document.prev_object == null) {
        document.prev_object = obj;
        return;
      }
      var html = document.lorViewDiffHistory.highlightContentDiff(obj, document.prev_object);
      $(document.prev_object).html(html);
      document.prev_object = obj;
    }
  );
}

function diffHighlightOff(button) {
  document.prev_object = null;
  $("div.messages div.msg div.msg_body").each(
    function(ind, obj){
      $(obj).html($(obj).attr("oldHtml"));
    }
  );
}

function toggleDiffHighlight(obj) {
  var button = obj.currentTarget;
  var attr = $(button).attr("highlighted");
  if (attr == "on") {
    $(button)
      .val("Подсветить различия")
      .attr("highlighted", "off");
    diffHighlightOff(button);
  } else {
    $(button)
      .val("Убрать подсветку")
      .attr("highlighted", "on");
    diffHighlightOn(button);
  }
}

$(document).ready(function() {
  var button = $('<input type="button">')
    .val("Подсветить различия")
    .attr("highlighted", "off")
    .addClass("toggleDiffHighlight")
    .appendTo($("#historyButtonBar"))
    .click(function(obj){
      toggleDiffHighlight(obj)
    });
});
