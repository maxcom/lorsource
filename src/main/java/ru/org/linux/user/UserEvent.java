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

package ru.org.linux.user;

import ru.org.linux.section.Section;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Элемент списка уведомлений
 */
public class UserEvent implements Serializable {

  private final int cid;
  private final int cAuthor;
  private final Timestamp cDate;
  private final String groupTitle;
  private final String groupUrlName;
  private final int sectionId;
  private static final long serialVersionUID = -8433869244309809050L;
  private final String subj;
  private final Timestamp lastmod;
  private final int msgid;
  private final UserEventFilterEnum type;
  private final String eventMessage;
  private final Timestamp eventDate;
  private final boolean unread;

  public UserEvent(int cid, int cAuthor, Timestamp cDate, String groupTitle,
                   String groupUrlName, int sectionId, String subj,
                   Timestamp lastmod, int msgid, UserEventFilterEnum type, String eventMessage,
                   Timestamp eventDate, boolean unread) {
    this.cid = cid;
    this.cAuthor = cAuthor;
    this.cDate = cDate;
    this.groupTitle = groupTitle;
    this.groupUrlName = groupUrlName;
    this.sectionId = sectionId;
    this.subj = subj;
    this.lastmod = lastmod;
    this.msgid = msgid;
    this.type = type;
    this.eventMessage = eventMessage;
    this.eventDate = eventDate;
    this.unread = unread;
  }
  
  public boolean isComment() {
    return cid>0;
  }

  public int getCid() {
    return cid;
  }

  public int getCommentAuthor() {
    return cAuthor;
  }

  public Timestamp getCommentDate() {
    return cDate;
  }

  public String getGroupTitle() {
    return groupTitle;
  }

  public String getGroupUrl() {
    return Section.getSectionLink(sectionId) + groupUrlName + '/';
  }

  public String getSubj() {
    return subj;
  }

  public Timestamp getLastmod() {
    return lastmod;
  }

  public int getMsgid() {
    return msgid;
  }

  public UserEventFilterEnum getType() {
    return type;
  }

  public String getEventMessage() {
    return eventMessage;
  }

  public Timestamp getEventDate() {
    return eventDate;
  }

  public boolean isUnread() {
    return unread;
  }
}

