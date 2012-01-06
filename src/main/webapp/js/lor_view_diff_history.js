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
      html = html.replace(/<(S*?)[^>]*>.*?|<.*?\/>/g, function(tag){
          return document.lorViewDiffHistory.pushHash(tag.toUpperCase());
      });

      return html;
    },

    plain2html: function (plain) {
      for(var tag in this.htmlHash){
        plain = plain.replace(RegExp(this.htmlHash[tag], 'g'), tag);
      }
      return plain;
    },

    highlightContentDiff: function (object1, object2) {
      var content1 = this.html2plain ($(object1).html());
      var content2 = this.html2plain ($(object2).html());

      var diffs = this.dmp.diff_main(content1, content2);
      var html = this.dmp.diff_prettyHtml(diffs);

      return this.plain2html(html);
    }
  };
}

function diffHighlightOn(button) {
  document.prev_object = null;
  $("div.messages div.msg div.msg_body").each(
    function(ind, obj){
      $(obj).attr("oldHtml", $(obj).html());
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
  document.lorViewDiffHistory = new LorViewDiffHistory();
  var button = $('<input type="button">')
    .val("Подсветить различия")
    .attr("highlighted", "off")
    .addClass("toggleDiffHighlight")
    .appendTo($("#historyButtonBar"))
    .click(function(obj){
      toggleDiffHighlight(obj)
    });
});
