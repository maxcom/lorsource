/*
 * Copyright 1998-2011 Linux.org.ru
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
package ru.org.linux.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.org.linux.dto.GalleryDto;
import ru.org.linux.site.Section;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 * Реализация класса GalleryDao.
 */
@Repository
public class GalleryDao {
  private static final Log log = LogFactory.getLog(GalleryDao.class);

  @Autowired
  private SimpleJdbcTemplate template;

  @Autowired
  private Properties properties;

  /**
   * Возвращает три последних объекта галереи.
   *
   * @return список GalleryDto объектов
   */
  public List<GalleryDto> getGalleryItems() {
    String sql = "SELECT topics.id as msgid, " +
        " topics.stat1, topics.title, topics.url, topics.linktext, nick, urlname FROM topics " +
        " JOIN groups ON topics.groupid = groups.id " +
        " JOIN users ON users.id = topics.userid WHERE topics.moderate AND section=" + Section.SECTION_GALLERY +
        " AND NOT deleted AND commitdate is not null ORDER BY commitdate DESC LIMIT 3";
    return template.query(sql, new RowMapper<GalleryDto>() {
      @Override
      public GalleryDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        GalleryDto item = new GalleryDto();
        item.setMsgid(rs.getInt("msgid"));
        item.setStat(rs.getInt("stat1"));
        item.setTitle(rs.getString("title"));
        item.setUrl(rs.getString("url"));
        item.setIcon(rs.getString("linktext"));
        item.setNick(rs.getString("nick"));
        item.setStat(rs.getInt("stat1"));
        item.setLink(Section.getSectionLink(Section.SECTION_GALLERY) + rs.getString("urlname") + '/' + rs.getInt("msgid"));

        // TODO: а это уже к доменому классу относится:
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
    });
  }
}
