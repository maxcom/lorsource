package ru.org.linux.user;

public enum UserLogAction {
  RESET_USERPIC("reset_userpic"),
  SET_USERPIC("set_userpic");

  private final String name;

  UserLogAction(String reset_userpic) {
    name = reset_userpic;
  }

  @Override
  public String toString() {
    return name;
  }
}
