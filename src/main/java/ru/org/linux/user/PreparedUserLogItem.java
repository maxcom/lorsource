package ru.org.linux.user;

public class PreparedUserLogItem {
  private final UserLogItem item;

  private final User actionUser;
  private final boolean self;

  public PreparedUserLogItem(UserLogItem item, User actionUser) {
    this.item = item;
    this.actionUser = actionUser;
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
}
