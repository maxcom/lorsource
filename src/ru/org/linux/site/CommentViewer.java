package ru.org.linux.site;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Logger;

import ru.org.linux.util.UtilException;

public class CommentViewer {
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  public static final int COMMENTS_INITIAL_BUFSIZE = 50;

  public static final int FILTER_NONE = 0;
  public static final int FILTER_ANONYMOUS = 1;
  public static final int FILTER_IGNORED = 2;

  private final Template tmpl;
  private final CommentList comments;
  private final boolean expired;
  private final Connection db;

  private final String user;
  public static final int FILTER_LISTANON = FILTER_ANONYMOUS+FILTER_IGNORED;

  private int outputCount = 0;

  public CommentViewer(Template t, Connection db, CommentList comments, String user, boolean expired) {
    tmpl=t;
    this.comments = comments;
    this.user=user;
    this.expired = expired;
    this.db = db;
  }

  private void showCommentList(StringBuffer buf, List<Comment> comments, int offset, int limit, Set<Integer> hideSet)
      throws IOException, UtilException, SQLException, UserNotFoundException {
    CommentView view = new CommentView();
    int shown = 0;

    for (ListIterator<Comment> i = comments.listIterator(0); i.hasNext();) {
      int index = i.nextIndex();

      Comment comment = i.next();

      if (index<offset || (limit!=0 && index>=offset+limit)) {
        continue;
      }

      if (hideSet==null || !hideSet.contains(comment.getMessageId())) {
        shown++;
        outputCount++;
        buf.append(view.printMessage(comment, tmpl, db, this.comments, true, tmpl.isModeratorSession(), user, expired));
      }
    }

    logger.fine("Showing list size="+comments.size()+" shown="+shown);    
  }

  public String show(int offset, int limit, Set<Integer> hideSet) throws IOException, UtilException, SQLException, UserNotFoundException {
    StringBuffer buf=new StringBuffer();

    showCommentList(buf, comments.getList(), offset, limit,  hideSet);

    return buf.toString();
  }

  public String showSubtree(int parentId) throws IOException, UtilException, MessageNotFoundException, SQLException, UserNotFoundException {
    StringBuffer buf=new StringBuffer();

    CommentNode parentNode = comments.getNode(parentId);

    if (parentNode==null) {
      throw new MessageNotFoundException(parentId);
    }

    List<Comment> parentList = new ArrayList<Comment>();
    parentNode.buildList(parentList);

    /* display comments */
    showCommentList(buf, parentList, 0, 0, null);

    return buf.toString();
  }

  public static int parseFilterChain(String filter) {
    if ("list".equals(filter)) {
      return FILTER_IGNORED;
    }

    if ("anonymous".equals(filter)) {
      return FILTER_ANONYMOUS;
    }

    if ("listanon".equals(filter)) {
      return FILTER_IGNORED+FILTER_ANONYMOUS;
    }

    return FILTER_NONE;
  }

  public static String toString(int filterMode) {
    switch (filterMode) {
      case FILTER_NONE: return "show";
      case FILTER_ANONYMOUS: return "anonymous";
      case FILTER_IGNORED: return "list";
      case FILTER_IGNORED+FILTER_ANONYMOUS: return "listanon";
      default: return "show";
    }
  }

  public int getOutputCount() {
    return outputCount;
  }
}
