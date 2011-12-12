/*
 * Copyright 1998-2011 Linux.org.ru
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

package ru.org.linux.message;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.org.linux.gallery.Screenshot;
import ru.org.linux.group.Group;
import ru.org.linux.user.User;
import ru.org.linux.site.Template;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:commonDAO-context.xml")
public class MessageDaoIntegrationTest {
  @Autowired
  MessageDao messageDao;

  @Test
  public void MessageAddTest()
    throws Exception {

    /*
      TODO: пока реализован только тест на добавление новой темы,
      потому что в MessageDao чёрт ногу сломит: там и template, и request,
      и автовайреды других DAO... получается полусервис-полуконтроллер-полуDAO.
    */
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("User-Agent")).thenReturn("some user agent");
    when(request.getRemoteAddr()).thenReturn("1.2.3.4");
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);

    AddMessageRequest form = mock(AddMessageRequest.class);
    when(form.getTags()).thenReturn(null);

    Template tmpl = mock(Template.class);

    Group groupDto = mock(Group.class);
    when(groupDto.isPollPostAllowed()).thenReturn(false);
    when(groupDto.isImagePostAllowed()).thenReturn(false);
    when(groupDto.getId()).thenReturn(213);

    User user = mock(User.class);
    when(user.getId()).thenReturn(1);

    Screenshot scrn = mock(Screenshot.class);

    Message msg = mock(Message.class);
    when(msg.getTitle()).thenReturn("TITLE!");
    when(msg.getUrl()).thenReturn("http://1111111.fff.dd");
    when(msg.getLinktext()).thenReturn("linked text");
    when(msg.getGroupId()).thenReturn(213);
    when(msg.getMessage()).thenReturn("Message body");
    when(msg.isLorcode()).thenReturn(false);

    int messageId = messageDao.addMessage(
      request,
      form,
      tmpl,
      groupDto,
      user,
      scrn,
      msg,
      new HashSet<User>()
    );
  }

}
