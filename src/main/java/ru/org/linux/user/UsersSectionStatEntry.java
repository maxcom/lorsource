package ru.org.linux.user;

public class UsersSectionStatEntry {
  private final int section;
  private final int count;

  public UsersSectionStatEntry(int section, int count) {
    this.section = section;
    this.count = count;
  }

  public int getSection() {
    return section;
  }

  public int getCount() {
    return count;
  }
}
