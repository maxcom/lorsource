package ru.org.linux.comment;

import ru.org.linux.site.PublicApi;

import java.sql.Timestamp;

@PublicApi
public class EditSummary {
  private final String editNick;
  private final Timestamp editDate;
  private final int editCount;

  public EditSummary(String editNick, Timestamp editDate, int editCount) {
    this.editNick = editNick;
    this.editDate = editDate;
    this.editCount = editCount;
  }

  public String getEditNick() {
    return editNick;
  }

  public Timestamp getEditDate() {
    return editDate;
  }

  public int getEditCount() {
    return editCount;
  }
}
