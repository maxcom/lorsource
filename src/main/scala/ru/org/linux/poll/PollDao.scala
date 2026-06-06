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

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.topic.TopicDao
import ru.org.linux.user.User
import scalikejdbc.*

@Repository
class PollDao(springDB: SpringDB):

  private def getVoteVariants(pollId: Int)(using DBSession): Seq[PollVariant] =
    sql"SELECT v.id, v.label FROM polls_variants v WHERE v.vote = $pollId ORDER BY v.id"
      .map(rs => PollVariant(rs.int("id"), rs.string("label")))
      .list
      .apply()

  /** Возвращает кол-во проголосовавших пользователей в голосовании.
    *
    * @param poll
    *   объект голосования
    * @return
    *   кол-во проголосвавших пользователей
    */
  def getCountUsers(poll: Poll): Int =
    springDB.run:
      sql"SELECT count(DISTINCT userid) FROM vote_users WHERE vote = ${poll.id}"
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)

  /** Возвращает кол-во голосов в голосовании.
    *
    * @param pollId
    *   идентификатор голосвания
    * @return
    *   кол-во голосов всего (несколько вариантов от одного пользователя суммируется"
    */
  def getVotersCount(pollId: Int): Int =
    springDB.run:
      sql"SELECT sum(votes) as s FROM polls_variants WHERE vote = $pollId"
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)

  /** Учет голосования, если user не голосовал в этом голосании, то добавить его варианты в голосование и пометить, что
    * он проголосовал.
    *
    * @param pollId
    *   идентификатор голосования
    * @param votes
    *   пункты за которые голосует пользователь
    * @param user
    *   голосующий пользователь
    * @throws BadVoteException
    *   неправильное голосование
    */
  @throws[BadVoteException]
  @Transactional
  def updateVotes(pollId: Int, votes: Array[Int], user: User): Unit =
    springDB.run:
      val count = sql"SELECT count(vote) FROM vote_users WHERE vote = $pollId AND userid = ${user.id}"
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)
      
      if count == 0 then
        for vote <- votes do
          val (valid, inserted, updated) =
            sql"""
            WITH valid_variant AS (
              SELECT id FROM polls_variants WHERE id = $vote AND vote = $pollId
            ), inserted_vote AS (
              INSERT INTO vote_users (vote, userid, variant_id)
              SELECT $pollId, ${user.id}, id FROM valid_variant
              ON CONFLICT (vote, userid, variant_id) DO NOTHING
              RETURNING variant_id
            ), updated_variant AS (
              UPDATE polls_variants
              SET votes = votes + 1
              WHERE id IN (SELECT variant_id FROM inserted_vote)
              RETURNING id
            )
            SELECT EXISTS(SELECT 1 FROM valid_variant) AS valid,
                   EXISTS(SELECT 1 FROM inserted_vote) AS inserted,
                   EXISTS(SELECT 1 FROM updated_variant) AS updated
          """.map(rs => (rs.boolean("valid"), rs.boolean("inserted"), rs.boolean("updated"))).single.apply().get

          if !valid then
            throw new BadVoteException

          if inserted && !updated then
            throw new IllegalStateException(s"Vote variant $vote for poll $pollId was inserted but not updated")

  /** Получить самое новое голосование.
    *
    * @return
    *   id текущего голосования
    */
  private def getMostRecentPollId: Int =
    springDB.run:
      sql"""SELECT polls.id FROM polls,topics
            WHERE topics.id=polls.topic
            AND topics.moderate = 't'
            AND topics.deleted = 'f'
            AND topics.commitdate = (select max(commitdate) from topics where groupid=19387 AND moderate AND NOT deleted)"""
        .map(rs => rs.int(1))
        .single
        .apply()
        .getOrElse(0)

  /** Получить самое новое голосование.
    *
    * @return
    *   текушие голование
    * @throws PollNotFoundException
    *   если голосование не существует
    */
  @throws[PollNotFoundException]
  def getMostRecentPoll(): Poll = getPoll(getMostRecentPollId)

  /** Получить голосование по идентификатору.
    *
    * @param pollId
    *   идентификатор голосования
    * @return
    *   объект голосование
    * @throws PollNotFoundException
    *   если голосование не существует
    */
  @throws[PollNotFoundException]
  def getPoll(pollId: Int): Poll =
    springDB.run:
      val row = sql"SELECT topic, multiselect FROM polls WHERE id = $pollId"
        .map(rs => (rs.int("topic"), rs.boolean("multiselect")))
        .single
        .apply()
        .getOrElse(throw new PollNotFoundException)
      val variants = getVoteVariants(pollId)
      Poll(pollId, row._1, row._2, variants)

  /** Получить голосование по идентификатору темы.
    *
    * @param topicId
    *   идентификатор темы голосования
    * @return
    *   объект голосование
    * @throws PollNotFoundException
    *   если голосование не существует
    */
  @throws[PollNotFoundException]
  def getPollByTopicId(topicId: Int): Poll =
    springDB.run:
      val row =
        sql"SELECT polls.id, polls.topic, polls.multiselect FROM polls,topics WHERE topics.id = $topicId AND polls.topic=topics.id"
          .map(rs => (rs.int("id"), rs.int("topic"), rs.boolean("multiselect")))
          .single
          .apply()
          .getOrElse(throw new PollNotFoundException)
      val variants = getVoteVariants(row._1)
      Poll(row._1, row._2, row._3, variants)

  /** Варианты опроса для анонимного пользователя
    *
    * @param poll
    *   опрос
    * @return
    *   неизменяемый список вариантов опроса
    */
  def getPollResults(poll: Poll): Seq[PollVariantResult] = getPollResults(poll, Poll.OrderId, None)

  /** Варианты опроса для кокретного пользователя
    *
    * @param poll
    *   объект голосования
    * @param order
    *   порядок сортировки вариантов Poll.ORDER_ID и Poll.ORDER_VOTES
    * @param user
    *   для какого пользователя отдаем
    * @return
    *   неизменяемый список вариантов опроса
    */
  def getPollResults(poll: Poll, order: Int, user: Option[User]): Seq[PollVariantResult] =
    val userId = user.map(_.id).getOrElse(0)

    val q =
      if order == Poll.OrderId then
        sql"""SELECT v.id, v.label, v.votes,
            (exists (select 1 FROM vote_users u WHERE u.vote=v.vote and u.variant_id = v.id
            and u.userid>0 and u.userid=$userId limit 1)) as "userVoted"
            FROM polls_variants v WHERE v.vote=${poll.id} ORDER BY v.id"""
      else if order == Poll.OrderVotes then
        sql"""SELECT v.id, v.label, v.votes,
            (exists (select 1 FROM vote_users u WHERE u.vote=v.vote and u.variant_id = v.id
            and u.userid>0 and u.userid=$userId limit 1)) as "userVoted"
            FROM polls_variants v WHERE v.vote=${poll.id} ORDER BY v.votes DESC, v.id"""
      else
        throw new RuntimeException(s"Oops!? order=$order")

    springDB.run:
      q.map(rs => PollVariantResult(rs.int("id"), rs.string("label"), rs.int("votes"), rs.boolean("userVoted")))
        .list
        .apply()

  /** Создать голосование.
    *
    * @param pollList    - Список вариантов ответов
    * @param multiSelect - true если голосование с мультивыбором
    * @param msgid       - идентификатор темы.
    */
  // call in @Transactional
  def createPoll(pollList: Seq[String], multiSelect: Boolean, msgid: Int): Unit =
    springDB.run:
      val voteid = sql"select nextval('vote_id') as voteid".map(rs => rs.int("voteid")).single.apply().get
      sql"INSERT INTO polls (id, multiselect, topic) VALUES ($voteid, $multiSelect, $msgid)".update.apply()
      for variant <- pollList do
        if variant.trim.nonEmpty then
          sql"INSERT INTO polls_variants (id, vote, label) VALUES (nextval('votes_id'), $voteid, $variant)"
            .update
            .apply()

  /** Добавить новый вариант ответа в голосование.
    *
    * @param poll  объект голосования
    * @param label - новый вариант ответа
    */
  private def addNewVariant(poll: Poll, label: String)(using DBSession): Unit =
    sql"INSERT INTO polls_variants (id, vote, label) VALUES (nextval('votes_id'), ${poll.id}, $label)".update.apply()

  /** Изменить вариант голосования.
    *
    * @param variant
    *   объект варианта голосования
    * @param label
    *   новое содержимое
    */
  private def updateVariant(variant: PollVariant, label: String)(using DBSession): Unit =
    if variant.label != label then
      sql"UPDATE polls_variants SET label = $label WHERE id = ${variant.id}".update.apply()

  /** Удалить вариант голосования
    *
    * @param variant
    *   объект варианта голосования
    */
  private def removeVariant(variant: PollVariant)(using DBSession): Unit =
    sql"DELETE FROM polls_variants WHERE id = ${variant.id}".update.apply()

  /** Обновить признак мультивыбора для опроса
    *
    * @param poll
    *   опрос
    * @param multiselect
    *   признак мультивыбора
    */
  private def updateMultiselect(poll: Poll, multiselect: Boolean)(using DBSession): Unit =
    sql"UPDATE polls SET multiselect = $multiselect WHERE id = ${poll.id}".update.apply()

  @throws[PollNotFoundException]
  @Transactional
  def updatePoll(poll: Poll, newVariants: Seq[PollVariant], multiselect: Boolean): Boolean =
    springDB.run:
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
