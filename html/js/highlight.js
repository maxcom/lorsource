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