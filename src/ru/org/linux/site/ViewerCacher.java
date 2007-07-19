package ru.org.linux.site;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.danga.MemCached.MemCachedClient;

import ru.org.linux.util.UtilException;

public class ViewerCacher {
  public static String getViewer(Viewer viewer, Template tmpl, boolean nocache, boolean closeConnection) throws UtilException, SQLException, IOException {
    MemCachedClient mcc = MemCachedSettings.getClient();

    String cacheId = MemCachedSettings.getId(tmpl, viewer.getVariantID(tmpl.getProf()));

    String res = null;

    if (!nocache) {
      res = (String) mcc.get(cacheId);
    }

    if (res==null) {
      try{
        Connection db = tmpl.getConnection("viewer-cacher");
        res = viewer.show(db);
      } finally {
        if (closeConnection) {
          tmpl.getObjectConfig().SQLclose();
        }
      }

      mcc.add(cacheId, res, viewer.getExpire());
    }

    return res;
  }
}
