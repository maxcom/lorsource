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

package ru.org.linux.comment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import ru.org.linux.auth.CaptchaService;
import ru.org.linux.auth.FloodProtector;
import ru.org.linux.auth.IPBlockDao;
import ru.org.linux.auth.IPBlockInfo;
import ru.org.linux.edithistory.EditHistoryService;
import ru.org.linux.msg.MsgDao;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.test.Users;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.user.IgnoreListDao;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserEventService;
import ru.org.linux.util.bbcode.LorCodeService;
import ru.org.linux.util.formatter.ToLorCodeFormatter;
import ru.org.linux.util.formatter.ToLorCodeTexFormatter;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.*;


/**
 * TODO: Тест отключен и не написан, потому что есть циклическая зависимость
 * LorCodeService -> TopicDao -> UserEventService -> LorCodeService
 * mock очень не любит такие зависимости. Тест должен быть дописан после упорядочивания зависимостей
 */


//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration("unit-tests-context.xml")
public class CommentServiceTest {

  @Autowired
  CommentService commentService;

  @Autowired
  private CommentDao commentDao;

  @Autowired
  private TopicDao messageDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private ToLorCodeFormatter toLorCodeFormatter;

  @Autowired
  private ToLorCodeTexFormatter toLorCodeTexFormatter;

  @Autowired
  private CaptchaService captcha;

  @Autowired
  private CommentPrepareService commentPrepareService;

  @Autowired
  private FloodProtector floodProtector;

  @Autowired
  private IPBlockDao ipBlockDao;

  @Autowired
  private LorCodeService lorCodeService;

  @Autowired
  private UserEventService userEventService;

  @Autowired
  private MsgDao msgbaseDao;

  @Autowired
  private SearchQueueSender searchQueueSender;

  @Autowired
  private IgnoreListDao ignoreListDao;

  @Autowired
  private EditHistoryService editHistoryService;

//  @Test
  public void checkPostData() throws Exception {
    CommentRequest commentRequest = new CommentRequest();
    User user = new User(Users.getUser1star());
    HttpServletRequest request = mock (HttpServletRequest.class);
    Errors errors = new BindException(commentRequest, "commentRequest");
    IPBlockInfo ipBlockInfo = new IPBlockInfo("127.0.0.1");

    commentService.checkPostData (commentRequest, user, ipBlockInfo, request, errors);
  }

}
