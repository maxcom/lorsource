/*
 * Copyright 1998-2021 Linux.org.ru
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

package ru.org.linux.edithistory;

import ru.org.linux.poll.Poll;

import java.sql.Timestamp;

public class EditHistoryRecord {
  private int id;
  private int msgid;
  private int editor;
  private String oldmessage;
  private Timestamp editdate;
  private String oldtitle;
  private String oldtags;
  private String oldlinktext;
  private String oldurl;
  private EditHistoryObjectTypeEnum objectType;
  private Boolean oldminor;
  private Integer oldimage;
  private Poll oldPoll;

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

  public String getOldlinktext() {
    return oldlinktext;
  }

  public void setOldlinktext(String oldlinktext) {
    this.oldlinktext = oldlinktext;
  }

  public String getOldurl() {
    return oldurl;
  }

  public void setOldurl(String oldurl) {
    this.oldurl = oldurl;
  }

  public EditHistoryObjectTypeEnum getObjectType() {
    return objectType;
  }

  public void setObjectType(EditHistoryObjectTypeEnum objectType) {
    this.objectType = objectType;
  }

  public void setObjectType(String objectType) {
    this.objectType = EditHistoryObjectTypeEnum.valueOf(objectType);
  }

  public Boolean getOldminor() {
    return oldminor;
  }

  public void setOldminor(Boolean oldminor) {
    this.oldminor = oldminor;
  }

  public Integer getOldimage() {
    return oldimage;
  }

  public void setOldimage(Integer oldimage) {
    this.oldimage = oldimage;
  }

  public Poll getOldPoll() {
    return oldPoll;
  }

  public void setOldPoll(Poll oldPoll) {
    this.oldPoll = oldPoll;
  }
}
