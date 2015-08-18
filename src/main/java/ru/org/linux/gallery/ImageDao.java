/*
 * Copyright 1998-2015 Linux.org.ru
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

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.tag.TagNotFoundException;
import ru.org.linux.topic.Topic;
import ru.org.linux.user.UserDao;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.image.ImageInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ImageDao {
  private static final Logger logger = LoggerFactory.getLogger(ImageDao.class);

  @Autowired
  private SectionService sectionService;

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Autowired
  private SiteConfig siteConfig;

  @Autowired
  private UserDao userDao;

  /**
   * Возвращает последние объекты галереи.
   *
   * @return список GalleryDto объектов
   */
  public List<GalleryItem> getGalleryItems(int countItems) {
    final Section gallery = sectionService.getSection(Section.SECTION_GALLERY);

    String sql = "SELECT t.msgid, t.stat1,t.title, t.userid, t.urlname, images.icon, images.original, images.id AS imageid, t.commitdate " +
            "FROM (SELECT topics.id AS msgid, topics.stat1, topics.title, userid, urlname, topics.commitdate " +
            "FROM topics JOIN groups ON topics.groupid = groups.id WHERE topics.moderate AND section="+Section.SECTION_GALLERY+ " " +
            "AND NOT topics.deleted AND commitdate IS NOT NULL ORDER BY commitdate DESC LIMIT ?) " +
            "as t JOIN images ON t.msgid = images.topic";

/*
    проверить на PostgreSQL 9.2, возможно там этот вариант лучше будет

    String sql = "SELECT topics.id as msgid, " +
      " topics.stat1, topics.title, images.icon, images.original, userid, urlname, images.id as imageid " +
      "FROM topics " +
      " JOIN groups ON topics.groupid = groups.id " +
      " JOIN images ON topics.id = images.topic "+
      " WHERE topics.moderate AND section=" + Section.SECTION_GALLERY +
      " AND NOT topics.deleted AND commitdate is not null ORDER BY commitdate DESC LIMIT ?";
*/
    return jdbcTemplate.query(sql, new GalleryItemRowMapper(gallery), countItems);
  }

  /**
   * Возвращает последние объекты галереи.
   *
   * @return список GalleryDto объектов
   */
  public List<GalleryItem> getGalleryItems(int countItems, int tagId) throws TagNotFoundException {
    final Section gallery = sectionService.getSection(Section.SECTION_GALLERY);

    String sql = "SELECT t.msgid, t.stat1,t.title, t.userid, t.urlname, images.icon, images.original, images.id AS imageid, t.commitdate " +
            "FROM (SELECT topics.id AS msgid, topics.stat1, topics.title, userid, urlname, topics.commitdate " +
            "FROM topics JOIN groups ON topics.groupid = groups.id WHERE topics.moderate AND section="+Section.SECTION_GALLERY+ " " +
            "AND NOT topics.deleted AND commitdate IS NOT NULL AND topics.id IN (SELECT msgid FROM tags WHERE tagid=?) ORDER BY commitdate DESC LIMIT ?) " +
            "as t JOIN images ON t.msgid = images.topic";

    return jdbcTemplate.query(sql, new GalleryItemRowMapper(gallery), tagId, countItems);
  }

  public List<PreparedGalleryItem> prepare(List<GalleryItem> items) {
    String htmlPath = siteConfig.getHTMLPathPrefix();

    ImmutableList.Builder<PreparedGalleryItem> builder = ImmutableList.builder();

    for (GalleryItem item : items) {
      try {
        ImageInfo iconInfo = new ImageInfo(htmlPath + item.getImage().getIcon());
        ImageInfo fullInfo = new ImageInfo(htmlPath + item.getImage().getOriginal());

        builder.add(new PreparedGalleryItem(
                item,
                userDao.getUserCached(item.getUserid()),
                iconInfo, fullInfo));
      } catch (FileNotFoundException e) {
        logger.error("Image not found! id={}: {}", item.getImage().getId(), e.getMessage());
      } catch (BadImageException | IOException e) {
        logger.error("Bad image id={}", item.getImage().getId(), e);
      }
    }

    return builder.build();
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

  public List<Image> imageByFile(String path) {
    return jdbcTemplate.query(
            "SELECT id, topic, original, icon FROM images WHERE original=?",
            new ImageRowMapper(),
            path
    );
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

  private static class GalleryItemRowMapper implements RowMapper<GalleryItem> {
    private final Section gallery;

    private GalleryItemRowMapper(Section gallery) {
      this.gallery = gallery;
    }

    @Override
    public GalleryItem mapRow(ResultSet rs, int rowNum) throws SQLException {
      GalleryItem item = new GalleryItem();
      item.setMsgid(rs.getInt("msgid"));
      item.setStat(rs.getInt("stat1"));
      item.setTitle(rs.getString("title"));
      item.setCommitDate(rs.getTimestamp("commitdate"));

      Image image = new Image(
              rs.getInt("imageid"),
              rs.getInt("msgid"),
              rs.getString("original"),
              rs.getString("icon")
      );

      item.setImage(image);

      item.setUserid(rs.getInt("userid"));
      item.setStat(rs.getInt("stat1"));
      item.setLink(gallery.getSectionLink() + rs.getString("urlname") + '/' + rs.getInt("msgid"));

      return item;
    }
  }
}
