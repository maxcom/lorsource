<%--
  ~ Copyright 1998-2014 Linux.org.ru
  ~    Licensed under the Apache License, Version 2.0 (the "License");
  ~    you may not use this file except in compliance with the License.
  ~    You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~    Unless required by applicable law or agreed to in writing, software
  ~    distributed under the License is distributed on an "AS IS" BASIS,
  ~    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~    See the License for the specific language governing permissions and
  ~    limitations under the License.
  --%>

<%@ page contentType="text/html; charset=utf-8"%>
<!-- SiteSearch Google -->
<form method="get" action="http://www.google.ru/custom" target="_top">
<table border="0" bgcolor="#000000">
<tr><td nowrap="nowrap" valign="top" align="left" height="32">
<a href="http://www.google.com/">
<img src="http://www.google.com/logos/Logo_25blk.gif" alt="Google" align="middle"></img></a>
</td>
<td nowrap="nowrap">
<input type="hidden" name="domains" value="www.linux.org.ru"></input>
<label for="sbi" style="display: none">Введите условия поиска</label>
<input type="text" name="q" size="31" maxlength="255" value="" id="sbi"></input>
<label for="sbb" style="display: none">Отправить форму поиска</label>
<input type="submit" name="sa" value="Поиск" id="sbb"></input>
</td></tr>
<tr>
<td>&nbsp;</td>
<td nowrap="nowrap">
<table>
<tr>
<td>
<input type="radio" name="sitesearch" value="" id="ss0"></input>
<label for="ss0" title="Искать в Интернете"><font size="-1" color="white">Web</font></label></td>
<td>
<input type="radio" name="sitesearch" value="www.linux.org.ru" checked id="ss1"></input>
<label for="ss1" title="Поиск www.linux.org.ru"><font size="-1" color="white">www.linux.org.ru</font></label></td>
</tr>
</table>
<input type="hidden" name="client" value="pub-6069094673001350"></input>
<input type="hidden" name="forid" value="1"></input>
<input type="hidden" name="ie" value="UTF-8"></input>
<input type="hidden" name="oe" value="UTF-8"></input>
<input type="hidden" name="flav" value="0000"></input>
<input type="hidden" name="sig" value="VNPb2D8JZrqtw9dZ"></input>
<input type="hidden" name="cof" value="GALT:#3399FF;GL:1;DIV:#666666;VLC:FFFFFF;AH:center;BGC:000000;LBGC:FFFF00;ALC:FFFFFF;LC:FFFFFF;T:CCCCCC;GFNT:FFFFFF;GIMP:FFFFFF;LH:65;LW:30;L:http://www.linux.org.ru/black/img/angry-logo.gif;S:http://;LP:1;FORID:1"></input>
<input type="hidden" name="hl" value="ru"></input>
</td></tr></table>
</form>
<!-- SiteSearch Google -->