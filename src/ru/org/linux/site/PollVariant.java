package ru.org.linux.site;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class PollVariant {
  private final int id;
  private String label;
  private final int votes;

  public PollVariant(int id, String label, int votes) {
    this.id = id;
    this.label = label;
    this.votes = votes;
  }

  public int getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public void updateLabel(Connection db, String label) throws SQLException {
    if (this.label.equals(label)) {
      return;
    }

    PreparedStatement pst = db.prepareStatement("UPDATE votes SET label=? WHERE id=?");
    pst.setString(1, label);
    pst.setInt(2, id);
    pst.executeUpdate();

    this.label = label;
  }

  public int getVotes() {
    return votes;
  }

  public void remove(Connection db) throws SQLException {
    Statement st = db.createStatement();
    st.executeUpdate("DELETE FROM votes WHERE id="+id);
  }
}
