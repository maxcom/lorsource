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

package ru.org.linux.site;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PreparedMessage {
  private final Message message;
  private final User author;
  private final DeleteInfo deleteInfo;
  private final User deleteUser;
  private final String processedMessage;
  private final PreparedPoll poll;
  private final User commiter;
  private final Tags tags;
  private final Group group;

  private final EditInfoDTO lastEditInfo;
  private final User lastEditor;
  private final int editCount;

  private final String userAgent;

  public PreparedMessage(Connection db, Message message, boolean includeCut) throws SQLException {
    this(db, message, new Tags(db, message.getId()), includeCut);
  }

  public PreparedMessage(Connection db, Message message, Tags tags) throws SQLException {
    this(db, message, tags, true);
  }

  public PreparedMessage(Connection db, Message message, Tags tags, boolean includeCut) throws SQLException {
    try {
      this.message = message;

      group = new Group(db, message.getGroupId());

      author = User.getUserCached(db, message.getUid());

      if (message.isDeleted()) {
        deleteInfo = DeleteInfo.getDeleteInfo(db, message.getId());

        if (deleteInfo!=null) {
          deleteUser = User.getUserCached(db, deleteInfo.getUserid());
        } else {
          deleteUser = null;
        }
      } else {
        deleteInfo = null;
        deleteUser = null;
      }

      if (message.isVotePoll()) {
        poll = new PreparedPoll(db, Poll.getPollByTopic(db, message.getId()));
      } else {
        poll = null;
      }

      if (message.getCommitby()!=0) {
        commiter = User.getUserCached(db, message.getCommitby());
      } else {
        commiter = null;
      }

      List<EditInfoDTO> editInfo = message.loadEditInfo(db);
      if (!editInfo.isEmpty()) {
        lastEditInfo = editInfo.get(0);
        lastEditor = User.getUserCached(db, lastEditInfo.getEditor());
        editCount = editInfo.size();
      } else {
        lastEditInfo = null;
        lastEditor = null;
        editCount = 0;
      }

      processedMessage = message.getProcessedMessage(db, includeCut);

      userAgent = loadUserAgent(db, message.getUserAgent());

      this.tags=tags;
    } catch (BadGroupException e) {
      throw new RuntimeException(e);
    } catch (UserNotFoundException e) {
      throw new RuntimeException(e);
    } catch (PollNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private static String loadUserAgent(Connection db, int id) throws SQLException {
    if (id==0) {
      return null;
    }

    PreparedStatement pst = db.prepareStatement("SELECT name FROM user_agents WHERE id=?");

    try {
      pst.setInt(1, id);
      ResultSet rs = pst.executeQuery();
      if (rs.next()) {
        return rs.getString(1);
      } else {
        return null;
      }
    } finally {
      pst.close();
    }
  }

  public Message getMessage() {
    return message;
  }

  public User getAuthor() {
    return author;
  }

  public DeleteInfo getDeleteInfo() {
    return deleteInfo;
  }

  public User getDeleteUser() {
    return deleteUser;
  }

  public String getProcessedMessage() {
    return processedMessage;
  }

  public PreparedPoll getPoll() {
    return poll;
  }

  public User getCommiter() {
    return commiter;
  }

  public EditInfoDTO getLastEditInfo() {
    return lastEditInfo;
  }

  public User getLastEditor() {
    return lastEditor;
  }

  public int getEditCount() {
    return editCount;
  }

  public int getId() {
    return message.getId();
  }

  public String getUserAgent() {
    return userAgent;
  }

  public Tags getTags() {
    return tags;
  }

  public Group getGroup() {
    return group;
  }
}
