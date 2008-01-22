var highLighted;

function highLight(message)
{
  var toHighLight = document.getElementById(message);
  if (toHighLight && toHighLight != highLighted) {
    if (highLighted)
    {
      highLighted.className = "msg";
    }
    highLighted = toHighLight;
    highLighted.className = "msg highLighted";
  }
}

function parseHash()
{
  var results = location.hash.match(/^#([1-9]\d*)$/);
  if (results) {
    highLight(results[1]);
  }
}
