/*
 * Copyright 1998-2010 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.site;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import ru.org.linux.util.HTMLFormatter;

public class Poll {
  public static final int MAX_POLL_SIZE = 15;
  public static final int ORDER_ID = 1;
  public static final int ORDER_VOTES = 2;

  private final int id;
  private final int topic;

  private final boolean current;
  private final boolean multiSelect;

  public static Poll getPollByTopic(Connection db, int msgid) throws SQLException, PollNotFoundException {
    PreparedStatement pst = db.prepareStatement("SELECT votenames.id FROM votenames,topics WHERE topics.id=? AND votenames.topic=topics.id");
    pst.clearParameters();
    pst.setInt(1, msgid);
    ResultSet rs = pst.executeQuery();
    if (!rs.next()) {
      throw new PollNotFoundException();
    }
    
    return new Poll(db, rs.getInt("id"));
  }

  public void setTopicId(Connection db, int msgid) throws SQLException {
    PreparedStatement addPst = db.prepareStatement("UPDATE votenames SET topic=? WHERE id=?");
    addPst.clearParameters();
    addPst.setInt(1, msgid);
    addPst.setInt(2, id);
    addPst.executeUpdate();
  }

  public static int getCurrentPollId(Connection db) throws SQLException {
    Statement st = db.createStatement();

    ResultSet rs = st.executeQuery("SELECT votenames.id FROM votenames,topics WHERE topics.id=votenames.topic AND topics.moderate = 't' AND topics.deleted = 'f' AND topics.commitdate = (select max(commitdate) from topics where groupid=19387 AND moderate AND NOT deleted)");

    return rs.next()?rs.getInt("id"):0;
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

    ResultSet rs = st.executeQuery("SELECT topic, multiselect FROM votenames WHERE id="+id);

    if (!rs.next()) {
      throw new PollNotFoundException(id);
    }

    topic = rs.getInt("topic");
    multiSelect = rs.getBoolean("multiselect");

    current = getCurrentPollId(db)==id;
  }

  public int getId() {
    return id;
  }

  private static int getNextPollId(Connection db) throws SQLException {
    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery("select nextval('vote_id') as voteid");
    rs.next();
    return rs.getInt("voteid");
  }

  public static int createPoll(Connection db, List<String> pollList, boolean multiSelect) throws SQLException {
    int voteid = getNextPollId(db);

    PreparedStatement pst = db.prepareStatement("INSERT INTO votenames (id, multiselect) values (?,?)");

    pst.setInt(1, voteid);
    pst.setBoolean(2, multiSelect);

    pst.executeUpdate();

    try {
      Poll poll = new Poll(db, voteid);

      for (String variant : pollList) {
        if (variant.trim().length() == 0) {
          continue;
        }

        poll.addNewVariant(db, variant);
      }
    } catch (PollNotFoundException ex) {
      throw new RuntimeException(ex);
    }

    return voteid;
  }

  public List<PollVariant> getPollVariants(Connection db, int order) throws SQLException {
    List<PollVariant> variants = new ArrayList<PollVariant>();
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

  /* TODO: move to JSP */
  public String renderPoll(Connection db, String fullUrl) throws SQLException {
    StringBuilder out = new StringBuilder();
    int max = getMaxVote(db);
    List<PollVariant> vars = getPollVariants(db, ORDER_VOTES);
    out.append("<table>");
    int total = 0;
    for (PollVariant var : vars) {
      out.append("<tr><td>");
      int votes = var.getVotes();
      out.append(HTMLFormatter.htmlSpecialChars(var.getLabel()));
      out.append("</td><td>").append(votes).append("</td><td>");
      total += votes;
      for (int i = 0; i < 20 * votes / max; i++) {
        out.append("<img src=\"").append(fullUrl).append("white/img/votes.png\" alt=\"*\">");
      }
      out.append("</td></tr>");
    }
    out.append("<tr><td colspan=2>Всего голосов: ").append(total).append("</td></tr>");
    out.append("</table>");
    return out.toString();
  }

  public boolean isCurrent() {
    return current;
  }

  public boolean isMultiSelect() {
    return multiSelect;
  }
}
