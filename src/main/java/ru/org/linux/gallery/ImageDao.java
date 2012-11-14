/*
 * Copyright 1998-2012 Linux.org.ru
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

package ru.org.linux.gallery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.org.linux.section.Section;
import ru.org.linux.spring.Configuration;
import ru.org.linux.topic.Topic;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ImageDao {
  private static final Log log = LogFactory.getLog(ImageDao.class);

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Autowired
  private Configuration configuration;

  /**
   * Возвращает три последних объекта галереи.
   *
   * @return список GalleryDto объектов
   */
  public List<GalleryItem> getGalleryItems(int countItems) {
    String sql = "SELECT topics.id as msgid, " +
      " topics.stat1, topics.title, images.icon, images.original, nick, urlname, images.id as imageid " +
      "FROM topics " +
      " JOIN groups ON topics.groupid = groups.id " +
      " JOIN images ON topics.id = images.topic "+
      " JOIN users ON users.id = topics.userid WHERE topics.moderate AND section=" + Section.SECTION_GALLERY +
      " AND NOT topics.deleted AND commitdate is not null ORDER BY commitdate DESC LIMIT ?";
    return jdbcTemplate.query(sql,
      new RowMapper<GalleryItem>() {
        @Override
        public GalleryItem mapRow(ResultSet rs, int rowNum) throws SQLException {
          GalleryItem item = new GalleryItem();
          item.setMsgid(rs.getInt("msgid"));
          item.setStat(rs.getInt("stat1"));
          item.setTitle(rs.getString("title"));

          Image image = new Image(
                  rs.getInt("imageid"),
                  rs.getInt("msgid"),
                  rs.getString("original"),
                  rs.getString("icon")
          );

          item.setImage(image);

          item.setNick(rs.getString("nick"));
          item.setStat(rs.getInt("stat1"));
          item.setLink(Section.getSectionLink(Section.SECTION_GALLERY) + rs.getString("urlname") + '/' + rs.getInt("msgid"));

          String htmlPath = configuration.getHTMLPathPrefix();

          try {
            item.setInfo(new ImageInfo(htmlPath + image.getIcon()));
            item.setImginfo(new ImageInfo(htmlPath + image.getOriginal()));
          } catch (BadImageException e) {
            log.error(e);
          } catch (IOException e) {
            log.error(e);
          }
          return item;
        }
      },
      countItems
    );
  }

  @Nullable
  public Image imageForTopic(@Nonnull Topic topic) {
    List<Image> found = jdbcTemplate.query(
            "SELECT id, topic, original, icon FROM images WHERE topic=? AND NOT deleted",
            new ImageRowMapper(),
            topic.getId()
    );

    if (found.isEmpty()) {
      return null;
    } else if (found.size() == 1) {
      return found.get(0);
    } else {
      throw new RuntimeException("Too many images for topic="+topic.getId());
    }
  }

  @Nonnull
  public Image getImage(int id) {
    return jdbcTemplate.queryForObject(
            "SELECT id, topic, original, icon FROM images WHERE id=?",
            new ImageRowMapper(),
            id
    );
  }

  public void saveImage(int topicId, String original, String icon) {
    jdbcTemplate.update("INSERT INTO images (topic, original, icon) VALUES (?,?,?)", topicId, original, icon);
  }

  private static class ImageRowMapper implements RowMapper<Image> {
    @Override
    public Image mapRow(ResultSet rs, int i) throws SQLException {
      return new Image(
              rs.getInt("id"),
              rs.getInt("topic"),
              rs.getString("original"),
              rs.getString("icon")
      );
    }
  }

  public void deleteImage(Image image) {
    jdbcTemplate.update("UPDATE images SET deleted='true' WHERE id=?", image.getId());
  }
}
