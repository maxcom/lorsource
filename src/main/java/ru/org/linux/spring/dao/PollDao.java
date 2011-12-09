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

package ru.org.linux.spring.dao;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.poll.Poll;
import ru.org.linux.poll.PollNotFoundException;
import ru.org.linux.poll.PollVariant;
import ru.org.linux.site.*;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PollDao {
  private static final String queryPoolIdByTopicId = "SELECT votenames.id FROM votenames,topics WHERE topics.id=? AND votenames.topic=topics.id";
  private static final String queryCurrentPollId = "SELECT votenames.id FROM votenames,topics WHERE topics.id=votenames.topic AND topics.moderate = 't' AND topics.deleted = 'f' AND topics.commitdate = (select max(commitdate) from topics where groupid=19387 AND moderate AND NOT deleted)";
  private static final String queryPool = "SELECT topic, multiselect FROM votenames WHERE id=?";
  private static final String queryMaxVotes = "SELECT max(votes) FROM votes WHERE vote=?";
  private static final String queryPollVariantsOrderById = "SELECT * FROM votes WHERE vote=? ORDER BY id";
  private static final String queryPollVariantsOrderByVotes = "SELECT * FROM votes WHERE vote=? ORDER BY votes DESC, id";

  private static final String queryCountVotesUser = "SELECT count(vote) FROM vote_users WHERE vote=? AND userid=?";
  private static final String queryCountVotesPool = "SELECT count(userid) FROM vote_users WHERE vote=?";
  private static final String queryCountVotes = "SELECT sum(votes) as s FROM votes WHERE vote=?";
  private static final String updateVote = "UPDATE votes SET votes=votes+1 WHERE id=? AND vote=?";
  private static final String insertVoteUser = "INSERT INTO vote_users VALUES(?, ?)";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  public List<VoteDto> getVoteDTO(final Integer pollId) {
    String sql = "SELECT id, label FROM votes WHERE vote= ? ORDER BY id";
    return jdbcTemplate.query(sql, new RowMapper<VoteDto>() {
      @Override
      public VoteDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        VoteDto dto = new VoteDto();
        dto.setId(rs.getInt("id"));
        dto.setLabel(rs.getString("label"));
        dto.setPollId(pollId);
        return dto;
      }
    }, pollId);
  }

  /**
   * Возвращает кол-во проголосовавших пользователей в голосовании
   * @param poll голосование
   * @return кол-во проголосвавших пользователей
   */
  public int getCountUsers(Poll poll) {
    return jdbcTemplate.queryForInt(queryCountVotesPool, poll.getId());
  }

  /**
   * Возвращает кол-во голосов в голосовании
   * @param pollId id голосвания
   * @return кол-во голосов всего (несколько вариантов от одного пользователя суммируется"
   */
  public Integer getVotersCount(Integer pollId) {
    return jdbcTemplate.queryForInt(queryCountVotes, pollId);
  }

  /**
   * Учет голсования, если user не голосовал в этом голосании, то
   * добавить его варианты в голосование и пометить, что он проголосовал
   * @param voteId id голосования
   * @param votes пункты за которые голосует пользователь
   * @param user голосующий пользователь
   * @throws BadVoteException неправильное голосование
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void updateVotes(int voteId, int[] votes, User user) throws BadVoteException {
    if(jdbcTemplate.queryForInt(queryCountVotesUser, voteId, user.getId()) == 0){
      for(int vote : votes) {
        if(jdbcTemplate.update(updateVote, vote, voteId) == 0) {
          throw new BadVoteException();
        }
      }
      jdbcTemplate.update(insertVoteUser, voteId, user.getId());
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
   * Получить текщее голосование
   * @return текушие голование
   * @throws PollNotFoundException при отсутствии голосования
   */
  public Poll getCurrentPoll() throws PollNotFoundException{
    return getPoll(getCurrentPollId());
  }


  /**
   * Получить голосование по id
   * @param poolId голосование
   * @return голосование
   * @throws PollNotFoundException если не существует такого голосования
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
   * Получить голосование по topic id
   * @param topicId id топика голосования
   * @return голосование
   * @throws PollNotFoundException отсутствует такое голосование
   */
  public Poll getPollByTopicId(int topicId) throws PollNotFoundException {
    try {
      return getPoll(jdbcTemplate.queryForInt(queryPoolIdByTopicId, topicId));
    } catch (EmptyResultDataAccessException exception) {
      throw new PollNotFoundException();
    }
  }

  /**
   * максимальное число голосов в голосовании
   * @param poll голосование
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
   * Варианты для опроса
   * @param poll опрос
   * @param order порядок сортировки вариантов Poll.ORDER_ID и Poll.ORDER_VOTES
   * @return неизменяемый список вариантов опроса
   */
  public ImmutableList<PollVariant> getPollVariants(Poll poll, int order) {
    final List<PollVariant> variants = new ArrayList<PollVariant>();
    switch (order) {
      case Poll.ORDER_ID:
        jdbcTemplate.query(queryPollVariantsOrderById, new RowCallbackHandler() {
          @Override
          public void processRow(ResultSet resultSet) throws SQLException {
            variants.add(new PollVariant(resultSet.getInt("id"),
                                         resultSet.getString("label"),
                                         resultSet.getInt("votes")));
          }
        }, poll.getId());
        break;
      case Poll.ORDER_VOTES:
        jdbcTemplate.query(queryPollVariantsOrderByVotes, new RowCallbackHandler() {
          @Override
          public void processRow(ResultSet resultSet) throws SQLException {
            variants.add(new PollVariant(resultSet.getInt("id"),
                                         resultSet.getString("label"),
                                         resultSet.getInt("votes")));
          }
        }, poll.getId());
        break;
      default:
        throw new RuntimeException("Oops!? order="+order);
    }
    return ImmutableList.copyOf(variants);
  }

  // call in @Transactional
  public void createPoll(List<String> pollList, boolean multiSelect, int msgid) {
    final int voteid = getNextPollId();

    jdbcTemplate.update("INSERT INTO votenames (id, multiselect, topic) values (?,?,?)", voteid, multiSelect, msgid);

    try {
      final Poll poll = getPoll(voteid);

      for (String variant : pollList) {
        if (variant.trim().length() == 0) {
          continue;
        }

        addNewVariant(poll, variant);
      }
    } catch (PollNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private int getNextPollId() {
    return jdbcTemplate.queryForInt("select nextval('vote_id') as voteid");
  }

  public void addNewVariant(Poll poll, String label) {
    jdbcTemplate.update(
            "INSERT INTO votes (id, vote, label) values (nextval('votes_id'), ?, ?)",
            poll.getId(),
            label
    );
  }

  public void updateVariant(PollVariant var, String label) {
    if (var.getLabel().equals(label)) {
      return;
    }

    jdbcTemplate.update("UPDATE votes SET label=? WHERE id=?", label, var.getId());
  }

  public void removeVariant(PollVariant variant) {
    jdbcTemplate.update("DELETE FROM votes WHERE id=?", variant.getId());
  }
}
