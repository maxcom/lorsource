package ru.org.linux.tag

import org.springframework.validation.Errors
import scala.collection.JavaConversions._
import java.util.regex.Pattern
import ru.org.linux.user.UserErrorException

object TagName {
  val MAX_TAGS_PER_TOPIC = 5
  val MIN_TAG_LENGTH = 2
  val MAX_TAG_LENGTH = 25

  private[this] val tagRE = Pattern.compile("([\\p{L}\\d][\\p{L}\\d \\+-.]+)", Pattern.CASE_INSENSITIVE)

  def isGoodTag(tag:String):Boolean = {
    tagRE.matcher(tag).matches() && tag.length() >= MIN_TAG_LENGTH && tag.length() <= MAX_TAG_LENGTH
  }

  @throws[UserErrorException]
  def checkTag(tag:String):Unit = {
    // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
    if (!isGoodTag(tag)) {
      throw new UserErrorException("Некорректный тег: '" + tag + '\'')
    }
  }

  private[this] def parseTags(tags: String) = {
    if (tags == null) {
      Set.empty[String]
    } else {
      // Теги разделяютчя пайпом или запятой
      val tagsArr = tags.replaceAll("\\|", ",").split(",")

      import scala.collection.breakOut

      val tagSet: Set[String] = tagsArr.filterNot(_.isEmpty).map(_.toLowerCase.trim)(breakOut)

      tagSet
    }
  }

  /**
   * Разбор строки тегов. Error при ошибках
   *
   * @param tags   список тегов через запятую
   * @param errors класс для ошибок валидации (параметр 'tags')
   * @return список тегов
   */
  def parseAndValidateTags(tags:String, errors:Errors):Seq[String] = {
    val (goodTags, badTags) = parseTags(tags).partition(isGoodTag)

    for (tag <- badTags) {
      // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
      if (tag.length() > MAX_TAG_LENGTH) {
        errors.rejectValue("tags", null, "Слишком длиный тег: '" + tag + "\' (максимум " + MAX_TAG_LENGTH + " символов)")
      } else if (!isGoodTag(tag)) {
        errors.rejectValue("tags", null, "Некорректный тег: '" + tag + '\'')
      }
    }

    if (goodTags.size > MAX_TAGS_PER_TOPIC) {
      errors.rejectValue("tags", null, "Слишком много тегов (максимум " + MAX_TAGS_PER_TOPIC + ')')
    }

    goodTags.toVector
  }

  /**
   * Разбор строки тегов. Игнорируем некорректные теги
   *
   * @param tags список тегов через запятую
   * @return список тегов
   */
  def parseAndSanitizeTags(tags:String):java.util.List[String] =
    parseTags(tags).filter(isGoodTag).toVector
}
