<?php $debug=1; ?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN"
                    "http://www.w3.org/TR/REC-html40/loose.dtd">
<HTML>
<HEAD>
	<TITLE>Добавление фотографии</TITLE>

<?php include("head.html"); ?>

<h1>Добавление фотографии</h1>

<?php

   $conn = connect_db();

######### login ############

if ($HTTP_POST_VARS[add])
{
if (!$_FILES[userfile])
   {
     echo "<b>Ошибка</b>: Вы забыли выбрать файл.";
     $errors=1;
   }

  if ( $HTTP_POST_VARS[nick] )               # если ввели имя
  {
    $result = pg_Exec($conn,"SELECT id FROM users WHERE nick = '" . $HTTP_POST_VARS[nick] . "'");
    if (!$result) echo "Error in select";
    $arr=@pg_fetch_row($result, 0);
    $userid=$arr[0];                      # восстанавливаем по имени iD
    if (!$userid) {
	echo "<b>Ошибка</b>: Пользователь не найден<br>";
	$errors=1;
    }
  } else {
     echo "<b>Ошибка</b>: Вы забыли ввести имя<br>";
     $errors=1;
  }

  if ($errors!=1) { 
    $result = pg_Exec($conn,"SELECT passwd FROM users WHERE id = '" . $userid . "'");
    if (!$result)  echo "Ошибка";
    $arr=pg_fetch_row($result, 0);
    $password1=$arr[0];
  }
if (!$HTTP_POST_VARS[password])
   {
     echo "<b>Ошибка</b>: Вы забыли ввести пароль.";
     $errors=1;
   }
else
     if ($HTTP_POST_VARS[password]!=$password1 || !$password1)
       {
         echo "Ошибка: Доступ запрещен";
         $errors=1;
       }
}
######### end login ########

if (!$HTTP_POST_VARS[add])
{
echo"<p>Вы можете поместить свою фотографию в форум.<br>Для этого: нажмите на кнопку <b>Browse(Обзор)</b> и выберите файл.<br>Потом введите свое имя и пароль.";
echo"<p><b>Ограничения на фотографию:</b>";
echo"<br>Ширина x Высота: от 50x50 до 150x150 пискелей";
echo"<br>Тип: jpeg, gif, png";
echo"<br>Размер не более 20 Kb";
echo"<br>Помещенная картинка должна быть фотографией; пингвины, снимки экранов, кровавые мальчики и прочее не допускается";
echo"<FORM ENCTYPE=\"multipart/form-data\" ACTION=addphoto.php METHOD=POST>";
echo"<INPUT TYPE=hidden name=add value=1>";
echo"<INPUT TYPE=hidden name=MAX_FILE_SIZE value=50000>";
echo"<br>Фотография: <INPUT NAME=userfile TYPE=file>";
echo"<br>Ваше имя: <INPUT NAME=nick TYPE=Text value=\"" . $NickCookie . "\">";
echo"<br>Ваш пароль: <INPUT NAME=password TYPE=Password>";
echo"<br><INPUT TYPE=submit VALUE=\"Отправить/Send\">";
echo"</FORM>";
}

if ( (!$errors) & ($HTTP_POST_VARS[add]) )
   {
#echo"<hr>";
#echo"<br>File: $userfile";
#echo"<br>Name: $userfile_name";   
#echo"<br>Size: $userfile_size";   
#echo"<br>Type: $userfile_type";   
#echo"<hr>";

$userfile_size = $_FILES[userfile][size];
$userfile_type = $_FILES[userfile][type];
$userfile = $_FILES[userfile][tmp_name];

if ($userfile_size>25000) 
              {
                echo"<br><b>Ошибка:</b> Слишком большой размер файла. Он не должен превышать 25 Kb";
                $errors=1;
              }
if ( ($userfile_type!="image/jpeg") && ($userfile_type!="image/gif") && ($userfile_type!="image/pjpeg") && ($userfile_type!="image/png")  && ($userfile_type!="image/x-png") ) 
              {
                echo"<br><b>Ошибка:</b> Неверный тип файла.";
                $errors=1;
              }

  if (!$errors)
   {

      $size = GetImageSize($userfile);
      echo"<p>Размер фотографии: $size[0]x$size[1]";

        if ( ($size[0]<50) || ($size[0]>150) || ($size[1]<50) || ($size[1]>150) )
              {
                echo"<br><b>Ошибка:</b> Недопустимые размеры фотографии.";
                $errors=1;
              }
   }

if (!$errors)
 {
    echo"<h3>Фотография прошла тест.</h3>";

     if ($userfile_type=="image/jpeg") $filename=$userid . ".jpg";
     if ($userfile_type=="image/pjpeg") $filename=$userid . ".jpg";
     if ($userfile_type=="image/gif") $filename=$userid . ".gif";
     if ($userfile_type=="image/png") $filename=$userid . ".png";
     if ($userfile_type=="image/x-png") $filename=$userid . ".png";

	if (!copy($userfile,"photos/" . $filename)) 
	{
    echo("failed to copy $filename...<br>\n");
    }
   else
   {

    $result = pg_Exec($conn,"UPDATE users SET photo='" . $filename . "' WHERE id='$userid'");
    if (!$result) echo "<h2>Ошибка записи :(</h2>";
    else echo"<h3>Ваша фотография записана.<br></h3>";
//    mail("max@ruwr.ru", "New Photo", "\n$nick posted his photo: http://www.linux.org.ru/photos/$filename");
  }
  }	
	
 }



if ($errors) echo"<p>| <a href=addphoto.php>попробовать еще раз</a> |";


?>
</center>
<?php include("footer.html"); ?>
</BODY>
</HTML>
