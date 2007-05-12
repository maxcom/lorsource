package ru.org.linux.logger;

import javax.servlet.ServletContext;

/**
 * Logs to servlet logfile
 */
public class ServletLogger extends Logger {
  private final ServletContext context;
  private boolean closed;

  public ServletLogger(ServletContext cont) {
    context = cont;
    closed = false;
  }

  public void log(String app, int level, String message) {
    if (!closed) {
      context.log(app + " [" + getLevelName(level) + "] " + message);
    }
  }

  public void close() {
    closed = true;
  }
}
