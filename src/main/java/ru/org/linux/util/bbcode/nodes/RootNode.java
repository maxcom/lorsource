/*
 * Copyright 1998-2014 Linux.org.ru
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

/*
 * Copyright (c) 2005-2006, Luke Plant
 * All rights reserved.
 * E-mail: <L.Plant.98@cantab.net>
 * Web: http://lukeplant.me.uk/
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *
 *      * The name of Luke Plant may not be used to endorse or promote
 *        products derived from this software without specific prior
 *        written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Rewrite with Java language and modified for lorsource by Ildar Hizbulin 2011
 * E-mail: <hizel@vyborg.ru>
 */

package ru.org.linux.util.bbcode.nodes;

import org.apache.commons.httpclient.URI;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.util.bbcode.ParserParameters;
import ru.org.linux.util.bbcode.ParserParameters.CutType;
import ru.org.linux.util.formatter.ToHtmlFormatter;

import java.util.HashSet;
import java.util.Set;

/**
 * Корневой узел дерева разбора LORCODE, а также все параметры для разбора
 */
public class RootNode extends Node {
  private int cutCount;
  //
  private CutType cutType;
  private URI cutURI;
  private UserDao userDao;
  private ToHtmlFormatter toHtmlFormatter;
  private final Set<User> replier;
  private boolean secure;
  private boolean rss;
  private boolean nofollow = false;

  public RootNode(ParserParameters parserParameters) {
    super(parserParameters);
    cutCount = -1;
    cutType = CutType.INCOMMENT;
    replier = new HashSet<>();
    secure = false;
  }

  public URI getCutURI() {
    return cutURI;
  }

  public void setCutURI(URI cutURI) {
    this.cutURI = cutURI;
  }

  public ToHtmlFormatter getToHtmlFormatter() {
    return toHtmlFormatter;
  }

  public void setToHtmlFormatter(ToHtmlFormatter toHtmlFormatter) {
    this.toHtmlFormatter = toHtmlFormatter;
  }

  public UserDao getUserDao() {
    return userDao;
  }

  public void setUserDao(UserDao userDao) {
    this.userDao = userDao;
  }


  public boolean isSecure() {
    return secure;
  }

  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  public boolean isRss() {
    return rss;
  }

  public void setRss(boolean rss) {
    this.rss = rss;
  }

  public boolean isNofollow() {
    return nofollow;
  }

  public void setNofollow(boolean nofollow) {
    this.nofollow = nofollow;
  }

  public void addReplier(User nick) {
    replier.add(nick);
  }

  public Set<User> getReplier() {
    return replier;
  }

  public void setCommentCutOptions() {
    cutType = CutType.INCOMMENT;
  }

  public void setMaximizedTopicCutOptions() {
    cutType = CutType.INTOPIC_MAXIMIZED;
  }

  public void setMinimizedTopicCutOptions(URI cutURI) {
    cutType = CutType.INTOPIC_MINIMIZED;
    this.cutURI = cutURI;
  }

  public boolean isComment() {
    return cutType == CutType.INCOMMENT;
  }

  public boolean isTopicMinimized() {
    return cutType == CutType.INTOPIC_MINIMIZED;
  }

  public boolean isTopicMaximized() {
    return cutType == CutType.INTOPIC_MAXIMIZED;
  }

  @Override
  public String renderXHtml() {
    return renderChildrenXHtml();
  }

  @Override
  public boolean allows(String tagname) {
    return parserParameters.getBlockLevelTags().contains(tagname);
  }

  @Override
  public String renderBBCode() {
    return renderChildrenBBCode();
  }

  @Override
  public String renderOg() {
    return renderChildrenOg();
  }

  public int getCutCount() {
    cutCount += 1;
    return cutCount;
  }
}
