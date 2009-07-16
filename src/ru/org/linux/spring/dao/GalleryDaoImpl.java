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

package ru.org.linux.spring.dao;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ru.org.linux.site.GalleryItem;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;

/**
 * User: sreentenko
 * Date: 01.05.2009
 * Time: 1:12:45
 */
public class GalleryDaoImpl {
  private static final Log log = LogFactory.getLog(GalleryDaoImpl.class);

  private SimpleJdbcTemplate template;
  private Properties properties;

  public SimpleJdbcTemplate getTemplate() {
    return template;
  }

  public void setTemplate(SimpleJdbcTemplate template) {
    this.template = template;
  }

  public Properties getProperties() {
    return properties;
  }

  public void setProperties(Properties properties) {
    this.properties = properties;
  }

  public List<GalleryItem> getGalleryItems() {
    String sql = "SELECT topics.id as msgid, " +
      " topics.stat1, topics.title, topics.url, topics.linktext, nick FROM topics " +
      " JOIN groups ON topics.groupid = groups.id JOIN sections  ON sections.id = groups.section" +
      " JOIN users ON users.id = topics.userid WHERE topics.moderate AND sections.id=3 " +
      " AND NOT deleted ORDER BY commitdate DESC LIMIT 3";
    return getTemplate().query(sql, new ParameterizedRowMapper<GalleryItem>() {
      public GalleryItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        return createGalleryItem(rs);
      }
    }, new HashMap());
  }

  private GalleryItem createGalleryItem(ResultSet rs) throws SQLException {
    GalleryItem item = new GalleryItem();
    item.setMsgid(rs.getInt("msgid"));
    item.setStat(rs.getInt("stat1"));
    item.setTitle(rs.getString("title"));
    item.setUrl(rs.getString("url"));
    item.setIcon(rs.getString("linktext"));
    item.setNick(rs.getString("nick"));
    item.setStat(rs.getInt("stat1"));

    String htmlPath = properties.getProperty("HTMLPathPrefix");
    item.setHtmlPath(htmlPath);
    try {
      item.setInfo(new ImageInfo(htmlPath + item.getIcon()));
      item.setImginfo(new ImageInfo(htmlPath + item.getUrl()));
    } catch (BadImageException e) {
      log.error(e);
    } catch (IOException e) {
      log.error(e);
    }
    return item;
  }
}
