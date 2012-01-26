/*
 * Copyright 1998-2010 Linux.org.ru
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

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.user.User;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PollDao {
  private static final String queryPoolIdByTopicId = "SELECT polls.id FROM polls,topics WHERE topics.id=? AND polls.topic=topics.id";
  private static final String queryCurrentPollId = "SELECT polls.id FROM polls,topics WHERE topics.id=polls.topic AND topics.moderate = 't' AND topics.deleted = 'f' AND topics.commitdate = (select max(commitdate) from topics where groupid=19387 AND moderate AND NOT deleted)";
  private static final String queryPool = "SELECT topic, multiselect FROM polls WHERE id=?";
  private static final String queryMaxVotes = "SELECT max(votes) FROM polls_variants WHERE vote=?";
  private static final String queryPollVariantsOrderById = "SELECT id, label, votes FROM polls_variants WHERE vote=? ORDER BY id";
  private static final String queryPollVariantsOrderByVotes = "SELECT id, label, votes FROM polls_variants WHERE vote=? ORDER BY votes DESC, id";
  private static final String queryPollUserVote = "select count(vote) from vote_users where userid=? and variant_id=?";

  private static final String queryCountVotesUser = "SELECT count(vote) FROM vote_users WHERE vote=? AND userid=?";
  private static final String queryCountVotesPool = "SELECT count(DISTINCT userid) FROM vote_users WHERE vote=?";
  private static final String queryCountVotes = "SELECT sum(votes) as s FROM polls_variants WHERE vote=?";
  private static final String updateVote = "UPDATE polls_variants SET votes=votes+1 WHERE id=? AND vote=?";
  private static final String insertVoteUser = "INSERT INTO vote_users VALUES(?, ?, ?)";
  private static final String insertPoll = "INSERT INTO polls (id, multiselect, topic) values (?,?,?)";
  
  private static final String deletePoll1 = "DELETE FROM vote_users     WHERE vote = ?";
  private static final String deletePoll2 = "DELETE FROM polls          WHERE id   = ?";
  private static final String deletePoll3 = "DELETE FROM polls_variants WHERE vote = ?";
  
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
   *
   * @param pollId идентификатор голосования
   * @return список вариантов голосования
   */
  public List<VoteDto> getVoteDTO(int pollId) {
    return jdbcTemplate.query(queryPollVariantsOrderById, new RowMapper<VoteDto>() {
      @Override
      public VoteDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        VoteDto dto = new VoteDto();
        dto.setId(rs.getInt("id"));
        dto.setLabel(rs.getString("label"));
        return dto;
      }
    }, pollId);
  }

  /**
   * Возвращает кол-во проголосовавших пользователей в голосовании.
   *
   * @param poll объект голосования
   * @return кол-во проголосвавших пользователей
   */
  public int getCountUsers(Poll poll) {
    return jdbcTemplate.queryForInt(queryCountVotesPool, poll.getId());
  }

  /**
   * Возвращает кол-во голосов в голосовании.
   *
   * @param pollId идентификатор голосвания
   * @return кол-во голосов всего (несколько вариантов от одного пользователя суммируется"
   */
  public Integer getVotersCount(Integer pollId) {
    return jdbcTemplate.queryForInt(queryCountVotes, pollId);
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
    if(jdbcTemplate.queryForInt(queryCountVotesUser, pollId, user.getId()) == 0){
      for(int vote : votes) {
        if(jdbcTemplate.update(updateVote, vote, pollId) == 0) {
          throw new BadVoteException();
        }
        jdbcTemplate.update(insertVoteUser, pollId, user.getId(), vote);
      }
    }
  }

  /**
   * Возвращает текщее авктивное голосование
   * @return id текущего голосования
   */
  public int getCurrentPollId() {
    try {
      return jdbcTemplate.queryForInt(queryCurrentPollId);
    } catch (EmptyResultDataAccessException exception) {
      return 0;
    }
  }

  /**
   * Получить текщее голосование.
   *
   * @return текушие голование
   * @throws PollNotFoundException если голосование не существует
   */
  public Poll getCurrentPoll() throws PollNotFoundException{
    return getPoll(getCurrentPollId());
  }


  /**
   * Получить голосование по идентификатору.
   *
   * @param poolId идентификатор голосования
   * @return объект голосование
   * @throws PollNotFoundException если голосование не существует
   */
  public Poll getPoll(final int poolId) throws PollNotFoundException {
    final int currentPollId = getCurrentPollId();
    try {
    return jdbcTemplate.queryForObject(queryPool,
        new RowMapper<Poll>() {
          @Override
          public Poll mapRow(ResultSet resultSet, int i) throws SQLException {
            return new Poll(poolId, resultSet.getInt("topic"), resultSet.getBoolean("multiselect"), poolId == currentPollId);
          }
        }, poolId);
    } catch (EmptyResultDataAccessException exception) {
      throw new PollNotFoundException();
    }
  }

  /**
   * Получить голосование по идентификатору темы.
   *
   * @param topicId идентификатор  темы голосования
   * @return объект голосования
   * @throws PollNotFoundException если голосование не существует
   */
  public Poll getPollByTopicId(int topicId) throws PollNotFoundException {
    try {
      return getPoll(jdbcTemplate.queryForInt(queryPoolIdByTopicId, topicId));
    } catch (EmptyResultDataAccessException exception) {
      throw new PollNotFoundException();
    }
  }

  /**
   * максимальное число голосов в голосовании.
   *
   * @param poll объект голосования
   * @return максимальное кол-во голосов
   */
  public int getMaxVote(Poll poll) {
    int max = jdbcTemplate.queryForInt(queryMaxVotes, poll.getId());
    if(max == 0){
      return 1;
    } else {
      return max;
    }
  }

  /**
   * Варианты опроса для ананимного пользователя
   * @param poll опрос
   * @param order порядок сортировки вариантов Poll.ORDER_ID и Poll.ORDER_VOTES
   * @return неизменяемый список вариантов опроса
   */
  public ImmutableList<PollVariant> getPollVariants(Poll poll, int order) {
    return getPollVariants(poll, order, null);
  }

  /**
   * Варианты опроса для кокретного пользователя
   *
   * @param poll  объект голосования
   * @param order порядок сортировки вариантов Poll.ORDER_ID и Poll.ORDER_VOTES
   * @param user для какого пользователя отдаем 
   * @return неизменяемый список вариантов опроса
   */
  public ImmutableList<PollVariant> getPollVariants(Poll poll, int order, final User user) {
    final List<PollVariant> variants = new ArrayList<PollVariant>();
    switch (order) {
      case Poll.ORDER_ID:
        jdbcTemplate.query(queryPollVariantsOrderById, new RowCallbackHandler() {
          @Override
          public void processRow(ResultSet resultSet) throws SQLException {
            int id = resultSet.getInt("id");
            String label = resultSet.getString("label");
            int votes = resultSet.getInt("votes");
            boolean voted = false;
            if(user != null && jdbcTemplate.queryForInt(queryPollUserVote, user.getId(), resultSet.getInt("id")) !=0) {
              voted = true;
            }
            variants.add(new PollVariant(id, label, votes, voted));
          }
        }, poll.getId());
        break;
      case Poll.ORDER_VOTES:
        jdbcTemplate.query(queryPollVariantsOrderByVotes, new RowCallbackHandler() {
          @Override
          public void processRow(ResultSet resultSet) throws SQLException {
            int id = resultSet.getInt("id");
            String label = resultSet.getString("label");
            int votes = resultSet.getInt("votes");
            boolean voted = false;
            if(user != null && jdbcTemplate.queryForInt(queryPollUserVote, user.getId(), resultSet.getInt("id")) !=0) {
              voted = true;
            }
            variants.add(new PollVariant(id, label, votes, voted));
          }
        }, poll.getId());
        break;
      default:
        throw new RuntimeException("Oops!? order="+order);
    }
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
  public void createPoll(List<String> pollList, boolean multiSelect, int msgid) {
    final int voteid = getNextPollId();

    jdbcTemplate.update(insertPoll, voteid, multiSelect, msgid);

    try {
      final Poll poll = getPoll(voteid);

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
    return jdbcTemplate.queryForInt(queryNextPollId);
  }

  /**
   * Удалить голосование.
   *
   * @param poll объект голосования
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void deletePoll(Poll poll) {
    jdbcTemplate.update(deletePoll1, poll.getId());
    jdbcTemplate.update(deletePoll2, poll.getId());
    jdbcTemplate.update(deletePoll3, poll.getId());
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
  public void updateVariant(PollVariant var, String label) {
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
  public void removeVariant(PollVariant variant) {
    jdbcTemplate.update(deleteVariant, variant.getId());
  }

  /**
   * Обновить признак мультивыбора для опроса
   * ALERT: не Transactional метод, использовать только внтури Transactional метода
   * @param poll опрос
   * @param multiselect признак мультивыбора
   */
  public void updateMultiselect(Poll poll, boolean multiselect) {
    jdbcTemplate.update(updateMultiselect, multiselect, poll.getId());
  }
}
