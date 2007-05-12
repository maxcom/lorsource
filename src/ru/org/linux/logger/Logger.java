package ru.org.linux.logger;

public abstract class Logger {
  public static final int EMERG = 0;
  public static final int ALERT = 1;
  public static final int CRITICAL = 2;
  public static final int ERROR = 3;
  public static final int WARNING = 4;
  public static final int NOTICE = 5;
  public static final int INFO = 6;
  public static final int DEBUG = 7;

  public abstract void log(String app, int level, String message);

  public void debug(String app, String message) {
    log(app, DEBUG, message);
  }

  public void info(String app, String message) {
    log(app, INFO, message);
  }

  public void notice(String app, String message) {
    log(app, NOTICE, message);
  }

  public void error(String app, String message) {
    log(app, ERROR, message);
  }


  public abstract void close();

  protected String getLevelName(int level) {
    switch (level) {
      case EMERG:
        return "emerg  ";
      case ALERT:
        return "alert  ";
      case CRITICAL:
        return "crit   ";
      case ERROR:
        return "err    ";
      case WARNING:
        return "warn   ";
      case NOTICE:
        return "notice ";
      case INFO:
        return "info   ";
      case DEBUG:
        return "debug  ";
      default:
        return "unknown";
    }

  }
}
