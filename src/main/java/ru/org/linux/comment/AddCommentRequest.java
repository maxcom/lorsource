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

package ru.org.linux.comment;

import ru.org.linux.message.Message;
import ru.org.linux.site.User;

public class AddCommentRequest {
  private String preview;
  private String mode;
  private String msg;
  private Comment replyto;
  private String title;
  private Message topic;

  private User nick;
  private String password;

  public void setPreview(String preview) {
    this.preview = preview;
  }

  public String getPreview() {
    return preview;
  }

  public boolean isPreviewMode() {
    return preview!=null;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

  public Comment getReplyto() {
    return replyto;
  }

  public void setReplyto(Comment replyto) {
    this.replyto = replyto;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Message getTopic() {
    return topic;
  }

  public void setTopic(Message topic) {
    this.topic = topic;
  }

  public User getNick() {
    return nick;
  }

  public void setNick(User nick) {
    this.nick = nick;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}