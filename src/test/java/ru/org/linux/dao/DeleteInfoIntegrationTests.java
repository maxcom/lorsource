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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.org.linux.dto.DeleteInfoDto;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("commonDAO-context.xml")
public class DeleteInfoIntegrationTests {
    @Autowired
    DeleteInfoDao deleteInfoDao;

    private final static int HARDCODED_MSGID=1946260;
    
    @Test
    public void deleteInfoTest() {
        DeleteInfoDto deleteInfoDto = deleteInfoDao.getDeleteInfo(HARDCODED_MSGID);
        Assert.assertEquals(
                "cavia_porcellus",
                deleteInfoDto.getNick()
        );
        Assert.assertEquals(
                "7.1 Ответ на некорректное сообщение (авто, уровень 1)",
                deleteInfoDto.getReason()
        );
        Assert.assertEquals(
                21464,
                deleteInfoDto.getUserid()
        );
    }
}
