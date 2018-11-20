/*
 * Copyright 1998-2016 Linux.org.ru
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

package ru.org.linux.topic;

import java.util.Map;

public class EditTopicRequest {
  private String url;
  private String linktext;
  private String title;
  private String msg;
  private String mode;
  private Boolean minor;
  private int bonus = 3;
  private String tags;

  private Map<Integer, String> poll;
  private Map<Integer, Integer> editorBonus;
  private String[] newPoll = new String[3];
  private boolean multiselect;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getLinktext() {
    return linktext;
  }

  public void setLinktext(String linktext) {
    this.linktext = linktext;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

  public Boolean getMinor() {
    return minor;
  }

  public void setMinor(Boolean minor) {
    this.minor = minor;
  }

  public int getBonus() {
    return bonus;
  }

  public void setBonus(int bonus) {
    this.bonus = bonus;
  }

  public String getTags() {
    return tags;
  }

  public void setTags(String tags) {
    this.tags = tags;
  }

  public Map<Integer, String> getPoll() {
    return poll;
  }

  public void setPoll(Map<Integer, String> poll) {
    this.poll = poll;
  }

  public String[] getNewPoll() {
    return newPoll;
  }

  public void setNewPoll(String[] newPoll) {
    this.newPoll = newPoll;
  }

  public boolean isMultiselect() {
    return multiselect;
  }

  public void setMultiselect(boolean multiselect) {
    this.multiselect = multiselect;
  }

  public Map<Integer, Integer> getEditorBonus() {
    return editorBonus;
  }

  public void setEditorBonus(Map<Integer, Integer> editorBonus) {
    this.editorBonus = editorBonus;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }
}
