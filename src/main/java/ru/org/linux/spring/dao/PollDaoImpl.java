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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.org.linux.site.Poll;
import ru.org.linux.site.PollNotFoundException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class PollDaoImpl {

  private static final String queryPoolIdByTopicId = "SELECT votenames.id FROM votenames,topics WHERE topics.id=? AND votenames.topic=topics.id";
  private static final String queryCurrentPollId = "SELECT votenames.id FROM votenames,topics WHERE topics.id=votenames.topic AND topics.moderate = 't' AND topics.deleted = 'f' AND topics.commitdate = (select max(commitdate) from topics where groupid=19387 AND moderate AND NOT deleted)";
  private static final String queryPool = "SELECT topic, multiselect FROM votenames WHERE id=?";

  private SimpleJdbcTemplate jdbcTemplate;

  public SimpleJdbcTemplate getJdbcTemplate() {
    return jdbcTemplate;
  }

  @Autowired
  public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<VoteDTO> getVoteDTO(final Integer pollId) {
    String sql = "SELECT id, label FROM votes WHERE vote= ? ORDER BY id";
    return jdbcTemplate.query(sql, new RowMapper<VoteDTO>() {
      @Override
      public VoteDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        VoteDTO dto = new VoteDTO();
        dto.setId(rs.getInt("id"));
        dto.setLabel(rs.getString("label"));
        dto.setPollId(pollId);
        return dto;
      }
    }, pollId);
  }

  public Integer getVotersCount(Integer pollId) {
    String sql = "SELECT sum(votes) as s FROM votes WHERE vote= ?";
    return jdbcTemplate.queryForInt(sql, pollId);
  }

  /**
   * Получить id Голосования по id топика
   * @param topicId id топика
   * @return id голосования
   * @throws PollNotFoundException гененрируется если такого голосования не существует
   */
  public int getPollId(int topicId) throws PollNotFoundException {
    try {
      return jdbcTemplate.queryForObject(queryPoolIdByTopicId, Integer.class, topicId);
    } catch (EmptyResultDataAccessException exception) {
      throw new PollNotFoundException();
    }
  }

  /**
   * Возвращает текщее авктивное голосование
   * @return id текущего голосования
   */
  public int getCurrentPollId() {
    try {
      return jdbcTemplate.queryForObject(queryCurrentPollId, Integer.class);
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
}
