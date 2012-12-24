package ru.org.linux.site;

@PublicApi
public class ApiDeleteInfo {
  private final String nick;
  private final String reason;

  public ApiDeleteInfo(String nick, String reason) {
    this.nick = nick;
    this.reason = reason;
  }

  public String getNick() {
    return nick;
  }

  public String getReason() {
    return reason;
  }
}
