/*
 * Copyright 1998-2024 Linux.org.ru
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
package ru.org.linux.poll;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
        @ContextConfiguration("classpath:database.xml"),
        @ContextConfiguration(classes = PollDaoIntegrationTestConfiguration.class)
})
public class PollDaoIntegrationTest {
  private static final Integer TEST_TOPIC_ID = 1937504;

  @Autowired
  private PollDao pollDao;

  @Test
  public void voteGetCurrentPollTest() {
    int currentPollId = pollDao.getMostRecentPollId();
    Poll poll = pollDao.getMostRecentPoll();
    assertEquals(currentPollId, poll.getId());
  }

  @Test
  @Transactional
  public void voteCreateAndRunningPollTest() {
    List <String> pollList = new ArrayList<>();
    pollList.add("Case 1");
    pollList.add("Case 2");
    pollList.add("Case 3");

    pollDao.createPoll(pollList, true, TEST_TOPIC_ID);
    Poll poll = pollDao.getPollByTopicId(TEST_TOPIC_ID);

      /* Проверяем правильность сохранения вариантов голосования */
    ImmutableList<PollVariantResult> pollVariants = pollDao.getPollResults(poll);
    assertEquals(3, pollVariants.size());

      /* Проверяем изменения по вариантам голосования */
    pollDao.addNewVariant(poll, "Case 4");
    pollVariants = pollDao.getPollResults(poll);
    assertEquals(4, pollVariants.size());

    PollVariantResult next = pollVariants.iterator().next();
    pollDao.removeVariant(new PollVariant(next.getId(), next.getLabel()));
    pollVariants = pollDao.getPollResults(poll);
    assertEquals(3, pollVariants.size());
  }
}
