/*
 * Copyright 1998-2026 Linux.org.ru
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
package ru.org.linux.poll

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Repository
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.topic.TopicDao
import ru.org.linux.user.User

import javax.sql.DataSource

@Repository
class PollDao(ds: DataSource, val transactionManager: PlatformTransactionManager) extends TransactionManagement {
  private val jdbcTemplate = new JdbcTemplate(ds)

  private val QueryPoolIdByTopicId = "SELECT polls.id FROM polls,topics WHERE topics.id=? AND polls.topic=topics.id"
  private val QueryCurrentPollId = "SELECT polls.id FROM polls,topics WHERE topics.id=polls.topic AND topics.moderate = 't' AND topics.deleted = 'f' AND topics.commitdate = (select max(commitdate) from topics where groupid=19387 AND moderate AND NOT deleted)"
  private val QueryPool = "SELECT topic, multiselect FROM polls WHERE id=?"
  /**
   * запрос для получения вариантов ответа
   *  черная магия "and u.userid>0 and u.userid=?" нужна для пропуска поиска результатов пользователя если userid задан как 0
   */
  private val QueryPollResultsOrderById =
    "SELECT v.id, v.label, v.votes, (exists (select 1 FROM vote_users u  WHERE u.vote=v.vote and u.variant_id = v.id " +
      " and u.userid>0 and u.userid=? limit 1)) as \"userVoted\" FROM polls_variants v WHERE v.vote=? ORDER BY v.id"

  private val QueryPollVariants =
    "SELECT v.id, v.label, v.votes FROM polls_variants v WHERE v.vote=? ORDER BY v.id"
  /**
   * запрос для получения статистики ответов, сортировка по количеству проголосовавших
   * userId тут тоже нужен поскольку на странице используется подсветка выбранных юзером вариантов
   */
  private val QueryPollResultsOrderByVotes = "SELECT v.id, v.label, v.votes, " +
    " (exists (select 1 FROM vote_users u  WHERE u.vote=v.vote and u.variant_id = v.id " +
    " and u.userid>0 and u.userid=? limit 1)) as \"userVoted\" FROM polls_variants v WHERE v.vote=? ORDER BY v.votes DESC, v.id"

  private val QueryCountVotesUser = "SELECT count(vote) FROM vote_users WHERE vote=? AND userid=?"
  private val QueryCountVotesPool = "SELECT count(DISTINCT userid) FROM vote_users WHERE vote=?"
  private val QueryCountVotes = "SELECT sum(votes) as s FROM polls_variants WHERE vote=?"
  private val UpdateVote = "UPDATE polls_variants SET votes=votes+1 WHERE id=? AND vote=?"
  private val InsertVoteUser = "INSERT INTO vote_users VALUES(?, ?, ?)"
  private val InsertPoll = "INSERT INTO polls (id, multiselect, topic) values (?,?,?)"

  private val QueryNextPollId = "select nextval('vote_id') as voteid"

  private val InsertNewVariant = "INSERT INTO polls_variants (id, vote, label) values (nextval('votes_id'), ?, ?)"
  private val UpdateVariant = "UPDATE polls_variants SET label=? WHERE id=?"
  private val DeleteVariant = "DELETE FROM polls_variants WHERE id=?"

  private val UpdateMultiselect = "UPDATE polls SET multiselect=? WHERE id=?"

  /**
   * Получить список вариантов голосования по идентификатору голосования.
   * Список отсортирован по id варианта
   *
   * @param pollId идентификатор голосования
   * @return список вариантов голосования
   */
  private def getVoteVariants(pollId: Int): Seq[PollVariant] =
    jdbcTemplate.queryAndMap(QueryPollVariants, pollId) { (rs, _) =>
      PollVariant(rs.getInt("id"), rs.getString("label"))
    }.toSeq

  /**
   * Возвращает кол-во проголосовавших пользователей в голосовании.
   *
   * @param poll объект голосования
   * @return кол-во проголосвавших пользователей
   */
  def getCountUsers(poll: Poll): Int = {
    jdbcTemplate.queryForObject[Integer](QueryCountVotesPool, poll.id).get
  }

  /**
   * Возвращает кол-во голосов в голосовании.
   *
   * @param pollId идентификатор голосвания
   * @return кол-во голосов всего (несколько вариантов от одного пользователя суммируется"
   */
  def getVotersCount(pollId: Int): Int = {
    jdbcTemplate.queryForObject[Integer](QueryCountVotes, pollId).get
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
  @throws[BadVoteException]
  def updateVotes(pollId: Int, votes: Array[Int], user: User): Unit = transactional() { _ =>
    if jdbcTemplate.queryForObject[Integer](QueryCountVotesUser, pollId, user.id).get == 0 then
      for vote <- votes do
        if jdbcTemplate.update(UpdateVote, vote, pollId) == 0 then
          throw new BadVoteException
        jdbcTemplate.update(InsertVoteUser, pollId, user.id, vote)
  }

  /**
   * Получить самое новое голосование.
   *
   * @return id текущего голосования
   */
  def getMostRecentPollId: Int = {
    try {
      jdbcTemplate.queryForObject[Integer](QueryCurrentPollId).get
    } catch {
      case _: EmptyResultDataAccessException => 0
    }
  }

  /**
   * Получить самое новое голосование.
   *
   * @return текушие голование
   * @throws PollNotFoundException если голосование не существует
   */
  @throws[PollNotFoundException]
  def getMostRecentPoll(): Poll = getPoll(getMostRecentPollId)

  /**
   * Получить голосование по идентификатору.
   *
   * @param pollId идентификатор голосования
   * @return объект голосование
   * @throws PollNotFoundException если голосование не существует
   */
  @throws[PollNotFoundException]
  def getPoll(pollId: Int): Poll = {
    val rs = jdbcTemplate.queryForRowSet(QueryPool, pollId)

    if !rs.next then
      throw new PollNotFoundException

    Poll(
      pollId,
      rs.getInt("topic"),
      rs.getBoolean("multiselect"),
      getVoteVariants(pollId)
    )
  }

  /**
   * Получить голосование по идентификатору темы.
   *
   * @param topicId идентификатор  темы голосования
   * @param userId  идентификатор пользователя, может быть 0 для пропуска
   * @return объект голосования
   * @throws PollNotFoundException если голосование не существует
   */
  @throws[PollNotFoundException]
  def getPollByTopicId(topicId: Int): Poll = {
    try {
      getPoll(jdbcTemplate.queryForObject[Integer](QueryPoolIdByTopicId, topicId).get)
    } catch {
      case _: EmptyResultDataAccessException =>
        throw new PollNotFoundException
    }
  }

  /**
   * Варианты опроса для анонимного пользователя
   *
   * @param poll опрос
   * @return неизменяемый список вариантов опроса
   */
  def getPollResults(poll: Poll): Seq[PollVariantResult] =
    getPollResults(poll, Poll.OrderId, null)

  /**
   * Варианты опроса для кокретного пользователя
   *
   * @param poll  объект голосования
   * @param order порядок сортировки вариантов Poll.ORDER_ID и Poll.ORDER_VOTES
   * @param user  для какого пользователя отдаем
   * @return неизменяемый список вариантов опроса
   */
  def getPollResults(poll: Poll, order: Int, user: User): Seq[PollVariantResult] =
    val q = if order == Poll.OrderId then
      QueryPollResultsOrderById
    else if order == Poll.OrderVotes then
      QueryPollResultsOrderByVotes
    else
      throw new RuntimeException(s"Oops!? order=$order")

    val userId = if user != null then user.id else 0

    jdbcTemplate.queryAndMap(q, userId, poll.id) { (rs, _) =>
      PollVariantResult(rs.getInt("id"), rs.getString("label"), rs.getInt("votes"), rs.getBoolean("userVoted"))
    }.toSeq

  /**
   * Создать голосование.
   *
   * @param pollList    - Список вариантов ответов
   * @param multiSelect - true если голосование с мультивыбором
   * @param msgid       - идентификатор темы.
   */
  // call in @Transactional
  def createPoll(pollList: Seq[String], multiSelect: Boolean, msgid: Int): Unit =
    val voteid = getNextPollId

    jdbcTemplate.update(InsertPoll, voteid, multiSelect, msgid)

    try {
      val poll = getPoll(voteid)
      for variant <- pollList do
        if !variant.trim.isEmpty then
          addNewVariant(poll, variant)
    } catch {
      case e: PollNotFoundException =>
        throw new RuntimeException(e)
    }

  /**
   * Получить идентификатор будущего голосования
   *
   * @return идентификатор будущего голосования
   */
  private def getNextPollId: Int = {
    jdbcTemplate.queryForObject[Integer](QueryNextPollId).get
  }

  /**
   * Добавить новый вариант ответа в голосование.
   *
   * @param poll  объект голосования
   * @param label - новый вариант ответа
   */
  def addNewVariant(poll: Poll, label: String): Unit = {
    jdbcTemplate.update(
      InsertNewVariant,
      poll.id,
      label
    )
  }

  /**
   * Изменить вариант голосования.
   *
   * @param variant объект варианта голосования
   * @param label   новое содержимое
   */
  private def updateVariant(variant: PollVariant, label: String): Unit = {
    if variant.label == label then
      return

    jdbcTemplate.update(UpdateVariant, label, variant.id)
  }

  /**
   * Удалить вариант голосования
   *
   * @param variant объект варианта голосования
   */
  def removeVariant(variant: PollVariant): Unit = {
    jdbcTemplate.update(DeleteVariant, variant.id)
  }

  /**
   * Обновить признак мультивыбора для опроса
   *
   * @param poll        опрос
   * @param multiselect признак мультивыбора
   */
  private def updateMultiselect(poll: Poll, multiselect: Boolean): Unit = {
    jdbcTemplate.update(UpdateMultiselect, multiselect, poll.id)
  }

  @throws[PollNotFoundException]
  def updatePoll(poll: Poll, newVariants: Seq[PollVariant], multiselect: Boolean): Boolean = transactional() { _ =>
    var modified = false

    val oldVariants = poll.variants

    val newMap = newVariants.map(v => v.id -> v.label).toMap

    for oldVar <- oldVariants do
      val label = newMap.get(oldVar.id)

      if !TopicDao.equalStrings(oldVar.label, label.orNull) then
        modified = true

      if label.isEmpty || label.get.isEmpty then
        removeVariant(oldVar)
      else
        updateVariant(oldVar, label.get)

    for newVar <- newVariants do
      if newVar.id == 0 && newVar.label != null && newVar.label.nonEmpty then
        modified = true
        addNewVariant(poll, newVar.label)

    if poll.multiSelect != multiselect then
      modified = true
      updateMultiselect(poll, multiselect)

    modified
  }
}