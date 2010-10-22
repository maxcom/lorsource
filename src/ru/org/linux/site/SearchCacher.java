/*
 * Copyright 1998-2010 Linux.org.ru
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import ru.org.linux.spring.commons.CacheProvider;

public class SearchCacher {
  private boolean fromCache;
  private long time = -1;

  public List<SearchItem> get(SearchViewer viewer, boolean nocache) throws SQLException, UserErrorException {
    CacheProvider mcc = MemCachedSettings.getCache();

    String cacheId = viewer.getVariantID();

    List<SearchItem> res = null;

    if (!nocache) {
      time = 0;
      long current = System.currentTimeMillis();
      res = (List<SearchItem>) mcc.getFromCache(cacheId);
      time = System.currentTimeMillis() - current;
      fromCache = true;
    }

    if (res==null) {
      Connection db = null;
      try{
        long current = System.currentTimeMillis();
        db = LorDataSource.getConnection();
        res = viewer.show(db);
        time = System.currentTimeMillis() - current;
        fromCache = false;
      } finally {
        if (db!=null) {
          db.close();
        }
      }

      mcc.storeToCache(cacheId, res, 15 * 60 * 1000);
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
