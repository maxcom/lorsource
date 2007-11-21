<?xml version="1.0" encoding="utf-8"?>
<%@ page pageEncoding="koi8-r" contentType="application/rss+xml; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.util.Date,com.danga.MemCached.MemCachedClient,ru.org.linux.site.MemCachedSettings,ru.org.linux.site.MessageTable,ru.org.linux.site.Template" errorPage="/error.jsp" buffer="200kb"%>
<% Template tmpl = new Template(request, config, response); %>

<rss version="2.0">
<channel>
<link>http://www.linux.org.ru/</link>
<language>ru</language>

<%
  int section = 1;
  if (request.getParameter("section") != null) {
    section = tmpl.getParameters().getInt("section");
  }

  int group = 0;
  if (request.getParameter("group") != null) {
    group = tmpl.getParameters().getInt("group");
  }

  MemCachedClient mcc = MemCachedSettings.getClient();
  String cacheId = MemCachedSettings.getId("rss?section=" + section + (group != 0 ? "&group=" + group : ""));

  Connection db = null;
  try {
    String res = (String) mcc.get(cacheId);
    if (res == null) {
      db = tmpl.getConnection();

      res = MessageTable.getSectionRss(db, section, group, tmpl.getConfig().getProperty("HTMLPathPrefix"), tmpl.getMainUrl());

      mcc.add(cacheId, res, new Date(new Date().getTime() + 10 * 60 * 1000));
    }

    out.print(res);
%>
<%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
</channel>
</rss>
