<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="ru.org.linux.site.Template" errorPage="error.jsp"%>
<% Template tmpl = new Template(request, config, response);%>
<%= tmpl.head() %>
<title>Поиск по сайту</title>
<%= tmpl.DocumentHeader() %>

<H1>Поиск по сайту</h1>
<h2>Поисковая система сайта</h2>
<FORM METHOD=GET ACTION="/cgi-bin/search.cgi">
<table order=1 width=100%>
<tr><td>
<BR>
Search for: <INPUT TYPE="text" NAME="q" SIZE=50 VALUE="">
<input TYPE="submit" VALUE="Search!"><BR>

Results per page:
<SELECT NAME="ps">
<option value="10">10
<option value="20" selected>20
<option value="50">50
</SELECT>

Output format:
<SELECT NAME="o">
<option value="0" selected>Long
<option value="1">Short
<option value="2">URL
</SELECT>


Match:

<SELECT NAME="m">
<option value="all" selected>All
<option value="any">Any
<option value="bool">Boolean
<option value="phrase">Full phrase
</SELECT>


Search for:
<SELECT NAME="wm">
<option value="wrd" selected>Whole word
<option value="beg">Beginning
<option value="end">Ending
<option value="sub">Substring
</SELECT>

<!-- Use this to limit URL match -->
<!--
through:
<SELECT NAME="ul">
<option value="" selected>Entire site
<option value="/manual/">Manual
<option value="/products/">Products
<option value="/support/">Support
</SELECT>
-->
in:
<SELECT NAME="wf">
<option value="222210">all sections
<option value="220000">Description
<option value="202000">Keywords
<option value="200200">Title
<option value="200010">Body
</SELECT>


<!-- Uncomment this to limit database subsection by "tag" -->
     <!-- Search through:
<SELECT NAME="t">
<option value="" selected>All sites
<option value="1">Sport
<option value="2">Technology
<option value="3">Shopping
</SELECT>  -->

<!-- Uncomment this to limit database subsection by language -->
<!-- Language:
<SELECT NAME="g">
<option value="" selected>Any
<option value="en">English
<option value="fr">French
<option value="ru">Russian
</SELECT> -->


</td></tr>
</table>
</form>
<h2>Поиск через Google</h2>
<!-- SiteSearch Google -->
<form method="get" action="http://www.google.ru/custom" target="_top">
<table border="0" bgcolor="#000000">
<tr><td nowrap="nowrap" valign="top" align="left" height="32">
<a href="http://www.google.com/">
<img src="http://www.google.com/logos/Logo_25blk.gif" border="0" alt="Google" align="middle"></img></a>
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
<input type="hidden" name="ie" value="KOI8-R"></input>
<input type="hidden" name="oe" value="KOI8-R"></input>
<input type="hidden" name="flav" value="0000"></input>
<input type="hidden" name="sig" value="VNPb2D8JZrqtw9dZ"></input>
<input type="hidden" name="cof" value="GALT:#3399FF;GL:1;DIV:#666666;VLC:FFFFFF;AH:center;BGC:000000;LBGC:FFFF00;ALC:FFFFFF;LC:FFFFFF;T:CCCCCC;GFNT:FFFFFF;GIMP:FFFFFF;LH:65;LW:30;L:http://www.linux.org.ru/black/img/angry-logo.gif;S:http://;LP:1;FORID:1"></input>
<input type="hidden" name="hl" value="ru"></input>
</td></tr></table>
</form>
<!-- SiteSearch Google -->

<%= tmpl.DocumentFooter() %>
