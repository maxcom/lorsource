package ru.org.linux.util;

import com.google.common.collect.ImmutableList;

import java.util.List;

public final class ListUtil {
  public static <T> List<T> headOrEmpty(List<T> list) {
    return list.isEmpty() ? ImmutableList.<T>of() : list.subList(0, 1);
  }

  public static <T> List<T> tailOrEmpty(List<T> list) {
    return list.size() <= 1 ? ImmutableList.<T>of() : list.subList(1, list.size());
  }

  public static <T> List<T> firstHalf(List<T> list) {
    int split = list.size() / 2 + (list.size() % 2);

    return list.subList(0, split);
  }

  public static <T> List<T> secondHalf(List<T> list) {
    int split = list.size() / 2 + (list.size() % 2);

    return list.subList(split, list.size());
  }
}
