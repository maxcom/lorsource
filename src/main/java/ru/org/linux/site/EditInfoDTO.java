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

import ru.org.linux.spring.dao.TagDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

public class EditInfoDTO {
  private int id;
  private int msgid;
  private int editor;
  private String oldmessage;
  private Timestamp editdate;
  private String oldtitle;
  private String oldtags;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getMsgid() {
    return msgid;
  }

  public void setMsgid(int msgid) {
    this.msgid = msgid;
  }

  public int getEditor() {
    return editor;
  }

  public void setEditor(int editor) {
    this.editor = editor;
  }

  public String getOldmessage() {
    return oldmessage;
  }

  public void setOldmessage(String oldmessage) {
    this.oldmessage = oldmessage;
  }

  public Timestamp getEditdate() {
    return editdate;
  }

  public void setEditdate(Timestamp editdate) {
    this.editdate = editdate;
  }

  public String getOldtitle() {
    return oldtitle;
  }

  public void setOldtitle(String oldtitle) {
    this.oldtitle = oldtitle;
  }

  public String getOldtags() {
    return oldtags;
  }

  public void setOldtags(String oldtags) {
    this.oldtags = oldtags;
  }

  public static EditInfoDTO createFromMessage(Connection db, Message message) throws SQLException {
    EditInfoDTO current = new EditInfoDTO();

    current.setOldmessage(message.getMessage());
    current.setEditdate(message.getPostdate());
    current.setEditor(message.getUid());
    current.setMsgid(message.getMessageId());
    current.setOldtags(TagDao.toString(TagDao.getMessageTags(db, message.getMessageId())));

    return current;
  }
}
