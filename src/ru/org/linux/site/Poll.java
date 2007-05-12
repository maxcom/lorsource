package ru.org.linux.site;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Poll {
  public static final int MAX_POLL_SIZE = 15;
  public static final int ORDER_ID = 1;
  public static final int ORDER_VOTES = 2;

  private final int id;
  private final String title;
  private final Timestamp postdate;
  private boolean moderate;
  private final int topic;
  private final int userid;
  private final int commitby;
  private final Timestamp commitDate;
  private final boolean deleted;

  public static int getCurrentPollId(Connection db) throws SQLException {
    Statement st = db.createStatement();

    ResultSet rs = st.executeQuery("select id from votenames where commitdate = (select max(commitdate) from votenames where moderate AND NOT deleted)");

    rs.next();

    return rs.getInt("id");
  }

  public static Poll getCurrentPoll(Connection db) throws SQLException {
    try {
      return new Poll(db, getCurrentPollId(db));
    } catch (PollNotFoundException ex) {
      throw new RuntimeException(ex);
    }
  }

  public Poll(Connection db, int id) throws SQLException, PollNotFoundException {
    this.id = id;

    Statement st = db.createStatement();

    ResultSet rs = st.executeQuery("SELECT title, moderate, topic, postdate, userid, commitby, commitdate, deleted FROM votenames WHERE id="+id);

    if (!rs.next()) {
      throw new PollNotFoundException(id);
    }

    this.title = rs.getString("title");
    this.moderate = rs.getBoolean("moderate");
    this.topic = rs.getInt("topic");
    this.postdate = rs.getTimestamp("postdate");
    this.userid = rs.getInt("userid");
    this.commitby = rs.getInt("commitby");
    this.commitDate = rs.getTimestamp("commitdate");
    this.deleted = rs.getBoolean("deleted");
  }

  public int getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  private static int getNextPollId(Connection db) throws SQLException {
    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery("select nextval('vote_id') as voteid");
    rs.next();
    return rs.getInt("voteid");
  }

  public static int createPoll(Connection db, User user, String title, List pollList) throws SQLException {
    int voteid = getNextPollId(db);

    PreparedStatement pst = db.prepareStatement("INSERT INTO votenames (id, title, topic, groupid, userid, postdate) values (?,?,nextval('s_msgid'), nextval('s_guid'), ?, CURRENT_TIMESTAMP)");

    pst.setInt(1, voteid);
    pst.setString(2, title);
    pst.setInt(3, user.getId());

    pst.executeUpdate();

    try {
      Poll poll = new Poll(db, voteid);

      for (Iterator i = pollList.iterator(); i.hasNext(); ) {
        String variant = (String) i.next();

        if (variant.trim().length()==0) {
          continue;
        }

        poll.addNewVariant(db, variant);
      }
    } catch (PollNotFoundException ex) {
      throw new RuntimeException(ex);
    }

    return voteid;
  }

  public boolean isCommited() {
    return moderate;
  }

  public void commit(Connection db, User commitby) throws SQLException {
    PreparedStatement pst = db.prepareStatement("UPDATE votenames SET moderate='t', commitby=?, commitdate='now' WHERE id=?");
    pst.setInt(1, commitby.getId());
    pst.setInt(2, id);

    PreparedStatement pst2 = db.prepareStatement("UPDATE users SET score=score+3 WHERE id IN (SELECT userid FROM votenames WHERE id=?) AND score<300");
    pst2.setInt(1, id);

    pst.executeUpdate();
    pst2.executeUpdate();

    pst.close();
    pst2.close();
  }

  public List getPollVariants(Connection db, int order) throws SQLException {
    List variants = new ArrayList();
    Statement st = db.createStatement();
    ResultSet rs;

    switch (order) {
      case ORDER_ID:
        rs = st.executeQuery("SELECT * FROM votes WHERE vote="+id+" ORDER BY id");
        break;
      case ORDER_VOTES:
        rs = st.executeQuery("SELECT * FROM votes WHERE vote="+id+" ORDER BY votes DESC, id");
        break;
      default:
        throw new RuntimeException("Oops!? order="+order);
    }

    while (rs.next()) {
      int varId = rs.getInt("id");
      String label = rs.getString("label");
      int votes = rs.getInt("votes");

      variants.add(new PollVariant(varId, label, votes));
    }

    return variants;
  }

  public int getTopicId() {
    return topic;
  }

  public int getMaxVote(Connection db) throws SQLException {
    Statement st = db.createStatement();
    ResultSet rs=st.executeQuery("SELECT max(votes) FROM votes WHERE vote="+id);
    rs.next();
    int max=rs.getInt("max");
    if (max == 0) {
      max = 1;
    }
    rs.close();
    st.close();

    return max;
  }

  public void addNewVariant(Connection db, String label) throws SQLException {
    PreparedStatement addPst = db.prepareStatement("INSERT INTO votes (id, vote, label) values (nextval('votes_id'), ?, ?)");

    addPst.clearParameters();

    addPst.setInt(1, id);
    addPst.setString(2, label);

    addPst.executeUpdate();
  }

  public Timestamp getPostdate() {
    return postdate;
  }

  public int getUserid() {
    return userid;
  }

  public int getCommitby() {
    return commitby;
  }

  public Timestamp getCommitDate() {
    return commitDate;
  }

  public boolean isDeleted() {
    return deleted;
  }
}
