package ru.org.linux.logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

/**
 * Logs everything to a single logfile
 * with instance wide locking (i.e. do not try to write to single log file
 * from different JVM's or from different instances of SimpleFileLogger
 */
public class SimpleFileLogger extends Logger {
  private OutputStreamWriter logfile;

  public SimpleFileLogger(String aPath) throws IOException {
    logfile = new OutputStreamWriter(new FileOutputStream(aPath, true), "KOI8-R");
    info("logger", "created logger to " + aPath);
  }

  public synchronized void log(String app, int level, String message) {
    Date date = new Date();
    if (logfile == null) {
      return;
    }
    try {
      logfile.write(date + " " + app + " [" + getLevelName(level) + "] " + message + '\n');
      logfile.flush();
    } catch (IOException e) {
    }
  }

  public synchronized void close() {
    if (logfile == null) {
      return;
    }

    info("logger", "closed logger");
    try {
      logfile.close();
    } catch (IOException e) {
    }
    logfile = null;
  }
}
