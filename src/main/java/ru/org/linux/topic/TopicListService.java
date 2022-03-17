/*
 * Copyright 1998-2022 Linux.org.ru
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

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.org.linux.group.Group;
import ru.org.linux.section.Section;
import ru.org.linux.tag.TagNotFoundException;
import ru.org.linux.tag.TagService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class TopicListService {
  private static final Logger logger = LoggerFactory.getLogger(TopicListService.class);

  private final TagService tagService;

  private final TopicListDao topicListDao;

  public TopicListService(TagService tagService, TopicListDao topicListDao) {
    this.tagService = tagService;
    this.topicListDao = topicListDao;
  }

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
          Integer month,
          int count,
          @Nullable User currentUser
  ) throws TagNotFoundException {
    logger.debug(
            "TopicListService.getTopicsFeed()" +
                    "; section=" + ((section != null) ? section.toString() : "(null)") +
                    "; group=" + ((group != null) ? group.toString() : "(null)") +
                    "; tag=" + tag +
                    "; offset=" + offset +
                    "; year=" + year +
                    "; month=" + month
    );

    TopicListDto topicListDto = new TopicListDto();

    if (section != null) {
      topicListDto.setSection(section.getId());
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
      topicListDto.setTag(tagService.getTagId(tag, false));
    }

    if (month != null && year != null) {
      topicListDto.setDateLimitType(TopicListDto.DateLimitType.BETWEEN);
      Calendar calendar = Calendar.getInstance();

      calendar.set(year, month - 1, 1, 0, 0, 0);
      topicListDto.setFromDate(calendar.getTime());

      calendar.add(Calendar.MONTH, 1);
      topicListDto.setToDate(calendar.getTime());
    } else {
      topicListDto.setLimit(count);
      topicListDto.setOffset(offset > 0 ? offset : null);
      if (tag == null && group == null && !section.isPremoderated()) {
        topicListDto.setDateLimitType(TopicListDto.DateLimitType.FROM_DATE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, -6);
        topicListDto.setFromDate(calendar.getTime());
      }
    }
    return topicListDao.getTopics(topicListDto, currentUser);
  }

  /**
   * Получение списка топиков пользователя.
   *
   * @param user       объект пользователя
   * @param offset     смещение в результатах выборки
   * @param isFavorite true если нужно выбрать избранные сообщения пользователя
   * @return список топиков пользователя
   */
  public List<Topic> getUserTopicsFeed(User user, Integer offset, boolean isFavorite, boolean watches) {
    return getUserTopicsFeed(user, null, null, offset, isFavorite, watches);
  }

  /**
   * Получение списка топиков пользователя.
   *
   * @param user      объект пользователя
   * @param section   секция, из которой выбрать сообщения
   * @param offset    смещение в результатах выборки
   * @param favorites true если нужно выбрать избранные сообщения пользователя
   * @return список топиков пользователя
   */
  public List<Topic> getUserTopicsFeed(User user, Section section, Group group, Integer offset, boolean favorites, boolean watches) {
    logger.debug(
            "TopicListService.getTopicsFeed()" +
                    "; user=" + ((user != null) ? user.toString() : "(null)") +
                    "; section=" + section +
                    "; offset=" + offset +
                    "; isFavorite=" + favorites
    );

    TopicListDto topicListDto = new TopicListDto();
    topicListDto.setLimit(20);
    topicListDto.setOffset(offset);
    topicListDto.setCommitMode(TopicListDao.CommitMode.ALL);
    topicListDto.setUserId(user.getId());
    topicListDto.setUserFavs(favorites);
    topicListDto.setUserWatches(watches);
    if (section != null) {
      topicListDto.setSection(section.getId());
    }
    if (group != null) {
      topicListDto.setGroup(group.getId());
    }

    return topicListDao.getTopics(topicListDto, null);
  }

  /**
   * Получение списка черновиков пользователя.
   *
   * @param user   объект пользователя
   * @param offset смещение в результатах выборки
   * @return список топиков пользователя
   */
  public List<Topic> getDrafts(User user, Integer offset) {
    TopicListDto topicListDto = new TopicListDto();
    topicListDto.setLimit(20);
    topicListDto.setOffset(offset);
    topicListDto.setCommitMode(TopicListDao.CommitMode.ALL);
    topicListDto.setUserId(user.getId());
    topicListDto.setShowDraft(true);

    return topicListDao.getTopics(topicListDto, null);
  }


  /**
   * Получение списка топиков для RSS-ленты.
   *
   * @param section  секция
   * @param group    группа
   * @param fromDate от какой даты получить список
   * @param noTalks  без Talks
   * @param tech     только технические
   * @return список топиков для RSS-ленты
   */
  public List<Topic> getRssTopicsFeed(
          Section section,
          Group group,
          Date fromDate,
          boolean noTalks,
          boolean tech
  ) {
    logger.debug(
            "TopicListService.getRssTopicsFeed()" +
                    "; section=" + ((section != null) ? section.toString() : "(null)") +
                    "; group=" + ((group != null) ? group.toString() : "(null)") +
                    "; fromDate=" + fromDate +
                    "; noTalks=" + noTalks +
                    "; tech=" + tech
    );

    TopicListDto topicListDto = new TopicListDto();

    if (section != null) {
      topicListDto.setSection(section.getId());
    }
    if (group != null) {
      topicListDto.setGroup(group.getId());
    }

    topicListDto.setDateLimitType(TopicListDto.DateLimitType.FROM_DATE);
    topicListDto.setFromDate(fromDate);

    topicListDto.setNotalks(noTalks);
    topicListDto.setTech(tech);
    topicListDto.setLimit(100);

    if (section.isPremoderated()) {
      topicListDto.setCommitMode(TopicListDao.CommitMode.COMMITED_ONLY);
    } else {
      topicListDto.setCommitMode(TopicListDao.CommitMode.POSTMODERATED_ONLY);
    }
    return topicListDao.getTopics(topicListDto, null);
  }

  public List<Topic> getUncommitedTopic(Section section, Date fromDate, boolean includeAnonymous) {
    TopicListDto topicListDto = new TopicListDto();
    topicListDto.setCommitMode(TopicListDao.CommitMode.UNCOMMITED_ONLY);
    if (section != null) {
      topicListDto.setSection(section.getId());
    }

    topicListDto.setDateLimitType(TopicListDto.DateLimitType.FROM_DATE);
    topicListDto.setFromDate(fromDate);
    topicListDto.setIncludeAnonymous(includeAnonymous);

    return topicListDao.getTopics(topicListDto, null);
  }

  public List<DeletedTopic> getDeletedTopics(int sectionId, boolean skipEmptyReason, boolean includeAnonymous) {
    return topicListDao.getDeletedTopics(sectionId, skipEmptyReason, includeAnonymous);
  }

  public List<Topic> getMainPageFeed(boolean showGalleryOnMain, int count, boolean hideMinor) {
    TopicListDto topicListDto = new TopicListDto();

    topicListDto.setLimit(count);

    topicListDto.setDateLimitType(TopicListDto.DateLimitType.FROM_DATE);
    // лучше бы этот лимит был по commit_date на главной
    topicListDto.setFromDate(DateTime.now().minusMonths(3).toDate());

    if (hideMinor) {
      topicListDto.setMiniNewsMode(TopicListDto.MiniNewsMode.MAJOR);
    }

    topicListDto.setCommitMode(TopicListDao.CommitMode.COMMITED_ONLY);

    if (showGalleryOnMain) {
      topicListDto.setSection(Section.SECTION_NEWS, Section.SECTION_GALLERY);
    } else {
      topicListDto.setSection(Section.SECTION_NEWS);
    }

    return topicListDao.getTopics(topicListDto, null);
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

  public List<Topic> getTopics(TopicListDto topicListDto, @Nullable User currentUser) {
    return topicListDao.getTopics(topicListDto, currentUser);
  }

  public List<DeletedTopic> getDeletedUserTopics(User user, int topics) {
    return topicListDao.getDeletedUserTopics(user, topics);
  }
}