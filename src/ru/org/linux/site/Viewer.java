package ru.org.linux.site;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.UtilException;

public interface Viewer {
  String show(Connection db) throws IOException, SQLException, UtilException;
  String getVariantID(ProfileHashtable prof) throws UtilException;
  java.util.Date getExpire();  
}
