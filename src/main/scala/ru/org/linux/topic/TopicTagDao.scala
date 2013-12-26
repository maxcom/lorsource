package ru.org.linux.topic

import org.springframework.stereotype.Repository
import org.springframework.scala.jdbc.core.JdbcTemplate
import javax.sql.DataSource
import ru.org.linux.tag.TagInfo
import org.springframework.beans.factory.annotation.Autowired
import com.google.common.collect.ImmutableMap
import scala.collection.JavaConversions._
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

@Repository
class TopicTagDao @Autowired() (ds:DataSource) {
  private val jdbcTemplate = new JdbcTemplate(ds)
  private val namedJdbcTemplate = new NamedParameterJdbcTemplate(ds)

  /**
   * Добавление тега к топику.
   *
   * @param msgId идентификационный номер топика
   * @param tagId идентификационный номер тега
   */
  def addTag(msgId:Int, tagId:Int):Unit = {
    jdbcTemplate.update("INSERT INTO tags VALUES(?,?)", msgId, tagId)

  }

  /**
   * Удаление тега у топика.
   *
   * @param msgId идентификационный номер топика
   * @param tagId идентификационный номер тега
   */
  def deleteTag(msgId:Int, tagId:Int):Unit = {
    jdbcTemplate.update("DELETE FROM tags WHERE msgid=? and tagid=?", msgId, tagId)
  }

  /**
   * Получить список тегов топика .
   *
   * @param msgid идентификационный номер топика
   * @return список тегов топика
   */
  def getTags(msgid:Int):Seq[TagInfo] = {
    jdbcTemplate.queryAndMap(
      "SELECT tags_values.value, tags_values.counter FROM tags, tags_values WHERE tags.msgid=? AND tags_values.id=tags.tagid ORDER BY value",
      msgid
    ) { (rs, _) => TagInfo(rs.getString("value"), rs.getInt("counter")) }
  }

  /**
   * Получение количества тегов, которые будут изменены для топиков (величина прироста использования тега).
   *
   * @param oldTagId идентификационный номер старого тега
   * @param newTagId идентификационный номер нового тега
   * @return величина прироста использования тега
   */
  def getCountReplacedTags(oldTagId:Int, newTagId:Int):Int = {
    jdbcTemplate.queryForSeq[Integer](
      "SELECT count (tagid) FROM tags WHERE tagid=? AND msgid NOT IN (SELECT msgid FROM tags WHERE tagid=?)",
      oldTagId,
      newTagId
    ).head
  }

  /**
   * Замена тега в топиках другим тегом.
   *
   * @param oldTagId идентификационный номер старого тега
   * @param newTagId идентификационный номер нового тега
   */
  def replaceTag(oldTagId:Int, newTagId:Int):Unit = {
    jdbcTemplate.update(
      "UPDATE tags SET tagid=? WHERE tagid=? AND msgid NOT IN (SELECT msgid FROM tags WHERE tagid=?)",
      newTagId,
      oldTagId,
      newTagId
    )
  }

  /**
   * Удаление тега из топиков.
   *
   * @param tagId идентификационный номер тега
   */
  def deleteTag(tagId:Int):Unit = {
    jdbcTemplate.update("DELETE FROM tags WHERE tagid=?", tagId)
  }

  /**
   * пересчёт счётчиков использования.
   */
  def reCalculateAllCounters():Unit = {
    jdbcTemplate.update("update tags_values set counter = (select count(*) from tags join topics on tags.msgid=topics.id where tags.tagid=tags_values.id and not deleted)")
  }

  def getTags(topics:Seq[Topic]):Vector[(Int, TagInfo)] = {
    if (topics.isEmpty) {
      Vector.empty
    } else {
      val topicIds:java.util.List[Int] = topics.map(_.getId)

      namedJdbcTemplate.query(
        "SELECT msgid, tags_values.value, tags_values.counter FROM tags, tags_values WHERE tags.msgid in (:list) AND tags_values.id=tags.tagid ORDER BY value",
        ImmutableMap.of("list", topicIds),
        new RowMapper[(Int, TagInfo)]() {
          def mapRow(resultSet: ResultSet, rowNum: Int): (Int, TagInfo) =
            resultSet.getInt("msgid") -> TagInfo(resultSet.getString("value"), resultSet.getInt("counter"))
        }).toVector
    }
  }
}
