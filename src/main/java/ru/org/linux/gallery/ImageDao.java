/*
 * Copyright 1998-2024 Linux.org.ru
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

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.topic.Topic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ImageDao {
  @Autowired
  private SectionService sectionService;

  private JdbcTemplate jdbcTemplate;
  private SimpleJdbcInsert jdbcInsert;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcInsert = new SimpleJdbcInsert(dataSource)
            .withTableName("images")
            .usingColumns("topic", "extension")
            .usingGeneratedKeyColumns("id");
  }

  /**
   * Возвращает последние объекты галереи.
   *
   * @return список GalleryDto объектов
   */
  public List<GalleryItem> getGalleryItems(int countItems) {
    final Section gallery = sectionService.getSection(Section.SECTION_GALLERY);

    String sql = "SELECT t.msgid, t.stat1,t.title, t.userid, t.urlname, images.extension, images.id AS imageid, t.commitdate " +
            "FROM (SELECT topics.id AS msgid, topics.stat1, topics.title, userid, urlname, topics.commitdate " +
            "FROM topics JOIN groups ON topics.groupid = groups.id WHERE topics.moderate AND section="+Section.SECTION_GALLERY+ " " +
            "AND NOT topics.deleted AND commitdate IS NOT NULL ORDER BY commitdate DESC LIMIT ?) " +
            "as t JOIN images ON t.msgid = images.topic WHERE NOT images.deleted ORDER BY commitdate DESC";

    return jdbcTemplate.query(sql, new GalleryItemRowMapper(gallery), countItems);
  }

  /**
   * Возвращает последние объекты галереи.
   *
   * @return список GalleryDto объектов
   */
  public List<GalleryItem> getGalleryItems(int countItems, int tagId) {
    final Section gallery = sectionService.getSection(Section.SECTION_GALLERY);

    String sql = "SELECT t.msgid, t.stat1,t.title, t.userid, t.urlname, images.extension, images.id AS imageid, t.commitdate " +
            "FROM (SELECT topics.id AS msgid, topics.stat1, topics.title, userid, urlname, topics.commitdate " +
            "FROM topics JOIN groups ON topics.groupid = groups.id WHERE topics.moderate AND section="+Section.SECTION_GALLERY+ " " +
            "AND NOT topics.deleted AND commitdate IS NOT NULL AND topics.id IN (SELECT msgid FROM tags WHERE tagid=?) ORDER BY commitdate DESC LIMIT ?) " +
            "as t JOIN images ON t.msgid = images.topic WHERE NOT images.deleted";

    return jdbcTemplate.query(sql, new GalleryItemRowMapper(gallery), tagId, countItems);
  }

  @Nullable
  public Image imageForTopic(@Nonnull Topic topic) {
    List<Image> found = jdbcTemplate.query(
            "SELECT id, topic, extension, deleted FROM images WHERE topic=? AND NOT deleted",
            new ImageRowMapper(),
            topic.getId()
    );

    if (found.isEmpty()) {
      return null;
    } else if (found.size() == 1) {
      return found.getFirst();
    } else {
      throw new RuntimeException("Too many images for topic="+topic.getId());
    }
  }

  public Image getImage(int id) {
    return jdbcTemplate.queryForObject(
            "SELECT id, topic, extension, deleted FROM images WHERE id=?",
            new ImageRowMapper(),
            id
    );
  }

  public int saveImage(int topicId, String extension) {
    ImmutableMap<String, ?> dataMap = ImmutableMap.of("topic", topicId, "extension", extension);
    
    return jdbcInsert.executeAndReturnKey(dataMap).intValue();
  }

  private static class ImageRowMapper implements RowMapper<Image> {
    @Override
    public Image mapRow(ResultSet rs, int i) throws SQLException {
      int imageid = rs.getInt("id");

      return new Image(
              imageid,
              rs.getInt("topic"),
              "images/"+imageid+"/original."+rs.getString("extension"),
              rs.getBoolean("deleted"));
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

      int imageid = rs.getInt("imageid");

      Image image = new Image(
              imageid,
              rs.getInt("msgid"),
              "images/"+imageid+"/original."+rs.getString("extension"),
              false);

      item.setImage(image);

      item.setUserid(rs.getInt("userid"));
      item.setStat(rs.getInt("stat1"));
      item.setLink(gallery.getSectionLink() + rs.getString("urlname") + '/' + rs.getInt("msgid"));

      return item;
    }
  }
}
