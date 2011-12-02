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

package ru.org.linux.spring;

import ru.org.linux.dto.UserDto;
import ru.org.linux.site.Section;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Элемент списка уведомлений
 */
public class RepliesListItem implements Serializable {

  public enum EventType {
    REPLY, DEL, WATCH, OTHER, REF
  }

  private final int cid;
  private final UserDto cAuthor;
  private final Timestamp cDate;
  private final String messageText;
  private final String groupTitle;
  private final String groupUrlName;
  private final String sectionTitle;
  private final int sectionId;
  private static final long serialVersionUID = -8433869244309809050L;
  private final String subj;
  private final Timestamp lastmod;
  private final int msgid;
  private final EventType type;
  private final String eventMessage;
  private final Timestamp eventDate;

  public RepliesListItem(int cid, UserDto cAuthor, Timestamp cDate, String messageText, String groupTitle,
                         String groupUrlName, String sectionTitle, int sectionId, String subj,
                         Timestamp lastmod, int msgid, EventType type, String eventMessage,
                         Timestamp eventDate) {
    this.cid = cid;
    this.cAuthor = cAuthor;
    this.cDate = cDate;
    this.messageText = messageText;
    this.groupTitle = groupTitle;
    this.groupUrlName = groupUrlName;
    this.sectionTitle = sectionTitle;
    this.sectionId = sectionId;
    this.subj = subj;
    this.lastmod = lastmod;
    this.msgid = msgid;
    this.type = type;
    this.eventMessage = eventMessage;
    this.eventDate = eventDate;
  }

  public int getCid() {
    return cid;
  }

  public UserDto getCommentAuthor() {
    return cAuthor;
  }

  public Timestamp getCommentDate() {
    return cDate;
  }

  public String getMessageText() {
    return messageText;
  }

  public String getNick() {
    return cAuthor.getNick();
  }

  public String getGroupTitle() {
    return groupTitle;
  }

  public String getSectionTitle() {
    return sectionTitle;
  }

  public String getGroupUrl() {
    return Section.getSectionLink(sectionId) + groupUrlName + '/';
  }

  public String getSectionUrl() {
    return Section.getSectionLink(sectionId);
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

  public EventType getType() {
    return type;
  }

  public String getEventMessage() {
    return eventMessage;
  }

  public Timestamp getEventDate() {
    return eventDate;
  }
}

