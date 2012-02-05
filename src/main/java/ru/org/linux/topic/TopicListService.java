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
package ru.org.linux.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.group.Group;
import ru.org.linux.section.Section;
import ru.org.linux.spring.commons.CacheProvider;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;
import ru.org.linux.util.URLUtil;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class TopicListService {
  private static final Log logger = LogFactory.getLog(TopicListService.class);

  @Autowired
  private TagDao tagDao;

  @Autowired
  private TopicListDao topicListDao;

  @Autowired
  private CacheProvider cacheProvider;


  /**
   * Получение списка топиков.
   *
   * @param section секция
   * @param group   группа
   * @param tag     тег
   * @param offset  смещение в результатах выборки
   * @param year    год
   * @param month   месяц
   * @return список топиков
   * @throws UserErrorException
   * @throws TagNotFoundException
   */
  public List<Topic> getTopicsFeed(
    Section section,
    Group group,
    String tag,
    Integer offset,
    Integer year,
    Integer month
  )
    throws UserErrorException, TagNotFoundException {
    logger.debug(
      new StringBuilder()
        .append("TopicListService.getTopicsFeed()")
        .append("; section=").append((section != null) ? section.toString() : "(null)")
        .append("; group=").append((group != null) ? group.toString() : "(null)")
        .append("; tag=").append(tag)
        .append("; offset=").append(offset)
        .append("; year=").append(year)
        .append("; month=").append(month)
        .toString()
    );

    TopicListDto topicListDto = new TopicListDto();

    if (section != null) {
      topicListDto.getSections().add(section.getId());
      if (section.isPremoderated()) {
        topicListDto.setCommitMode(TopicListDao.CommitMode.COMMITED_ONLY);
      } else {
        topicListDto.setCommitMode(TopicListDao.CommitMode.POSTMODERATED_ONLY);
      }
    }

    if (group != null) {
      topicListDto.setGroup(group.getId());
    }

    if (tag != null) {
      topicListDto.setTag(tagDao.getTagId(tag));
    }

    if (month != null && year != null) {
      topicListDto.setDateLimitType(TopicListDto.DateLimitType.BETWEEN);
      Calendar calendar = Calendar.getInstance();

      calendar.set(year, month - 1, 1, 0, 0, 0);
      topicListDto.setFromDate(calendar.getTime());

      calendar.add(Calendar.MONTH, 1);
      topicListDto.setToDate(calendar.getTime());
    } else {

      topicListDto.setLimit(20);
      topicListDto.setOffset(offset > 0 ? offset : null);
      if (tag == null && group == null && !section.isPremoderated()) {
        topicListDto.setDateLimitType(TopicListDto.DateLimitType.MONTH_AGO);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, -6);
        topicListDto.setFromDate(calendar.getTime());
      }
    }
    return getCachedFeed(topicListDto);
  }

  /**
   * Получение списка топиков пользователя.
   *
   * @param user   объект пользователя
   * @param offset смещение в результатах выборки
   * @return список топиков пользователя
   */
  public List<Topic> getUserTopicsFeed(User user, Integer offset, boolean isFavorite) {
    logger.debug(
      new StringBuilder()
        .append("TopicListService.getTopicsFeed()")
        .append("; user=").append((user != null) ? user.toString() : "(null)")
        .append("; offset=").append(offset)
        .toString()
    );

    TopicListDto topicListDto = new TopicListDto();
    topicListDto.setLimit(20);
    topicListDto.setOffset(offset);
    topicListDto.setCommitMode(TopicListDao.CommitMode.ALL);
    topicListDto.setUserId(user.getId());
    topicListDto.setUserFavs(isFavorite);

    return getCachedFeed(topicListDto);
  }

  /**
   * Получение списка топиков для RSS-ленты.
   *
   * @param section    секция
   * @param group      группа
   * @param fromDate   от какой даты получить список
   * @param noTalks    без Talks
   * @param tech       только технические
   * @param feedBurner
   * @return список топиков для RSS-ленты
   */
  public List<Topic> getRssTopicsFeed(
    Section section,
    Group group,
    Date fromDate,
    boolean noTalks,
    boolean tech,
    boolean feedBurner
  ) {
    logger.debug(
      new StringBuilder()
        .append("TopicListService.getRssTopicsFeed()")
        .append("; section=").append((section != null) ? section.toString() : "(null)")
        .append("; group=").append((group != null) ? group.toString() : "(null)")
        .append("; fromDate=").append(fromDate)
        .append("; noTalks=").append(noTalks)
        .append("; tech=").append(tech)
        .append("; feedBurner=").append(feedBurner)
        .toString()
    );

    TopicListDto topicListDto = new TopicListDto();

    if (section != null) {
      topicListDto.getSections().add(section.getId());
    }
    if (group != null) {
      topicListDto.setGroup(group.getId());
    }

    topicListDto.setDateLimitType(TopicListDto.DateLimitType.MONTH_AGO);
    topicListDto.setFromDate(fromDate);

    topicListDto.setNotalks(noTalks);
    topicListDto.setTech(tech);
    topicListDto.setLimit(20);

    if (section.isPremoderated()) {
      topicListDto.setCommitMode(TopicListDao.CommitMode.COMMITED_ONLY);
    } else {
      topicListDto.setCommitMode(TopicListDao.CommitMode.POSTMODERATED_ONLY);
    }
    return (feedBurner)
      ? topicListDao.getTopics(topicListDto)
      : getCachedFeed(topicListDto);
  }

  /**
   * @param section
   * @param fromDate
   * @return
   */
  public List<Topic> getAllTopicsFeed(
    Section section,
    Date fromDate
  ) {
    logger.debug(
      new StringBuilder()
        .append("TopicListService.getAllTopicsFeed()")
        .append("; section=").append((section != null) ? section.toString() : "(null)")
        .append("; fromDate=").append(fromDate)
        .toString()
    );

    TopicListDto topicListDto = new TopicListDto();
    topicListDto.setCommitMode(TopicListDao.CommitMode.UNCOMMITED_ONLY);
    if (section != null) {
      topicListDto.getSections().add(section.getId());
    }

    topicListDto.setDateLimitType(TopicListDto.DateLimitType.MONTH_AGO);
    topicListDto.setFromDate(fromDate);

    return getCachedFeed(topicListDto);
  }

  /**
   * @param sectionId
   * @return
   */
  public List<TopicListDto.DeletedTopic> getDeletedTopicsFeed(Integer sectionId) {
    logger.debug(
      new StringBuilder()
        .append("TopicListService.getDeletedTopicsFeed()")
        .append("; sectionId=").append(sectionId)
        .toString()
    );

    return topicListDao.getDeletedTopics(sectionId);
  }

  public List<Topic> getMainPageFeed(boolean isShowGalleryOnMain) {
    logger.debug(
      new StringBuilder()
        .append("TopicListService.getMainPageFeed()")
        .append("; isShowGalleryOnMain=").append(isShowGalleryOnMain)
        .toString()
    );

    TopicListDto topicListDto = new TopicListDto();

    topicListDto.getSections().add(Section.SECTION_NEWS);
    topicListDto.setLimit(20);
    topicListDto.setDateLimitType(TopicListDto.DateLimitType.MONTH_AGO);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -1);
    topicListDto.setFromDate(calendar.getTime());
    topicListDto.setCommitMode(TopicListDao.CommitMode.COMMITED_ONLY);

    if (isShowGalleryOnMain) {
      topicListDto.getSections().add(Section.SECTION_GALLERY);
    }
    return getCachedFeed(topicListDto);
  }


  /**
   * Корректировка смещения в результатах выборки.
   *
   * @param offset оригинальное смещение
   * @return скорректированное смещение
   */
  public int fixOffset(Integer offset) {
    if (offset != null) {
      if (offset < 0) {
        return 0;
      }

      if (offset > 200) {
        return 200;
      }

      return offset;
    } else {
      return 0;
    }
  }

  /**
   * Получение списка топиков из кэша или из СУБД.
   *
   * @param topicListDto объект, содержащий условия выборки
   * @return список топиков
   */
  private List<Topic> getCachedFeed(TopicListDto topicListDto) {
    int cacheAge = getCacheAge(topicListDto);
    if (cacheAge == 0) {
      return topicListDao.getTopics(topicListDto);
    }

    String cacheKey = null;
    try {
      cacheKey = makeCacheKey(topicListDto);
      logger.trace("cacheKey=" + cacheKey);
    } catch (UnsupportedEncodingException e) {
      logger.error("Fail to create cache key", e);
      return topicListDao.getTopics(topicListDto);
    }

    List<Topic> result = (List<Topic>) cacheProvider.getFromCache(cacheKey);
    if (result == null) {
      result = topicListDao.getTopics(topicListDto);
      cacheProvider.storeToCache(cacheKey, result, cacheAge);
    }
    return result;
  }

  /**
   * Создание уникального ключа для кэша согласно условиям выборки.
   *
   * @param topicListDto объект, содержащий условия выборки
   * @return Строка, содержащая уникальный ключ кэша
   * @throws UnsupportedEncodingException
   */
  private String makeCacheKey(TopicListDto topicListDto)
    throws UnsupportedEncodingException {
    URLUtil.QueryString queryString = new URLUtil.QueryString();
    queryString.add("tg", topicListDto.getTag());
    queryString.add("cm", topicListDto.getCommitMode());


    for (int section : topicListDto.getSections()) {
      queryString.add("sec", section);
    }
    queryString.add("grp", topicListDto.getGroup());

    queryString.add("dlmtType", topicListDto.getDateLimitType());
    queryString.add("dlmt1", topicListDto.getFromDate());
    queryString.add("dlmt2", topicListDto.getToDate());
    if (topicListDto.getUserId() != 0) {
      queryString.add("u", topicListDto.getUserId());
    }

    queryString.add("f", topicListDto.isUserFavs());
    queryString.add("lmt", topicListDto.getLimit());
    queryString.add("offst", topicListDto.getOffset());
    queryString.add("notalks", topicListDto.isNotalks());
    queryString.add("tech", topicListDto.isTech());

    return "view-news?" + queryString.toString();
  }

  /**
   * Получение "времени жизни" данных в кэше.
   *
   * @param topicListDto объект, содержащий условия выборки
   * @return количество миллисекунд.
   */
  private int getCacheAge(TopicListDto topicListDto) {
    if (topicListDto.getLimit() == null || topicListDto.getLimit().equals(0)) {
      return 10 * 60 * 1000;
    }

    if (topicListDto.getCommitMode() == TopicListDao.CommitMode.COMMITED_ONLY) {
      return 0;
    }

    return 30 * 1000;
  }

}
