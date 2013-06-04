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

package ru.org.linux.comment;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.user.User;
import ru.org.linux.util.StringUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public interface CommentDao {

  /**
   * Получить комментарий по id
   *
   * @param id id нужного комментария
   * @return нужный комментарий
   * @throws MessageNotFoundException при отсутствии сообщения
   */
  Comment getById
  (
    int id
  )
    throws MessageNotFoundException;

  /**
   * Список комментариев топика
   *
   * @param topicId     id топика
   * @param showDeleted вместе с удаленными
   * @return список комментариев топика
   */
  List<Comment> getCommentList
  (
    int topicId,
    boolean showDeleted
  );

  /**
   * Удалить комментарий.
   *
   * @param msgid      идентификационнай номер комментария
   * @param reason     причина удаления
   * @param user       пользователь, удаляющий комментарий
   * @param scoreBonus сколько снять скора у автора комментария
   * @return true если комментарий был удалён, иначе false
   */
  boolean deleteComment
  (
    int msgid,
    String reason,
    User user,
    int scoreBonus
  );

  /**
   * Обновляет статистику после удаления комментариев в одном топике.
   *
   * @param commentId идентификатор любого из удаленных комментариев (обычно корневой в цепочке)
   * @param count     количество удаленных комментариев
   */
  void updateStatsAfterDelete
  (
    int commentId,
    int count
  );

  /**
   * Удалить рекурсивно ответы на комментарий
   *
   * @param node  CommentNode удаляемого комментария
   * @param user  пользователь, удаляющий комментарий
   * @param score сколько снять скора у автора комментария
   * @return список идентификационных номеров удалённых комментариев
   */
  List<Integer> deleteReplys
  (
          CommentNode node,
          User user,
          boolean score
  );

  /**
   * Сколько ответов на комментарий
   *
   * @param msgid id комментария
   * @return число ответов на комментарий
   */
  int getReplaysCount
  (
    int msgid
  );

  /**
   * Массовое удаление комментариев пользователя со всеми ответами на комментарии.
   *
   * @param user      пользователь для экзекуции
   * @param moderator экзекутор-модератор
   * @return список удаленных комментариев
   */
  List<Integer> deleteAllByUser
  (
    User user,
    final User moderator
  );

  /**
   * Добавить новый комментарий.
   *
   *
   * @param comment данные комментария
   * @param message текст комментария
   * @param userAgent
   * @return идентификационный номер нового комментария
   * @throws MessageNotFoundException
   */
  int saveNewMessage
  (
          final Comment comment,
          String message,
          String userAgent);

  /**
   * Редактирование комментария.
   *
   * @param oldComment  данные старого комментария
   * @param newComment  данные нового комментария
   * @param commentBody текст нового комментария
   */
  void edit
  (
    final Comment oldComment,
    final Comment newComment,
    final String commentBody
  );

  /**
   * Обновить информацию о последнем редакторе комментария.
   *
   * @param id        идентификационный номер комментария
   * @param editorId  идентификационный номер редактора комментария
   * @param editDate  дата редактирования
   * @param editCount количество исправлений
   */
  void updateLatestEditorInfo
  (
    int id,
    int editorId,
    Date editDate,
    int editCount
  );

  /**
   * Получить список комментариев пользователя.
   *
   * @param userId идентификационный номер пользователя
   * @param limit  сколько записей должно быть в ответе
   * @param offset начиная с какой позиции выдать ответ
   * @return список комментариев пользователя
   */
  List<CommentsListItem> getUserComments
  (
    int userId,
    int limit,
    int offset
  );

  /**
   * Получить список последних удалённых комментариев пользователя.
   *
   * @param userId идентификационный номер пользователя
   * @return список удалённых комментариев пользователя
   */
   List<DeletedListItem> getDeletedComments
   (
     int userId
   );

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  List<Integer> getCommentsByIPAddressForUpdate(String ip, Timestamp timedelta);

  /**
   * DTO-класс, описывающий данные удалённого комментария
   */
  class DeletedListItem {
    private final String ptitle;
    private final String gtitle;
    private final int msgid;
    private final String title;
    private final String reason;
    private final Timestamp delDate;
    private final int bonus;
    private final int cid;

    public DeletedListItem(ResultSet rs) throws SQLException {
      ptitle = rs.getString("ptitle");
      gtitle = rs.getString("gtitle");
      msgid = rs.getInt("msgid");
      title = StringUtil.makeTitle(rs.getString("title"));
      reason = rs.getString("reason");
      delDate = rs.getTimestamp("deldate");
      bonus = rs.getInt("bonus");
      cid = rs.getInt("cid");
    }

    public String getPtitle() {
      return ptitle;
    }

    public String getGtitle() {
      return gtitle;
    }

    public int getMsgid() {
      return msgid;
    }

    public String getTitle() {
      return title;
    }

    public String getReason() {
      return reason;
    }

    public Timestamp getDelDate() {
      return delDate;
    }

    public int getBonus() {
      return bonus;
    }

    public int getCommentId() {
      return cid;
    }
  }

  class CommentsListItem {
    private String sectionTitle;
    private String groupTitle;
    private int topicId;
    private int commentId;
    private String title;
    private Timestamp postdate;

    public String getSectionTitle() {
      return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
      this.sectionTitle = sectionTitle;
    }

    public String getGroupTitle() {
      return groupTitle;
    }

    public void setGroupTitle(String groupTitle) {
      this.groupTitle = groupTitle;
    }

    public int getTopicId() {
      return topicId;
    }

    public void setTopicId(int topicId) {
      this.topicId = topicId;
    }

    public int getCommentId() {
      return commentId;
    }

    public void setCommentId(int commentId) {
      this.commentId = commentId;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public Timestamp getPostdate() {
      return postdate;
    }

    public void setPostdate(Timestamp postdate) {
      this.postdate = postdate;
    }


  }
}
