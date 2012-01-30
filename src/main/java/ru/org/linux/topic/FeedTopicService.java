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
public class FeedTopicService {
  private static final Log logger = LogFactory.getLog(FeedTopicService.class);

  @Autowired
  private TagDao tagDao;

  @Autowired
  private FeedTopicDao feedTopicDao;

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
        .append("FeedTopicService.getTopicsFeed()")
        .append("; section=").append((section != null) ? section.toString() : "(null)")
        .append("; group=").append((group != null) ? group.toString() : "(null)")
        .append("; tag=").append(tag)
        .append("; offset=").append(offset)
        .append("; year=").append(year)
        .append("; month=").append(month)
        .toString()
    );

    FeedTopicDto feedTopicDto = new FeedTopicDto();

    if (section != null) {
      feedTopicDto.getSections().add(section.getId());
      if (section.isPremoderated()) {
        feedTopicDto.setCommitMode(FeedTopicDao.CommitMode.COMMITED_ONLY);
      } else {
        feedTopicDto.setCommitMode(FeedTopicDao.CommitMode.POSTMODERATED_ONLY);
      }
    }

    if (group != null) {
      feedTopicDto.setGroup(group.getId());
    }

    if (tag != null) {
      feedTopicDto.setTag(tagDao.getTagId(tag));
    }

    if (month != null && year != null) {
      feedTopicDto.setDateLimitType(FeedTopicDto.DateLimitType.BETWEEN);
      Calendar calendar = Calendar.getInstance();

      calendar.set(year, month-1, 1);
      feedTopicDto.setFromDate(calendar.getTime());

      calendar.add(Calendar.MONTH, 1);
      feedTopicDto.setToDate(calendar.getTime());
    } else {

      feedTopicDto.setLimit(20);
      feedTopicDto.setOffset(offset > 0 ? offset : null);
      if (tag == null && group == null && !section.isPremoderated()) {
        feedTopicDto.setDateLimitType(FeedTopicDto.DateLimitType.MONTH_AGO);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, -6);
        feedTopicDto.setFromDate(calendar.getTime());
      }
    }
    return getCachedFeed(feedTopicDto);
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
        .append("FeedTopicService.getTopicsFeed()")
        .append("; user=").append((user != null) ?user.toString(): "(null)")
        .append("; offset=").append(offset)
        .toString()
    );

    FeedTopicDto feedTopicDto = new FeedTopicDto();
    feedTopicDto.setLimit(20);
    feedTopicDto.setOffset(offset);
    feedTopicDto.setCommitMode(FeedTopicDao.CommitMode.ALL);
    feedTopicDto.setUserId(user.getId());
    feedTopicDto.setUserFavs(isFavorite);

    return getCachedFeed(feedTopicDto);
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
        .append("FeedTopicService.getRssTopicsFeed()")
        .append("; section=").append((section != null) ? section.toString() : "(null)")
        .append("; group=").append((group != null) ? group.toString() : "(null)")
        .append("; fromDate=").append(fromDate)
        .append("; noTalks=").append(noTalks)
        .append("; tech=").append(tech)
        .append("; feedBurner=").append(feedBurner)
        .toString()
    );

    FeedTopicDto feedTopicDto = new FeedTopicDto();

    if (section != null) {
      feedTopicDto.getSections().add(section.getId());
    }
    if (group != null) {
      feedTopicDto.setGroup(group.getId());
    }

    feedTopicDto.setDateLimitType(FeedTopicDto.DateLimitType.MONTH_AGO);
    feedTopicDto.setFromDate(fromDate);

    feedTopicDto.setNotalks(noTalks);
    feedTopicDto.setTech(tech);
    feedTopicDto.setLimit(20);

    if (section.isPremoderated()) {
      feedTopicDto.setCommitMode(FeedTopicDao.CommitMode.COMMITED_ONLY);
    } else {
      feedTopicDto.setCommitMode(FeedTopicDao.CommitMode.POSTMODERATED_ONLY);
    }
    return (feedBurner)
      ? feedTopicDao.getTopics(feedTopicDto)
      : getCachedFeed(feedTopicDto);
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
        .append("FeedTopicService.getAllTopicsFeed()")
        .append("; section=").append((section != null) ? section.toString() : "(null)")
        .append("; fromDate=").append(fromDate)
        .toString()
    );

    FeedTopicDto feedTopicDto = new FeedTopicDto();
    feedTopicDto.setCommitMode(FeedTopicDao.CommitMode.UNCOMMITED_ONLY);
    if (section != null) {
      feedTopicDto.getSections().add(section.getId());
    }

    feedTopicDto.setDateLimitType(FeedTopicDto.DateLimitType.MONTH_AGO);
    feedTopicDto.setFromDate(fromDate);

    return getCachedFeed(feedTopicDto);
  }

  /**
   * @param sectionId
   * @return
   */
  public List<FeedTopicDto.DeletedTopic> getDeletedTopicsFeed(Integer sectionId) {
    logger.debug(
      new StringBuilder()
        .append("FeedTopicService.getDeletedTopicsFeed()")
        .append("; sectionId=").append(sectionId)
        .toString()
    );

    if (sectionId == null) {
      return feedTopicDao.getDeletedTopics();
    } else {
      return feedTopicDao.getDeletedTopics(sectionId);
    }
  }

  public List<Topic> getMainPageFeed(boolean isShowGalleryOnMain) {
    logger.debug(
      new StringBuilder()
        .append("FeedTopicService.getMainPageFeed()")
        .append("; isShowGalleryOnMain=").append(isShowGalleryOnMain)
        .toString()
    );

    FeedTopicDto feedTopicDto = new FeedTopicDto();

    feedTopicDto.getSections().add(Section.SECTION_NEWS);
    feedTopicDto.setLimit(20);
    feedTopicDto.setDateLimitType(FeedTopicDto.DateLimitType.MONTH_AGO);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -1);
    feedTopicDto.setFromDate(calendar.getTime());
    feedTopicDto.setCommitMode(FeedTopicDao.CommitMode.COMMITED_ONLY);

    if (isShowGalleryOnMain) {
      feedTopicDto.getSections().add(Section.SECTION_GALLERY);
    }
    return getCachedFeed(feedTopicDto);
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
   * @param feedTopicDto объект, содержащий условия выборки
   * @return список топиков
   */
  private List<Topic> getCachedFeed(FeedTopicDto feedTopicDto) {
    int cacheAge = getCacheAge(feedTopicDto);
    if (cacheAge == 0) {
      return feedTopicDao.getTopics(feedTopicDto);
    }

    String cacheKey = null;
    try {
      cacheKey = makeCacheKey(feedTopicDto);
      logger.trace("cacheKey=" + cacheKey);
    } catch (UnsupportedEncodingException e) {
      logger.error("Fail to create cache key", e);
      return feedTopicDao.getTopics(feedTopicDto);
    }

    List<Topic> result = (List<Topic>) cacheProvider.getFromCache(cacheKey);
    if (result == null) {
      result = feedTopicDao.getTopics(feedTopicDto);
      cacheProvider.storeToCache(cacheKey, result, cacheAge);
    }
    return result;
  }

  /**
   * Создание уникального ключа для кэша согласно условиям выборки.
   *
   * @param feedTopicDto объект, содержащий условия выборки
   * @return Строка, содержащая уникальный ключ кэша
   * @throws UnsupportedEncodingException
   */
  private String makeCacheKey(FeedTopicDto feedTopicDto)
    throws UnsupportedEncodingException {
    URLUtil.QueryString queryString = new URLUtil.QueryString();
    queryString.add("tg", feedTopicDto.getTag());
    queryString.add("cm", feedTopicDto.getCommitMode());


    for (int section : feedTopicDto.getSections()) {
      queryString.add("sec", section);
    }
    queryString.add("grp", feedTopicDto.getGroup());

    queryString.add("dlmtType", feedTopicDto.getDateLimitType());
    queryString.add("dlmt1", feedTopicDto.getFromDate());
    queryString.add("dlmt2", feedTopicDto.getToDate());
    if (feedTopicDto.getUserId() != 0) {
      queryString.add("u", feedTopicDto.getUserId());
    }

    queryString.add("f", feedTopicDto.isUserFavs());
    queryString.add("lmt", feedTopicDto.getLimit());
    queryString.add("offst", feedTopicDto.getOffset());
    queryString.add("notalks", feedTopicDto.isNotalks());
    queryString.add("tech", feedTopicDto.isTech());

    return "view-news?" + queryString.toString();
  }

  /**
   * Получение "времени жизни" данных в кэше.
   *
   * @param feedTopicDto объект, содержащий условия выборки
   * @return количество миллисекунд.
   */
  private int getCacheAge(FeedTopicDto feedTopicDto) {
    if (feedTopicDto.getLimit() == null || feedTopicDto.getLimit().equals(0)) {
      return 10 * 60 * 1000;
    }

    if (feedTopicDto.getCommitMode() == FeedTopicDao.CommitMode.COMMITED_ONLY) {
      return 0;
    }

    return 30 * 1000;
  }

}
