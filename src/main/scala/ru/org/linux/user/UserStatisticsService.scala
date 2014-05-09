package ru.org.linux.user

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import java.sql.Timestamp
import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import ru.org.linux.section.{Section, SectionService}

@Service
class UserStatisticsService @Autowired() (userDao: UserDao, ignoreListDao: IgnoreListDao, sectionService: SectionService) {
  def getStats(user:User, exact:Boolean) : UserStats = {
    val ignoreCount = ignoreListDao.getIgnoreStat(user)
    val (exactCommentCount, commentCount) = userDao.getCommentCount(user, exact)
    val (firstComment, lastComment) = userDao.getFirstAndLastCommentDate(user)
    val (firstTopic, lastTopic) = userDao.getFirstAndLastTopicDate(user)

    val topicsBySection = userDao.getSectionStats(user).map(
      e => new PreparedUsersSectionStatEntry(sectionService.getSection(e.getSection), e.getCount)
    )

    new UserStats(
      ignoreCount,
      commentCount,
      exactCommentCount,
      firstComment,
      lastComment,
      firstTopic,
      lastTopic,
      topicsBySection
    )
  }
}

case class UserStats (
  @BeanProperty ignoreCount: Int,
  @BeanProperty commentCount: Int,
  @BeanProperty exactCommentCount: Boolean,
  @BeanProperty firstComment: Timestamp,
  @BeanProperty lastComment: Timestamp,
  @BeanProperty firstTopic: Timestamp,
  @BeanProperty lastTopic: Timestamp,
  @BeanProperty topicsBySection: java.util.List[PreparedUsersSectionStatEntry]
)

case class PreparedUsersSectionStatEntry (
  @BeanProperty section: Section,
  @BeanProperty count: Int
)