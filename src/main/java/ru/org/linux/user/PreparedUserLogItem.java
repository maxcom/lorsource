package ru.org.linux.user;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class PreparedUserLogItem {
  private final UserLogItem item;

  private final User actionUser;
  private final ImmutableMap<String, String> options;
  private final boolean self;

  public PreparedUserLogItem(UserLogItem item, User actionUser, Map<String, String> options) {
    this.item = item;
    this.actionUser = actionUser;
    this.options = ImmutableMap.copyOf(options);
    self = item.getUser()==item.getActionUser();
  }

  public UserLogItem getItem() {
    return item;
  }

  public User getActionUser() {
    return actionUser;
  }

  public boolean isSelf() {
    return self;
  }

  public ImmutableMap<String, String> getOptions() {
    return options;
  }
}
