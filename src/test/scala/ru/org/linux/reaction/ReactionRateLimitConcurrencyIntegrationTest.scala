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
package ru.org.linux.reaction

import munit.FunSuite
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.comment.Comment
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.SpringTestSupport
import ru.org.linux.topic.Topic
import ru.org.linux.user.User
import scalikejdbc.*

import java.util.concurrent.{CountDownLatch, ConcurrentLinkedQueue, TimeUnit}
import scala.jdk.CollectionConverters.*

/** Регрессионный тест TOCTOU лимита реакций (5 за 10 минут): параллельные транзакции одного автора сериализуются
  * advisory-блокировкой внутри checkRateLimit, поэтому burst из BurstSize конкурентных реакций по разным целям даёт
  * ровно столько успешных вставок, сколько осталось до лимита (ReactionsLimit минус baseline занятых слотов окна),
  * остальные — ReactionRateLimitException.
  *
  * Намеренно без TransactionalTestSupport: воркеры выполняются в собственных транзакциях и коммитят по-настоящему
  * (иначе невозможно воспроизвести гонку), поэтому состояние БД восстанавливается явно.
  */
@ContextConfiguration(classes = Array(classOf[ReactionDaoIntegrationTestConfiguration]))
class ReactionRateLimitConcurrencyIntegrationTest extends FunSuite with SpringTestSupport:

  @Autowired
  var reactionDao: ReactionDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private val TestUserId = 1
  private val BurstSize = ReactionService.ReactionsLimit * 2

  private def mockComment(id: Int, topicId: Int): Comment =
    val comment = mock(classOf[Comment])
    when(comment.id).thenReturn(id)
    when(comment.topicId).thenReturn(topicId)
    comment

  private def mockTopic(id: Int): Topic =
    val topic = mock(classOf[Topic])
    when(topic.id).thenReturn(id)
    topic

  private def mockUser(id: Int): User =
    val user = mock(classOf[User])
    when(user.id).thenReturn(id)
    user

  test("parallelBurstIsCappedByRateLimit"):
    // цели выбираем без существующих лог-строк пользователя: upsert по совпавшей цели не добавляет row и
    // «возвращает» слот лимита, делая число успехов зависящим от порядка выполнения воркеров
    val commentTargets = springDB.run:
      sql"""select c.id, c.topic from comments c where not c.deleted and not exists
            (select 1 from reactions_log r where r.origin_user = $TestUserId and r.topic_id = c.topic and r.comment_id = c.id)
            order by c.id limit ${BurstSize / 2}""".map(rs => (rs.int("id"), rs.int("topic"))).list.apply()

    val topicTargets = springDB.run:
      sql"""select t.id from topics t where not t.deleted and not exists
            (select 1 from reactions_log r where r.origin_user = $TestUserId and r.topic_id = t.id and r.comment_id is null)
            order by t.id limit ${BurstSize / 2}""".map(rs => rs.int("id")).list.apply()

    assume(commentTargets.size >= BurstSize / 2, "not enough comments in test db")
    assume(topicTargets.size >= BurstSize / 2, "not enough topics in test db")

    def commentReactions(id: Int): String = springDB.run:
      sql"select reactions from comments where id = $id".map(rs => rs.string("reactions")).single.apply().get

    def topicReactions(id: Int): String = springDB.run:
      sql"select reactions from topics where id = $id".map(rs => rs.string("reactions")).single.apply().get

    val originalComments = commentTargets.map(c => c._1 -> commentReactions(c._1))
    val originalTopics = topicTargets.map(t => t -> topicReactions(t))

    val originalLog = springDB.run:
      sql"select topic_id, comment_id, reaction, set_date from reactions_log where origin_user = $TestUserId"
        .map(rs => (rs.int("topic_id"), rs.intOpt("comment_id"), rs.string("reaction"), rs.timestamp("set_date")))
        .list
        .apply()

    def restore(): Unit = springDB.run:
      sql"delete from reactions_log where origin_user = $TestUserId".update.apply()
      originalLog.foreach { case (topicId, commentId, reaction, setDate) =>
        sql"""insert into reactions_log (origin_user, topic_id, comment_id, reaction, set_date)
              values ($TestUserId, $topicId, $commentId, $reaction, $setDate)""".update.apply()
      }
      originalComments.foreach { case (id, r) =>
        sql"update comments set reactions = $r::jsonb where id = $id".update.apply()
      }
      originalTopics.foreach { case (id, r) =>
        sql"update topics set reactions = $r::jsonb where id = $id".update.apply()
      }
    end restore

    // предочистка: восстановленное исходное состояние (могут остаться свежие лог-строки — напр., после
    // упавшего прогона; baseline ниже учитывает занятые ими слоты окна лимита)
    restore()

    val user = mockUser(TestUserId)

    val baseline = springDB.localTx {
      reactionDao.recentReactionCount(user)
    }
    val expectedSuccesses = math.max(0, ReactionService.ReactionsLimit - baseline)

    val results = new ConcurrentLinkedQueue[Either[Throwable, Int]]()
    val startGate = new CountDownLatch(1)
    val doneGate = new CountDownLatch(BurstSize)

    // прогрев пула: добиваемся BurstSize одновременно занятых соединений (minimumIdle=1, пул растёт лениво —
    // иначе создание соединений сериализует первых воркеров и гонка не воспроизводится)
    val warmupHeld = new CountDownLatch(BurstSize)
    val warmupRelease = new CountDownLatch(1)
    val warmupThreads = (1 to BurstSize).map { _ =>
      new Thread(() =>
        springDB.run {
          warmupHeld.countDown()
          warmupRelease.await()
        })
    }
    warmupThreads.foreach(_.start())
    assert(warmupHeld.await(60, TimeUnit.SECONDS), "pool warmup stalled (maximumPoolSize < BurstSize?)")
    warmupRelease.countDown()
    warmupThreads.foreach(_.join(10_000))

    val jobs: Seq[() => Int] =
      commentTargets.map { case (commentId, topicId) =>
        () =>
          springDB.localTx {
            reactionDao.checkRateLimit(user)
            reactionDao.setCommentReaction(mockComment(commentId, topicId), user, "\uD83D\uDC4D", set = true)
          }
      } ++
        topicTargets.map { topicId => () =>
          springDB.localTx {
            reactionDao.checkRateLimit(user)
            reactionDao.setTopicReaction(mockTopic(topicId), user, "\uD83D\uDC4D", set = true)
          }
        }

    def worker(job: () => Int): Runnable =
      () =>
        startGate.await()
        try
          results.add(Right(job()))
        catch
          case e: Throwable =>
            results.add(Left(e))
        finally
          doneGate.countDown()
        ()

    val threads = jobs.map(job => new Thread(worker(job)))

    try
      threads.foreach(_.start())
      startGate.countDown()
      assert(doneGate.await(60, TimeUnit.SECONDS), "workers did not finish in time")
      threads.foreach(_.join(10_000))

      val outcomes = results.asScala.toSeq
      assertEquals(outcomes.size, BurstSize)
      assertEquals(outcomes.count(_.isRight), expectedSuccesses, s"baseline=$baseline outcomes: $outcomes")

      val failures = outcomes.collect { case Left(e) =>
        e
      }
      assertEquals(failures.size, BurstSize - expectedSuccesses)
      failures.foreach { e =>
        assert(e.isInstanceOf[ReactionRateLimitException], s"unexpected failure: $e")
      }
    finally
      restore()

end ReactionRateLimitConcurrencyIntegrationTest
