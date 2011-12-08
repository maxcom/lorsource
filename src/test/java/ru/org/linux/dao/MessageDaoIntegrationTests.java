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

package ru.org.linux.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.org.linux.dto.GroupDto;
import ru.org.linux.dto.MessageDto;
import ru.org.linux.dto.UserDto;
import ru.org.linux.site.Screenshot;
import ru.org.linux.site.Template;
import ru.org.linux.spring.AddMessageRequest;

import javax.servlet.http.HttpServletRequest;

import java.util.HashSet;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("commonDAO-context.xml")
public class MessageDaoIntegrationTests {
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

    GroupDto groupDto = mock(GroupDto.class);
    when(groupDto.isPollPostAllowed()).thenReturn(false);
    when(groupDto.isImagePostAllowed()).thenReturn(false);
    when(groupDto.getId()).thenReturn(213);

    UserDto user = mock(UserDto.class);
    when(user.getId()).thenReturn(1);

    Screenshot scrn = mock(Screenshot.class);

    MessageDto msg = mock(MessageDto.class);
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
      new HashSet<UserDto>()
    );
  }

}
