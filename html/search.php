<?

/*   mnoGoSearch-php v.3.1.2.5
 *   for mnoGoSearch ( formely known as UdmSearch ) free web search engine
 *   (C) 1999,2000 by Sergey Kartashoff <gluke@biosys.net>
 *   (C) 1998-1999 by Vladas Lapinskas <lapinskas@mail.iae.lt>
 *                    Kir Kolyshkin <kir@sever.net>
 *                    UdmSearch Developers Team <devel@search.udm.net>
 *   oracle7 support by Roman V. Petrov (poma@mail.ru)
 *
 *   features: ispell support (db & text mode), 
 *             mysql , oracle (both v7 and v8) and postgresql SQL-backends
 *             both in native and odbc modes support
 *             crc-multi, crc, multi-dict and single-dict storage modes support
 */

require('udmsearch/config.inc');
require('udmsearch/init.inc');
require('udmsearch/common.inc');

if (! function_exists('crc32')) require('udmsearch/crc32.inc');
require('udmsearch/template.inc');
require('udmsearch/db_func.inc');
require('udmsearch/ispell.inc');
require('udmsearch/parse.inc');

if ($lang_content_negotiation == 'yes') {
// path to template file ($lang_content_negotiation = 'yes')
// please refer to docs on this feature before using it.
	$template_file= ereg_replace(".php.*", ".htm", basename($SCRIPT_FILENAME));
	$template_file = "./" . $template_file;
} else {
// path to template file ($lang_content_negotiation = 'no')
	$template_file='udmsearch/search.htm';
}

// -----------------------------------------------
//  M A I N 
// -----------------------------------------------


init();


if (! $have_query_flag) {
    print_template('bottom');
    drop_temp_table(0);
    return;	
} elseif ($have_query_flag && ($q=='')) {
    print_template('noquery'); 	
    print_template('bottom');
    drop_temp_table(0);
    return;
}         

if (eregi('db',$ispellmode) || eregi('text',$ispellmode)) load_affix();

list($query_url_id,$query_url,$query_count_url_id)=last_parse($q);

if (count($all_words) == 0) {
    print_template('notfound');
    print_template('bottom');
    drop_temp_table(0);
    track_query($query_orig,time(),0);
    return;
} 

if (($dbtype == 'oracle')||
    ($dbtype == 'oracle7') ||
    ($dbtype == 'oracle8'))
{
   if($DEBUG) echo "main(): ",$query_count_url_id,"<BR><HR>";

   if (!($res2=db_query($query_count_url_id)))  print_error_local('Query error: '.$query_count_url_id."\n<BR>".db_error());

   $row=db_fetchrow($res2);
   $found=$row[0];

   db_freeresult($res2);
}

if($DEBUG) echo "main(): ",$query_url_id,"<BR><HR>";
if (!($res=db_query($query_url_id))) print_error_local('Query error: '.$query_url_id."\n<BR>".db_error());

$from=IntVal($np)*IntVal($ps); 
$to=IntVal($np+1)*IntVal($ps); 

if (($dbtype == 'mysql') ||
    ($dbtype == 'pgsql') ||
    ($dbtype == '')) {
   $found=db_numrows($res);
}

if($to>$found) $to=$found;
if (($from+$ps)<$found) $isnext=1;
$nav=make_nav($query_orig);

if ($found>0) {
    print_template('restop');
} else {
    print_template('notfound');
    print_template('bottom');
    drop_temp_table(0);
    track_query($query_orig,time(),0);
    return;
}

$data_seek=db_dataseek($res,$from);
if (! $data_seek) {
    print_template('notfound');
    print_template('bottom');
    drop_temp_table(0);
    track_query($query_orig,time(),0);
    return;
}

$url_id_array=array();
$rating_array=array();

for ($i=0; $i<$ps; $i++) {
  $row=db_fetchrow($res);

  if (!$row) break;

  $url_id_array[] = $row[0];
  $rating_array[] = $row[1];
}

db_freeresult($res);

drop_temp_table(1);

$ndoc=$from+1;
for ($i=0; $i<count($url_id_array); $i++) {

  $query=ereg_replace("%URL_IN%",$url_id_array[$i],$query_url);
  $rating=$rating_array[$i];

  if($DEBUG) echo "main(): ",$query,"<BR><HR>";
  if (!($res=db_query($query))) print_error_local('Query error: '.$query."\n<BR>".db_error());
  
  $data=db_fetchrow($res);
  db_freeresult($res);

  $url=$data[0];
  $title=$data[1];
  $title=($title) ? htmlspecialChars($title):'No title';
  $text=ParseDocText(htmlspecialChars($data[2]));
  $contype=$data[3];
  $docsize=$data[4];
  $lastmod=format_lastmod($data[5]);
  $keyw=htmlspecialChars($data[6]);
  $desc=htmlspecialChars($data[7]);
  $crc=$data[8];
  $rec_id=0+$data[9];

  if ($db_format == '3.1') {
  	$category=$data[10];
  }

  if ((ereg("^ftp://", $url)) && 
      ($templates['ftpres'][0] != '')) {
	print_template('ftpres');
  } elseif ((ereg("^https?://", $url)) && 
            ($templates['httpres'][0] != '')) {
	print_template('httpres');
  } else {
	print_template('res');
  }
  
  $ndoc++;
}

print_template('resbot');    
print_template('bottom');

track_query($query_orig,time(),$found);

if ($dbtype == 'oracle7') {
    Ora_Close($dbconn); // closing oracle7 cursor
}

?>
