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

setInterval(parseHash, 1000);