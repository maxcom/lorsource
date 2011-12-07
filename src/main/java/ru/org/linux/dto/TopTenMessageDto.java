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

package ru.org.linux.dto;

import java.io.Serializable;
import java.sql.Timestamp;

public class TopTenMessageDto implements  Serializable{
  private String url;
  private Timestamp lastmod;
  private String title;
  private Integer pages;
  private Integer answers;
  private static final long serialVersionUID = 166352344159392938L;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Timestamp getLastmod() {
    return lastmod;
  }

  public void setLastmod(Timestamp lastmod) {
    this.lastmod = lastmod;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Integer getPages() {
    return pages;
  }

  public void setPages(Integer pages) {
    this.pages = pages;
  }

  public Integer getAnswers() {
    return answers;
  }

  public void setAnswers(Integer answers) {
    this.answers = answers;
  }
}