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

package ru.org.linux.poll;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.user.User;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class PollDao {
  private static final String queryPoolIdByTopicId = "SELECT polls.id FROM polls,topics WHERE topics.id=? AND polls.topic=topics.id";
  private static final String queryCurrentPollId = "SELECT polls.id FROM polls,topics WHERE topics.id=polls.topic AND topics.moderate = 't' AND topics.deleted = 'f' AND topics.commitdate = (select max(commitdate) from topics where groupid=19387 AND moderate AND NOT deleted)";
  private static final String queryPool = "SELECT topic, multiselect FROM polls WHERE id=?";
  private static final String queryPollVariantsOrderById =
          "SELECT v.id, v.label, v.votes, (SELECT count(u.vote) FROM vote_users u " +
                  " WHERE u.vote=v.vote and u.variant_id = v.id and u.userid>0 and u.userid=?) as \"userVoted\" FROM polls_variants v WHERE v.vote=? ORDER BY v.id";
  private static final String queryPollVariantsOrderByVotes = "SELECT id, label, votes FROM polls_variants WHERE vote=? ORDER BY votes DESC, id";
  private static final String queryPollUserVote = "select count(vote) from vote_users where userid=? and variant_id=?";

  private static final String queryCountVotesUser = "SELECT count(vote) FROM vote_users WHERE vote=? AND userid=?";
  private static final String queryCountVotesPool = "SELECT count(DISTINCT userid) FROM vote_users WHERE vote=?";
  private static final String queryCountVotes = "SELECT sum(votes) as s FROM polls_variants WHERE vote=?";
  private static final String updateVote = "UPDATE polls_variants SET votes=votes+1 WHERE id=? AND vote=?";
  private static final String insertVoteUser = "INSERT INTO vote_users VALUES(?, ?, ?)";
  private static final String insertPoll = "INSERT INTO polls (id, multiselect, topic) values (?,?,?)";
  
  private static final String queryNextPollId = "select nextval('vote_id') as voteid";
  
  private static final String insertNewVariant = "INSERT INTO polls_variants (id, vote, label) values (nextval('votes_id'), ?, ?)";
  private static final String updateVariant = "UPDATE polls_variants SET label=? WHERE id=?";
  private static final String deleteVariant = "DELETE FROM polls_variants WHERE id=?";
  
  private static final String updateMultiselect = "UPDATE polls SET multiselect=? WHERE id=?";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  /**
   * Получить список вариантов голосования по идентификатору голосования.
   * Список отсортирован по id варианта
   *
   * @param pollId идентификатор голосования
   * @return список вариантов голосования
   */
  private List<PollVariant> getVoteDTO(int pollId,int userId) {
    return jdbcTemplate.query(queryPollVariantsOrderById, (rs, rowNum) ->
            new PollVariant(rs.getInt("id"),
                    rs.getString("label"),rs.getInt("userVoted")), userId,pollId);
  }

  /**
   * Возвращает кол-во проголосовавших пользователей в голосовании.
   *
   * @param poll объект голосования
   * @return кол-во проголосвавших пользователей
   */
  public int getCountUsers(Poll poll) {
    return jdbcTemplate.queryForObject(queryCountVotesPool, Integer.class, poll.getId());
  }

  /**
   * Возвращает кол-во голосов в голосовании.
   *
   * @param pollId идентификатор голосвания
   * @return кол-во голосов всего (несколько вариантов от одного пользователя суммируется"
   */
  public int getVotersCount(int pollId) {
    return jdbcTemplate.queryForObject(queryCountVotes, Integer.class, pollId);
  }

  /**
   * Учет голосования, если user не голосовал в этом голосании, то
   * добавить его варианты в голосование и пометить, что он проголосовал.
   *
   * @param pollId идентификатор голосования
   * @param votes  пункты за которые голосует пользователь
   * @param user   голосующий пользователь
   * @throws BadVoteException неправильное голосование
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void updateVotes(int pollId, int[] votes, User user) throws BadVoteException {
    if(jdbcTemplate.queryForObject(queryCountVotesUser, Integer.class, pollId, user.getId()) == 0){
      for(int vote : votes) {
        if(jdbcTemplate.update(updateVote, vote, pollId) == 0) {
          throw new BadVoteException();
        }
        jdbcTemplate.update(insertVoteUser, pollId, user.getId(), vote);
      }
    }
  }

  /**
   * Получить самое новое голосование.
   * @return id текущего голосования
   */
  public int getMostRecentPollId() {
    try {
      return jdbcTemplate.queryForObject(queryCurrentPollId, Integer.class);
    } catch (EmptyResultDataAccessException exception) {
      return 0;
    }
  }

  /**
   * Получить самое новое голосование.
   *
   * @return текушие голование
   * @throws PollNotFoundException если голосование не существует
   */
  public Poll getMostRecentPoll(int userId) throws PollNotFoundException{
    return getPoll(getMostRecentPollId(),userId);
  }

  /**
   * Получить голосование по идентификатору.
   *
   * @param pollId идентификатор голосования
   * @return объект голосование
   * @throws PollNotFoundException если голосование не существует
   */
  public Poll getPoll(final int pollId,int userId) throws PollNotFoundException {
    SqlRowSet rs = jdbcTemplate.queryForRowSet(queryPool, pollId);

    if (!rs.next()) {
      throw new PollNotFoundException();
    }

    return Poll.apply(
            pollId,
            rs.getInt("topic"),
            rs.getBoolean("multiselect"),
            getVoteDTO(pollId,userId)
    );
  }

  /**
   * Получить голосование по идентификатору темы.
   *
   * @param topicId идентификатор  темы голосования
   * @return объект голосования
   * @throws PollNotFoundException если голосование не существует
   */
  public Poll getPollByTopicId(int topicId,int userId) throws PollNotFoundException {
    try {
      return getPoll(jdbcTemplate.queryForObject(queryPoolIdByTopicId, Integer.class, topicId),userId);
    } catch (EmptyResultDataAccessException exception) {
      throw new PollNotFoundException();
    }
  }

  /**
   * Варианты опроса для ананимного пользователя
   *
   * @param poll опрос
   * @return неизменяемый список вариантов опроса
   */
  public ImmutableList<PollVariantResult> getPollVariants(Poll poll) {
    return getPollVariants(poll, Poll.OrderId(), null);
  }

  /**
   * Варианты опроса для кокретного пользователя
   *
   * @param poll  объект голосования
   * @param order порядок сортировки вариантов Poll.ORDER_ID и Poll.ORDER_VOTES
   * @param user для какого пользователя отдаем 
   * @return неизменяемый список вариантов опроса
   */
  public ImmutableList<PollVariantResult> getPollVariants(Poll poll, int order, final User user) {
    final List<PollVariantResult> variants = new ArrayList<>();
    
    String query;

    if (order == Poll.OrderId()) {
      query = queryPollVariantsOrderById;
    } else if (order == Poll.OrderVotes()) {
      query = queryPollVariantsOrderByVotes;
    } else {
      throw new RuntimeException("Oops!? order=" + order);
    }

    jdbcTemplate.query(query, resultSet -> {
      int id = resultSet.getInt("id");
      String label = resultSet.getString("label");
      int votes = resultSet.getInt("votes");
      boolean voted = false;
      if(user != null && jdbcTemplate.queryForObject(queryPollUserVote, Integer.class, user.getId(), resultSet.getInt("id")) !=0) {
        voted = true;
      }
      variants.add(new PollVariantResult(id, label, votes, voted));
    }, poll.getId());

    return ImmutableList.copyOf(variants);
  }

  /**
   * Создать голосование.
   *
   * @param pollList    - Список вариантов ответов
   * @param multiSelect - true если голосование с мультивыбором
   * @param msgid       - идентификатор темы.
   */
  // call in @Transactional
  public void createPoll(List<String> pollList, boolean multiSelect, int msgid,int userId) {
    final int voteid = getNextPollId();

    jdbcTemplate.update(insertPoll, voteid, multiSelect, msgid);

    try {
      final Poll poll = getPoll(voteid,userId);

      for (String variant : pollList) {
        if (variant.trim().isEmpty()) {
          continue;
        }

        addNewVariant(poll, variant);
      }
    } catch (PollNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Получить идентификатор будущего голосования
   *
   * @return идентификатор будущего голосования
   */
  private int getNextPollId() {
    return jdbcTemplate.queryForObject(queryNextPollId, Integer.class);
  }

  /**
   * Добавить новый вариант ответа в голосование.
   *
   * @param poll  объект голосования
   * @param label - новый вариант ответа
   */
  public void addNewVariant(Poll poll, String label) {
    jdbcTemplate.update(
            insertNewVariant,
            poll.getId(),
            label
    );
  }

  /**
   * Изменить вариант голосования.
   *
   * @param var   объект варианта голосования
   * @param label новое содержимое
   */
  private void updateVariant(PollVariant var, String label) {
    if (var.getLabel().equals(label)) {
      return;
    }

    jdbcTemplate.update(updateVariant, label, var.getId());
  }

  /**
   * Удалить вариант голосования
   *
   * @param variant объект варианта голосования
   */
  void removeVariant(PollVariant variant) {
    jdbcTemplate.update(deleteVariant, variant.getId());
  }

  /**
   * Обновить признак мультивыбора для опроса
   * @param poll опрос
   * @param multiselect признак мультивыбора
   */
  private void updateMultiselect(Poll poll, boolean multiselect) {
    jdbcTemplate.update(updateMultiselect, multiselect, poll.getId());
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public boolean updatePoll(Poll poll, List<PollVariant> newVariants, boolean multiselect) throws PollNotFoundException {
    boolean modified = false;

    List<PollVariant> oldVariants = poll.getVariants();

    Map<Integer, String> newMap = PollVariant.toMap(newVariants);

    for (final PollVariant var : oldVariants) {
      final String label = newMap.get(var.getId());

      if (!TopicDao.equalStrings(var.getLabel(), label)) {
        modified = true;
      }

      if (Strings.isNullOrEmpty(label)) {
        removeVariant(var);
      } else {
        updateVariant(var, label);
      }
    }

    for (final PollVariant var : newVariants) {
      if (var.getId()==0 && !Strings.isNullOrEmpty(var.getLabel())) {
        modified = true;

        addNewVariant(poll, var.getLabel());
      }
    }

    if (poll.isMultiSelect()!=multiselect) {
      modified = true;
      updateMultiselect(poll, multiselect);
    }

    return modified;
  }
}
