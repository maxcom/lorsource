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

import ru.org.linux.site.User;
import ru.org.linux.spring.dao.UserDao;
import ru.org.linux.util.bbcode.ParserParameters;
import ru.org.linux.util.formatter.ToHtmlFormatter;

import java.util.HashSet;
import java.util.Set;

/**
 * Корневой узел дерева разбора LORCODE, а также все параметры для разбора
 */
public class RootNode extends Node {
  private int cutCount;
  private boolean renderCut;
  private boolean cleanCut;
  private String cutUrl;
  private UserDao userDao;
  private ToHtmlFormatter toHtmlFormatter;
  private Set<User> replier;
  private boolean secure;

  public RootNode(ParserParameters parserParameters) {
    super(parserParameters);
    cutCount = -1;
    renderCut = true;
    cleanCut = true;
    cutUrl = "";
    replier = new HashSet<User>();
    secure = false;
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

  public void addReplier(User nick) {
    replier.add(nick);
  }

  public Set<User> getReplier() {
    return replier;
  }

  public void setRenderOptions(boolean renderCut, boolean cleanCut, String cutUrl) {
    this.renderCut = renderCut;
    this.cleanCut = cleanCut;
    this.cutUrl = cutUrl;
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

  public int getCutCount() {
    cutCount += 1;
    return cutCount;
  }

  public boolean isRenderCut() {
    return renderCut;
  }

  public boolean isCleanCut() {
    return cleanCut;
  }

  public String getCutUrl() {
    return cutUrl;
  }
}
