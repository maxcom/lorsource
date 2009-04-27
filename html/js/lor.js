// ctrlenter.js

function ctrl_enter(e, form)
{
    if (((e.keyCode == 13) || (e.keyCode == 10)) && (e.ctrlKey)) form.submit();
}

// addtag.js
function addTag(tag) {
  var tags = document.getElementById('tags');
  if (tags.value != "") {
    tags.value += ",";
  }
  tags.value += tag;
}

// hightlight.js

var highLighted;

function highLight(toHighLight)
{
    if (highLighted==toHighLight) {
      return;
    }

    if (highLighted) {
      highLighted.className="msg";
    }

    highLighted = toHighLight;
    highLighted.className = "msg highLighted";
}

function highlightMessage(id)
{
  var toHighLight = document.getElementById(id);

  if (toHighLight)
  {
    highLight(toHighLight);
  }
}

function parseHash()
{
  var results = location.hash.match(/^#([1-9]\d*)$/);
  if (results) {
    highlightMessage(results[1]);
  }
}

function jump(link) {
  if (link && link.href) {
    document.location = link.href;
  }
}

// enable comment frame
setInterval(parseHash, 1000);

$(document).ready(function() {
  var options = {
    success: function(xml) {
      if ($(xml).find("error").length) {
        alert($(xml).find("error").text());
        window.location="/login.jsp";
      } else if ($(xml).find("ok").length) {
        window.location.reload();
      }
    },
    dataType: "xml",
    error: function (xhr) {
      alert('Error!  Status = ' + xhr.status);
      window.location="/login.jsp";
    }
  }

  $('#regform').ajaxForm(options);

  $('loginbutton').bind('click', function(e) {
    $("#regmenu").fadeOut("fast", function() { $("#regform").fadeIn("fast") });
    return false;
  });

  $('hide_loginbutton').bind('click', function(e) {
    $("#regform").fadeOut("fast", function() { $("#regmenu").fadeIn("fast") });
    return false;
  });
});