package ru.org.linux.site;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DupeProtector {
  public static final int THRESHHOLD = 30000;

  private static final DupeProtector instance = new DupeProtector();

  private final Map hash = new HashMap();

  public DupeProtector() {
  }

  public synchronized boolean check(String ip) {
    cleanup();

    if (hash.containsKey(ip)) return false;

    hash.put(ip, new Date());

    return true;
  }

  public synchronized void cleanup() {
    Date current = new Date();

    for (Iterator i = hash.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry entry = (Map.Entry) i.next();
      Date date = (Date) entry.getValue();

      if ((current.getTime()-date.getTime())>THRESHHOLD)
        i.remove();
    }
  }

  public static DupeProtector getInstance() {
    return instance;
  }
}