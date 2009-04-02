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

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DupeProtector {
  private static final int THRESHHOLD = 30000;
  private static final int THRESHHOLD_TRUSTED = 3000;
  
  private static final DupeProtector instance = new DupeProtector();

  private final Map<String,Date> hash = new HashMap<String,Date>();

  private DupeProtector() {
  }

  public synchronized boolean check(String ip,boolean trusted) {
    cleanup(trusted);

    if (hash.containsKey(ip)) {
      return false;
    }

    hash.put(ip, new Date());

    return true;
  }

  public void checkDuplication(String ip) throws DuplicationException {
    if (!check(ip,false)) {
      throw new DuplicationException();
    }
  }

  public void checkDuplication(String ip,boolean trusted) throws DuplicationException {
    if (!check(ip,trusted)) {
      throw new DuplicationException();
    }
  }

  private synchronized void cleanup(boolean trusted) {
    Date current = new Date();

    for (Iterator<Map.Entry<String,Date>> i = hash.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry<String,Date> entry = i.next();
      Date date = entry.getValue();

      if (trusted && (current.getTime()-date.getTime())>THRESHHOLD_TRUSTED) {
        i.remove();
      }
      else if ((current.getTime()-date.getTime())>THRESHHOLD) {
        i.remove();
      }
    }
  }

  public static DupeProtector getInstance() {
    return instance;
  }
}