package ru.org.linux.user;

import ru.org.linux.section.Section;

public class PreparedUsersSectionStatEntry {
  private final Section section;
  private final int count;

  public PreparedUsersSectionStatEntry(Section section, int count) {
    this.section = section;
    this.count = count;
  }

  public Section getSection() {
    return section;
  }

  public int getCount() {
    return count;
  }
}
