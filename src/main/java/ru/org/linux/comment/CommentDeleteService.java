/*
 * Copyright 1998-2024 Linux.org.ru
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

package ru.org.linux.comment;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.site.DeleteInfo;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.topic.TopicService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserEventService;

import java.sql.Timestamp;
import java.util.*;

@Service
public class CommentDeleteService {
  private final CommentDao commentDao;
  private final TopicService topicService;
  private final UserDao userDao;
  private final UserEventService userEventService;
  private final DeleteInfoDao deleteInfoDao;
  private final CommentReadService commentService;
  private final TopicDao topicDao;

  public CommentDeleteService(CommentDao commentDao, TopicService topicService, UserDao userDao,
                              UserEventService userEventService, DeleteInfoDao deleteInfoDao,
                              CommentReadService commentService, TopicDao topicDao) {
    this.commentDao = commentDao;
    this.topicService = topicService;
    this.userDao = userDao;
    this.userEventService = userEventService;
    this.deleteInfoDao = deleteInfoDao;
    this.commentService = commentService;
    this.topicDao = topicDao;
  }

  /**
   * Удаляем коментарий, если на комментарий есть ответы - генерируем исключение
   *
   * @param msgid      id удаляемого сообщения
   * @param reason     причина удаления
   * @param user       модератор который удаляет
   * @param scoreBonus сколько шкворца снять
   * @param checkForReply производить ли проверку на ответы
   * @throws ScriptErrorException генерируем исключение если на комментарий есть ответы
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public boolean deleteComment(int msgid, String reason, User user, int scoreBonus,
                               boolean checkForReply) throws ScriptErrorException {
    if (checkForReply && commentDao.getRepliesCount(msgid) != 0) {
      throw new ScriptErrorException("Нельзя удалить комментарий с ответами");
    }

    boolean deleted = doDeleteComment(msgid, reason, user, -scoreBonus);

    if (deleted) {
      if (scoreBonus != 0) {
        Comment comment = commentDao.getById(msgid);
        userDao.changeScore(comment.getUserid(), -scoreBonus);
      }
      commentDao.updateStatsAfterDelete(msgid, 1);
      userEventService.processCommentsDeleted(ImmutableList.of(msgid));
    }

    return deleted;
  }

  /**
   * Удалить комментарий.
   *
   * @param msgid      идентификационнай номер комментария
   * @param reason     причина удаления
   * @param user       пользователь, удаляющий комментарий
   * @param scoreBonus количество снятого скора
   * @return true если комментарий был удалён, иначе false
   */
  private boolean doDeleteComment(int msgid, String reason, User user, int scoreBonus) {
    boolean deleted = commentDao.deleteComment(msgid, reason, user);

    if (deleted) {
      deleteInfoDao.insert(msgid, user, reason, scoreBonus);
    }

    return deleted;
  }

  private boolean doDeleteComment(int msgid, String reason, User user) {
    return doDeleteComment(msgid, reason, user, 0);
  }

  /**
   * Удалить комментарий.
   *
   * @param comment    удаляемый комментарий
   * @param reason     причина удаления
   * @param user       пользователь, удаляющий комментарий
   * @param scoreBonus сколько снять скора у автора комментария
   * @return true если комментарий был удалён, иначе false
   */
  private boolean deleteComment(Comment comment, String reason, User user, int scoreBonus) {
    Preconditions.checkArgument(scoreBonus<=0, "Score bonus on delete must be non-positive");

    boolean del = commentDao.deleteComment(comment.getId(), reason, user);

    if (del && scoreBonus!=0) {
      userDao.changeScore(comment.getUserid(), scoreBonus);
    }

    return del;
  }

  /**
   * Удаление ответов на комментарии.
   *
   * @param comment удаляемый комментарий
   * @param user   пользователь, удаляющий комментарий
   * @param scoreBonus  сколько снять скора у автора комментария
   * @return список идентификационных номеров удалённых комментариев
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public List<Integer> deleteWithReplys(Topic topic, Comment comment, String reason, User user, int scoreBonus) {
    CommentList commentList = commentService.getCommentList(topic, false);

    CommentNode node = commentList.getNode(comment.getId());

    List<CommentAndDepth> replys = getAllReplys(node, 0);

    List<Integer> deleted = deleteReplys(comment, reason, replys, user, -scoreBonus);

    userEventService.processCommentsDeleted(deleted);

    return deleted;
  }

  /**
     * Удалить рекурсивно ответы на комментарий
     *
     * @param replys список ответов
     * @param user  пользователь, удаляющий комментарий
     * @param rootBonus сколько снять скора у автора корневого комментария
     * @return список идентификационных номеров удалённых комментариев
     */
  private List<Integer> deleteReplys(Comment root, String rootReason, List<CommentAndDepth> replys, User user, int rootBonus) {
    boolean score = rootBonus < -2;

    List<Integer> deleted = new ArrayList<>(replys.size());
    List<DeleteInfoDao.InsertDeleteInfo> deleteInfos = new ArrayList<>(replys.size());

    for (CommentAndDepth cur : replys) {
      Comment child = cur.comment();

      DeleteInfoDao.InsertDeleteInfo info = cur.deleteInfo(score, user);

      boolean del = deleteComment(child, info.getReason(), user, info.getBonus());

      if (del) {
        deleteInfos.add(info);
        deleted.add(child.getId());
      }
    }

    boolean deletedMain = deleteComment(root, rootReason, user, rootBonus);

    if (deletedMain) {
      deleteInfos.add(new DeleteInfoDao.InsertDeleteInfo(root.getId(), rootReason, rootBonus, user.getId()));
      deleted.add(root.getId());
    }

    deleteInfoDao.insert(deleteInfos);

    if (!deleted.isEmpty()) {
      commentDao.updateStatsAfterDelete(root.getId(), deleted.size());
    }

    return deleted;
  }

  /**
   * Удаление топиков, сообщений по ip и за определнный период времени, те комментарии на которые существуют ответы пропускаем
   *
   * @param ip        ip для которых удаляем сообщения (не проверяется на корректность)
   * @param timeDelta врменной промежуток удаления (не проверяется на корректность)
   * @param moderator экзекутор-можератор
   * @param reason    причина удаления, которая будет вписана для удаляемых топиков
   * @return список id удаленных сообщений
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public DeleteCommentResult deleteCommentsByIPAddress(
    String ip,
    Timestamp timeDelta,
    final User moderator,
    final String reason)
  {
    List<Integer> deletedTopics = topicService.deleteByIPAddress(ip, timeDelta, moderator, reason);

    List<Integer> skippedComments = new ArrayList<>();

    // Удаляем комментарии если на них нет ответа
    List<Integer> commentIds = commentDao.getCommentsByIPAddressForUpdate(ip, timeDelta);

    List<Integer> deletedCommentIds = new ArrayList<>();

    for (int msgid : commentIds) {
      if (commentDao.getRepliesCount(msgid) == 0) {
        if (doDeleteComment(msgid, reason, moderator)) {
          deletedCommentIds.add(msgid);
        }
      } else {
        skippedComments.add(msgid);
      }
    }

    for (int msgid : deletedCommentIds) {
      commentDao.updateStatsAfterDelete(msgid, 1);
    }

    userEventService.processCommentsDeleted(deletedCommentIds);

    return new DeleteCommentResult(deletedTopics, deletedCommentIds, skippedComments);
  }

  /**
   * Блокировка и массивное удаление всех топиков и комментариев пользователя со всеми ответами на комментарии
   *
   * @param user      пользователь для экзекуции
   * @param moderator экзекутор-модератор
   * @param reason    прична блокировки
   * @return список удаленных комментариев
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public DeleteCommentResult deleteAllCommentsAndBlock(User user, final User moderator, String reason) {
    userDao.block(user, moderator, reason);

    List<Integer> deletedTopicIds = topicService.deleteAllByUser(user, moderator);

    final List<Integer> deletedCommentIds = new ArrayList<>();

    // Удаляем все комментарии
    List<Integer> commentIds = commentDao.getAllByUserForUpdate(user);
    List<Integer> skippedComments = new ArrayList<>();

    for (int msgid : commentIds) {
      if (commentDao.getRepliesCount(msgid) == 0) {
        doDeleteComment(msgid, "Блокировка пользователя с удалением сообщений", moderator);
        commentDao.updateStatsAfterDelete(msgid, 1);
        deletedCommentIds.add(msgid);
      } else {
        skippedComments.add(msgid);
      }
    }

    userEventService.processCommentsDeleted(deletedCommentIds);

    return new DeleteCommentResult(deletedTopicIds, deletedCommentIds, skippedComments);
  }

  private static List<CommentAndDepth> getAllReplys(CommentNode node, int depth) {
    List<CommentAndDepth> replys = new LinkedList<>();

    for (CommentNode r : node.childs()) {
      replys.addAll(getAllReplys(r, depth + 1));
      replys.add(new CommentAndDepth(r.getComment(), depth));
    }

    return replys;
  }

  private record CommentAndDepth(Comment comment, int depth) {

    private DeleteInfoDao.InsertDeleteInfo deleteInfo(boolean score, User user) {
        int bonus;
        String reason;

        if (score) {
          switch (depth) {
            case 0 -> {
              reason = "7.1 Ответ на некорректное сообщение (авто, уровень 0)";
              bonus = -2;
            }
            case 1 -> {
              reason = "7.1 Ответ на некорректное сообщение (авто, уровень 1)";
              bonus = -1;
            }
            default -> {
              reason = "7.1 Ответ на некорректное сообщение (авто, уровень >1)";
              bonus = 0;
            }
          }
        } else {
          reason = "7.1 Ответ на некорректное сообщение (авто)";
          bonus = 0;
        }

        return new DeleteInfoDao.InsertDeleteInfo(comment.getId(), reason, bonus, user.getId());
      }
    }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void undeleteComment(Comment comment) {
    DeleteInfo deleteInfo = deleteInfoDao.getDeleteInfo(comment.getId(), true);

    if (deleteInfo!=null && deleteInfo.getBonus()!=0) {
      userDao.changeScore(comment.getUserid(), -deleteInfo.getBonus());
    }

    commentDao.undeleteComment(comment);
    deleteInfoDao.delete(comment.getId());
    topicDao.updateLastmod(comment.getTopicId(), false);
  }
}
