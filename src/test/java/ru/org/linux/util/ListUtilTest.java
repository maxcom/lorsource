/*
 * Copyright 1998-2015 Linux.org.ru
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

package ru.org.linux.util;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static ru.org.linux.util.ListUtil.firstHalf;
import static ru.org.linux.util.ListUtil.secondHalf;

public class ListUtilTest {
  @Test
  public void halfsEven() {
    List<String> data = ImmutableList.of("1", "2", "3", "4");

    assertEquals(ImmutableList.of("1", "2"), firstHalf(data));
    assertEquals(ImmutableList.of("3", "4"), secondHalf(data));
  }

  @Test
  public void halfsOdd() {
    List<String> data = ImmutableList.of("1", "2", "3");

    assertEquals(ImmutableList.of("1", "2"), firstHalf(data));
    assertEquals(ImmutableList.of("3"), secondHalf(data));
  }

  @Test
  public void halfsEmpty() {
    List<String> data = ImmutableList.of();

    assertEquals(ImmutableList.<String>of(), firstHalf(data));
    assertEquals(ImmutableList.<String>of(), secondHalf(data));
  }
}
