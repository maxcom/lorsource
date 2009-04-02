/*
 * Copyright 1998-2009 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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

  public static String getViewer(Viewer viewer, Template tmpl, boolean nocache) throws UtilException, SQLException, IOException, UserErrorException {
    return new ViewerCacher().get(viewer, tmpl, nocache);
  }

  public String get(Viewer viewer, Template tmpl, boolean nocache) throws UtilException, SQLException, IOException, UserErrorException {
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
      Connection db = null;
      try{
        long current = new Date().getTime();
        db = LorDataSource.getConnection();
        res = viewer.show(db);
        time = new Date().getTime() - current;
        fromCache = false;
      } finally {
        if (db!=null) {
          db.close();
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
