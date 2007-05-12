<?php

if ($debug!="1") error_reporting(0);

function connect_db() 
{
 global $StyleCookie;

 $conn = pg_connect("host=localhost dbname=linux user=linuxweb password=obsidian0");
 
 if (!$conn)
   {
    echo "<center><b>Произошла ошибка доступа к базе данных</b></center>";
    include("footer.html");
    exit;
   }

# echo "\n<!-- database connect ok -->\n";
 return $conn;
}

function clearstr($string)
{
  return chop(ereg_replace("[\n\r]", " ", $string));
}

function show_box($box)
{
	global $conn, $StyleCookie;

	echo "<div class=columnbox>";
	$box=basename($box);
	include("boxes/$box.php3");
	echo "</div>";
}

if (strstr($HTTP_USER_AGENT, "htdig")) $HTDIG=1;
if (strstr($HTTP_USER_AGENT, "UdmSearch")) $HTDIG=1;


?>
