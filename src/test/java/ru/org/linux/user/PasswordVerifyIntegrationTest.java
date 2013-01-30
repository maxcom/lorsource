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
package ru.org.linux.user;

import edu.vt.middleware.password.RuleResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;

/**
 */
@ContextConfiguration("integration-tests-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class PasswordVerifyIntegrationTest {

  @Autowired
  private PasswordVerify passwordVerify;

  @Test
  public void test() {
    RuleResult result1 = passwordVerify.validate("ololo1O");
    RuleResult result2 = passwordVerify.validate("ololo");
    assertTrue(result1.isValid());
    assertFalse(result2.isValid());
  }
}
