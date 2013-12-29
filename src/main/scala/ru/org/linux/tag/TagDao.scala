package ru.org.linux.tag

import org.springframework.stereotype.Repository
import org.springframework.beans.factory.annotation.Autowired
import javax.sql.DataSource
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import com.typesafe.scalalogging.slf4j.Logging
import scala.collection.JavaConversions._
import java.sql.ResultSet

import TagDao._

@Repository
class TagDao @Autowired() (ds:DataSource) extends Logging {
  private val jdbcTemplate = new JdbcTemplate(ds)
  private val simpleJdbcInsert =
    new SimpleJdbcInsert(ds).withTableName("tags_values").usingColumns("value").usingGeneratedKeyColumns("id")

  /**
   * Создать новый тег.
   *
   * @param tagName название нового тега
   * @return tag id
   */
  def createTag(tagName: String): Int = {
    assume(TagName.isGoodTag(tagName), "Tag name must be valid")

    val id = simpleJdbcInsert.executeAndReturnKey(Map("value" -> tagName)).intValue
    logger.debug(s"Создан тег: '$tagName' id=$id")
    id
  }

  /**
   * Изменить название существующего тега.
   *
   * @param tagId   идентификационный номер существующего тега
   * @param tagName новое название тега
   */
  def changeTag(tagId: Int, tagName: String):Unit = {
    jdbcTemplate.update("UPDATE tags_values set value=? WHERE id=?", tagName, tagId)
  }

  /**
   * Удалить тег.
   *
   * @param tagId идентификационный номер тега
   */
  def deleteTag(tagId: Int):Unit = {
    jdbcTemplate.update("DELETE FROM tags_values WHERE id=?", tagId)
  }

  def getTopTags: java.util.List[String] = {
    val topTags = jdbcTemplate.queryForSeq[String](
      "SELECT value FROM tags_values WHERE counter>1 " +
        "ORDER BY counter DESC LIMIT " + TOP_TAGS_COUNT
    )

    topTags.sorted
  }

  /**
   * Получение списка первых букв тегов.
   *
   * @return список первых букв тегов.
   */
  private[tag] def getFirstLetters: java.util.List[String] = {
    val query =
      "select distinct firstchar from " +
        "(select lower(substr(value,1,1)) as firstchar from tags_values " +
        "where counter > 0 order by firstchar) firstchars"

    val letters = jdbcTemplate.queryForSeq[String](query)

    letters.sorted
  }

  /**
   * Получение списка тегов по префиксу.
   *
   * @param prefix       префикс имени тега
   * @return список тегов
   */
  private[tag] def getTagsByPrefix(prefix: String, minCount: Int): java.util.List[TagInfo] = {
    jdbcTemplate.queryAndMap(
      "select counter, value, id from tags_values " +
        "where value like ? and counter >= ?  " +
        "order by value",
      escapeLikeWildcards(prefix) + "%", minCount
    ) (tagInfoMapper)
  }

  /**
   * Получение списка тегов по префиксу.
   *
   * @param prefix       префикс имени тега
   * @return список тегов
   */
  private[tag] def getTopTagsByPrefix(prefix: String, minCount: Int, count: Int): java.util.List[String] = {
    val query = "select value from tags_values " +
      "where value like ? and counter >= ? order by counter DESC LIMIT ?"

    val tags = jdbcTemplate.queryForSeq[String](query,
      escapeLikeWildcards(prefix) + "%", minCount, count)

    tags.sorted
  }

  /**
   * Получение идентификационного номера тега по названию.
   *
   * @param tag название тега
   * @param skipZero пропускать неиспользуемые теги
   * @return идентификационный номер
   */
  def getTagId(tag: String, skipZero: Boolean): Option[Integer] = {
    jdbcTemplate.queryForObject[Integer](
      "SELECT id FROM tags_values WHERE value=?" + (if (skipZero) " AND counter>0" else ""),
      tag
    )
  }

  def getTagId(tag: String):Option[Integer] = getTagId(tag, skipZero = false)

  def getTagInfo(tagId: Int): TagInfo = {
    jdbcTemplate.queryForObjectAndMap(
      "SELECT counter, value, id  FROM tags_values WHERE id=?", tagId
    )(tagInfoMapper).get
  }

  def relatedTags(tagid: Int): java.util.List[String] = {
    jdbcTemplate.queryForSeq[String](
      "select value from " +
        "(select st.tagid, count(*) as cnt from tags as mt join tags as st on mt.msgid=st.msgid " +
        "where mt.tagid=? and mt.tagid<>st.tagid group by st.tagid having count(*)>2) as q " +
        "join tags_values on q.tagid=tags_values.id where counter>5 order by cnt::real/counter desc limit 10", tagid)
  }
}

object TagDao {
  private final val TOP_TAGS_COUNT: Int = 50

  private def escapeLikeWildcards(str: String): String = {
    str.replaceAll("[_%]", "\\\\$0")
  }

  private def tagInfoMapper(rs:ResultSet, rowNum:Int) =
    TagInfo(rs.getString("value"), rs.getInt("counter"), rs.getInt("id"))
}
