package ru.org.linux.user;

public class PreparedUserLogItem {
  private final UserLogItem item;

  private final User actionUser;

  public PreparedUserLogItem(UserLogItem item, User actionUser) {
    this.item = item;
    this.actionUser = actionUser;
  }

  public UserLogItem getItem() {
    return item;
  }

  public User getActionUser() {
    return actionUser;
  }
}
