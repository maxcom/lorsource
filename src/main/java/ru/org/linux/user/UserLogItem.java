package ru.org.linux.user;

import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import java.util.Map;

public class UserLogItem {
  private final int id;
  private final int user;
  private final int actionUser;
  private final DateTime actionDate;
  private final UserLogAction action;
  private final ImmutableMap<String, String> options;

  public UserLogItem(
          int id,
          int user,
          int actionUser,
          DateTime actionDate,
          UserLogAction action,
          Map<String, String> options
  ) {
    this.id = id;
    this.user = user;
    this.actionUser = actionUser;
    this.actionDate = actionDate;
    this.action = action;
    this.options = ImmutableMap.copyOf(options);
  }

  public int getId() {
    return id;
  }

  public int getUser() {
    return user;
  }

  public int getActionUser() {
    return actionUser;
  }

  public DateTime getActionDate() {
    return actionDate;
  }

  public UserLogAction getAction() {
    return action;
  }

  public ImmutableMap<String, String> getOptions() {
    return options;
  }
}
