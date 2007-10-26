package ru.org.linux.site;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

import com.danga.MemCached.MemCachedClient;

import ru.org.linux.util.UtilException;

public class ViewerCacher {
  private boolean fromCache;
  private long time = -1;

  public static String getViewer(Viewer viewer, Template tmpl, boolean nocache, boolean closeConnection) throws UtilException, SQLException, IOException, UserErrorException {
    return new ViewerCacher().get(viewer, tmpl, nocache, closeConnection);
  }

  public String get(Viewer viewer, Template tmpl, boolean nocache, boolean closeConnection) throws UtilException, SQLException, IOException, UserErrorException {
    MemCachedClient mcc = MemCachedSettings.getClient();

    String cacheId = MemCachedSettings.getId(viewer.getVariantID(tmpl.getProf()));

    String res = null;

    if (!nocache) {
      time = 0;
      long current = new Date().getTime();
      res = (String) mcc.get(cacheId);
      time = new Date().getTime() - current;
      fromCache = true;
    }

    if (res==null) {
      try{
        long current = new Date().getTime();
        Connection db = tmpl.getConnection();
        res = viewer.show(db);
        time = new Date().getTime() - current;
        fromCache = false;
      } finally {
        if (closeConnection) {
          tmpl.getObjectConfig().SQLclose();
        }
      }

      mcc.add(cacheId, res, viewer.getExpire());
    }

    return res;
  }

  public boolean isFromCache() {
    return fromCache;
  }

  public long getTime() {
    return time;
  }
}
