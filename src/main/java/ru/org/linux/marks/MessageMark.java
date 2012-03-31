package ru.org.linux.marks;

import com.google.common.collect.ImmutableMap;

public enum MessageMark {
  BAN(100, "я за бан"),
  WORKSFORME(101, "умвр"),
  PLUSONE(102, "+1"),
  NONEED(103, "не нужно"),
  THANKS(104, "спасибо"),
  ;

  
  private static final ImmutableMap<Integer, MessageMark> reverse;
  
  static {
    ImmutableMap.Builder<Integer, MessageMark> builder = ImmutableMap.builder();
    for (MessageMark mark : values()) {
      builder.put(mark.getId(), mark);
    }
    
    reverse = builder.build();
  }
  
  private final int id;
  private final String title;

  private MessageMark(int id, String title) {
    this.id = id;
    this.title = title;
  }

  public int getId() {
    return id;
  }
  
  public static MessageMark getById(int id) {
    return reverse.get(id);
  }

  public String getTitle() {
    return title;
  }
}
