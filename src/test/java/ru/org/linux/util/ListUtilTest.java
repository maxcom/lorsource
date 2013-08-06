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
