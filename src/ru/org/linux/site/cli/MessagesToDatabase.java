package ru.org.linux.site.cli;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.logging.Logger;

public final class MessagesToDatabase {
  private static final Logger logger=Logger.getLogger(MessagesToDatabase.class.getName());

  public static final String database = "jdbc:postgresql:linux";
  public static final int BUFFER_SIZE=16*1024;

  public MessagesToDatabase() {
  }
  public static void main(String[] args) throws Exception {

    logger.info("Connecting to DB: "+database);

    Class.forName("org.postgresql.Driver");
    Connection db = DriverManager.getConnection(database, "linuxweb", "obsidian0");
    PreparedStatement st = db.prepareStatement("INSERT INTO msgbase VALUES(?,?)");

    File dir = new File("/var/www/linux.org.ru/lor-storage/linux-storage/msgbase");

    logger.info("Reading directory");
    String[] files = dir.list();

    logger.info("Got "+files.length+" files!");

    char[] buffer = new char[BUFFER_SIZE];
    for (int i=0; i<files.length; i++) {
      if (i%10000==0) {
        logger.info("Current msgid = "+files[i]+" (i="+i+ ')');
      }

      int msgid = Integer.parseInt(files[i]);

      if (msgid>1279505) {
        continue;
      }

      FileReader reader = new FileReader(dir.getPath()+ '/' +files[i]);
      StringBuffer str = new StringBuffer();

      while (true) {
        int size = reader.read(buffer);

        if (size==-1) {
          break;
        }

        str.append(buffer, 0, size);
      }

      st.setInt(1, msgid);
      st.setString(2, str.toString());

      st.execute();

      st.clearParameters();
    }

    logger.info("Closing connection");
    db.close();
  }
}