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
import ru.org.linux.site.User;
import ru.org.linux.spring.dao.UserDao;
import ru.org.linux.util.bbcode.nodes.RootNode;

import java.util.Set;

@Service
public class LorCodeService {
  private static final Parser defaultParser = new Parser(new DefaultParserParameters());

  @Autowired
  UserDao userDao;

  public String parser(String lorcode) {
    return defaultParser.parse(lorcode).renderXHtml();
  }

  public String parser(String lorcode, boolean renderCut, boolean cleanCut, String cutUrl) {
    return defaultParser.parse(lorcode, renderCut, cleanCut, cutUrl, userDao).renderXHtml();
  }

  public ParserResult parserWithReplies(String lorcode) {
    RootNode rootNode = defaultParser.parse(lorcode);
    String html = rootNode.renderXHtml();
    Set<User> replier = rootNode.getReplier();
    return new ParserResult(html, replier);
  }

  public ParserResult parserWithReplies(String lorcode,  boolean renderCut, boolean cleanCut, String cutUrl) {
    RootNode rootNode = defaultParser.parse(lorcode, renderCut, cleanCut, cutUrl, userDao);
    String html = rootNode.renderXHtml();
    Set<User> replier = rootNode.getReplier();
    return new ParserResult(html, replier);
  }
}
