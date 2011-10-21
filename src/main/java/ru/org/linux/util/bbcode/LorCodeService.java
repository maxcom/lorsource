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


package ru.org.linux.util.bbcode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.spring.Configuration;
import ru.org.linux.spring.dao.MessageDao;
import ru.org.linux.spring.dao.UserDao;
import ru.org.linux.util.bbcode.nodes.RootNode;

@Service
public class LorCodeService {
  private static final Parser defaultParser = new Parser(new DefaultParserParameters());

  @Autowired
  UserDao userDao;

  @Autowired
  Configuration configuration;

  @Autowired
  MessageDao messageDao;

  public String parser(String text) {
    return parser(text, true, true, "", false);
  }

  public String parser(String text, boolean secure) {
    return parser(text, true, true, "", secure);
  }

  public String parser(String text, boolean renderCut, boolean cleanCut, String cutUrl, boolean secure) {
    return defaultParser.parseRoot(prepareRootNode(renderCut, cleanCut, cutUrl, secure), text).renderXHtml();
  }

  public ParserResult parserWithReplies(String text) {
    return parserWithReplies(text, true, true, "", false);
  }

  public ParserResult parserWithReplies(String text, boolean secure) {
    return parserWithReplies(text, true, true, "", secure);
  }

  public ParserResult parserWithReplies(String text,  boolean renderCut, boolean cleanCut, String cutUrl, boolean secure) {
    RootNode parsedRootNode = defaultParser.parseRoot(prepareRootNode(renderCut, cleanCut, cutUrl, secure), text);
    return new ParserResult(parsedRootNode.renderXHtml(), parsedRootNode.getReplier());
  }

  private RootNode prepareRootNode(boolean renderCut, boolean cleanCut, String cutUrl, boolean secure) {
    RootNode rootNode = defaultParser.getRootNode();
    rootNode.setRenderOptions(renderCut, cleanCut, cutUrl);
    rootNode.setUserDao(userDao);
    rootNode.setMessageDao(messageDao);
    rootNode.setConfiguration(configuration);
    rootNode.setSecure(secure);
    return rootNode;
  }
}
