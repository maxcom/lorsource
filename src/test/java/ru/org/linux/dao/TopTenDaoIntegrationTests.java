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
import ru.org.linux.dto.TopTenMessageDto;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("commonDAO-context.xml")
public class TopTenDaoIntegrationTests {
  @Autowired
  TopTenDao topTenDao;

  @Test
  public void topTenTest() {
    List<TopTenMessageDto> tenMessageDtos = topTenDao.getMessages();
    // TODO: не знаю, как дальше тестировать. Сообщений TOP10 может быть а может и не быть.
    // пожалуй, признак успешного теста - ничего не свалилось в exception :)
  }

}
