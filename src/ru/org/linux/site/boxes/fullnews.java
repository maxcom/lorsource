package ru.org.linux.site.boxes;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.site.NewsViewer;
import ru.org.linux.site.config.PropertiesConfig;
import ru.org.linux.site.config.SQLConfig;
import ru.org.linux.storage.StorageException;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.UtilException;

public final class fullnews extends Boxlet {
  public String getContentImpl(ProfileHashtable profile) throws IOException, SQLException, StorageException, UtilException {
    Connection db = null;
    try {
      db = ((SQLConfig) config).getConnection("fullnews");
      StringBuffer buf = new StringBuffer();
      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("SELECT topics.stat1, topics.lastmod, topics.title as subj, postdate, nick, image, groups.title as gtitle, topics.id as msgid, sections.comment, groups.id as guid, topics.url, topics.linktext, sections.imagepost, linkup, postdate<(CURRENT_TIMESTAMP-expire) as expired, message FROM topics,groups,users,sections,msgbase WHERE sections.id=groups.section AND topics.id=msgbase.id AND topics.moderate AND topics.userid=users.id AND topics.groupid=groups.id AND section=1 AND NOT deleted AND commitdate>(CURRENT_TIMESTAMP-'1 month'::interval) ORDER BY commitdate DESC LIMIT 20");
      NewsViewer nw = new NewsViewer(((PropertiesConfig) config).getProperties(), profile, rs, false, false);
      buf.append(nw.showAll());
      rs.close();
      return buf.toString();
    } finally {
      if (db != null) {
        ((SQLConfig) config).SQLclose();
      }
    }
  }

  public String getInfo() {
    return "Опрос";
  }

  public String getVariantID(ProfileHashtable prof, Properties request) throws UtilException {
    return "SearchMode=" + prof.getBooleanProperty("SearchMode") + "&topics=" + prof.getIntProperty("topics") + "&style=" + prof.getStringProperty("style");
  }

  public long getVersionID(ProfileHashtable profile, Properties request) {
    long time = new Date().getTime();

    return time - time % (60 * 1000); // 1 min
  }

}
