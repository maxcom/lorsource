package ru.org.linux.spring.dao;

import java.util.List;
import java.util.HashMap;
import java.util.Properties;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;
import java.io.FileNotFoundException;

import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.builder.ToStringBuilder;

import ru.org.linux.site.GalleryItem;
import ru.org.linux.site.Config;
import ru.org.linux.site.Template;
import ru.org.linux.site.config.PropertiesConfig;
import ru.org.linux.util.ImageInfo;
import ru.org.linux.util.BadImageException;
import ru.org.linux.spring.commons.PropertiesFacade;

/**
 * User: sreentenko
 * Date: 01.05.2009
 * Time: 1:12:45
 */
public class GalleryDaoImpl {

  private static final Log log = LogFactory.getLog(GalleryDaoImpl.class);

  private SimpleJdbcTemplate template;
  private PropertiesFacade properties;

  public SimpleJdbcTemplate getTemplate() {
    return template;
  }

  public void setTemplate(SimpleJdbcTemplate template) {
    this.template = template;
  }

  public PropertiesFacade getProperties() {
    return properties;
  }

  public void setProperties(PropertiesFacade properties) {
    this.properties = properties;
  }

  public List<GalleryItem> getGalleryItems(){
    String sql = "SELECT topics.id as msgid, topics.stat1, topics.title, topics.url," +
        " topics.linktext, nick FROM topics, sections, groups, users WHERE groups.id=topics.groupid" +
        " AND groups.section=sections.id AND users.id=topics.userid AND topics.moderate AND sections.id=3 AND NOT deleted " +
        " ORDER BY commitdate DESC LIMIT 3";
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

    String htmlPath = properties.getProperties().getProperty("HTMLPathPrefix", "");

    try {
      item.setInfo(new ImageInfo(htmlPath + item.getIcon()));
      item.setImginfo(new ImageInfo(htmlPath + item.getUrl()));
    } catch (BadImageException e) {
      log.error(e);
    } catch (IOException e) {
      log.error(e);
    }
    log.debug(ToStringBuilder.reflectionToString(item));
    return item;
  }
}
